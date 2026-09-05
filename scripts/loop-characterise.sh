#!/usr/bin/env bash
# Autonomous characterization-test loop over the pure-logic surface of Thinlet.
# Each pass: Claude writes ONE new test suite for a target the coverage worklist
# named, the script runs every gating CI job plus a mutation gate, and commits
# only if all of them pass. Where loop-modernise.sh spends the net, this builds it.
#
#   scripts/loop-characterise.sh              # up to 10 slices, one resumed session
#   scripts/loop-characterise.sh 3            # cap at 3 slices
#   scripts/loop-characterise.sh --new        # fresh Claude context each slice
#   scripts/loop-characterise.sh --dry-run    # one slice, verified but never committed
#
# Scope is the mirror image of loop-modernise: the TEST tree is writable, main
# source is fenced absolutely, and new files are required rather than forbidden.
# guard_no_main is the load-bearing one — a loop that can edit both the code and
# its tests can make a test pass by changing the code, and no gate here catches
# that. D91 extracted the pure-logic surface so this guard can stay absolute.
#
# The teeth gate is mutation coverage scoped to the slice's assigned method
# (D92): a green test proves nothing on its own, and a flat mutation score is
# no better — an assertion-free test still scores ~50% because PIT counts a
# thrown exception as a kill. "No survivor in the method you were given" is the
# condition that floor cannot clear.
#
# Runs on the bare HOST, not in the dev container: it shells out to `docker run`
# via local-ci.sh, and the container has no docker CLI or socket.
#
# CHARACTERISE_COAUTHOR overrides the commit trailer (default: Claude Opus 5).
#
# The whole body lives in main() so bash parses the file before executing any of
# it — a rollback must never rewrite the script out from under the interpreter.
set -euo pipefail

readonly TARGET="thinlet-core/src/test/java"
readonly MSG_REL=".characterise-msg"
readonly LOG_REL=".characterise-verify.log"
readonly DECLINED_REL=".characterise-declined"
readonly WORKLIST_REL=".characterise-worklist"
readonly COAUTHOR="${CHARACTERISE_COAUTHOR:-Claude Opus 5}"
readonly JACOCO_XML="thinlet-core/target/site/jacoco/jacoco.xml"

root="$(git rev-parse --show-toplevel)"
msg_file="$root/$MSG_REL"
log="$root/$LOG_REL"
declined="$root/$DECLINED_REL"
worklist="$root/$WORKLIST_REL"

# The pure-logic surface: methods a plain JUnit test can drive without
# synthesising an AWT event. D91 made the private ones package-private static;
# the rest are reached through the public parse/accessor/tree API. Paint, event
# dispatch, focus, popup, timer and Renderer methods are deliberately absent —
# they are the later phase, once this loop is proven.
logic_targets() {
    cat << 'NAMES'
parse addAttribute addElement getMethod finishParse instance
getDOMText getDOMAttribute getDOMCount getDOMNode
createImpl addImpl removeItemImpl getIndex findNextItem getNextItem insertItem
getItemImpl getItemCountImpl getDefinition
setChoice setBoolean setInteger setRectangle setKeystrokeImpl
selectAll selectItem extend setLead select changeCheck
getChars filter getSum getFieldSize getGrid checkOffset getCaretLocation
spinValueFromText spinTextFromValue findScroll
hasAccelerator findText getListItem processList getMenu
NAMES
}

# Working-tree changes, one path per line, excluding this script's scratch files.
changed_paths() {
    git -C "$root" status --porcelain=v1 --untracked-files=all \
        | awk '{ if ($0 ~ / -> /) { sub(/.* -> /, ""); print } else { print substr($0, 4) } }' \
        | grep -vx -e "$MSG_REL" -e "$LOG_REL" -e "$DECLINED_REL" -e "$WORKLIST_REL" || true
}

# Scoped rollback. Never `git reset --hard`: that is unbounded and would revert
# the maintainer's unrelated work and this script alike.
rollback() {
    git -C "$root" checkout -- "$TARGET" KNOWN-QUIRKS.md > /dev/null 2>&1 || true
    git -C "$root" clean -fdq -- "$TARGET"
    rm -f "$msg_file"
}

run_logged() {
    local status=0
    set +e
    "$@" 2>&1 | tee -a "$log"
    status="${PIPESTATUS[0]}"
    set -e
    return "$status"
}

# The new suite this slice added: the single untracked .java file under TARGET.
# Deriving it from git rather than asking the pass to name it keeps one source
# of truth, and guard_new_files_only has already proved there is exactly one.
new_test_file() {
    git -C "$root" status --porcelain=v1 --untracked-files=all -- "$TARGET" \
        | awk '/^\?\? .*\.java$/ { print substr($0, 4) }'
}

new_test_class() {
    local f
    f="$(new_test_file)"
    [ -n "$f" ] || return 1
    printf '%s\n' "$f" | sed -e "s|^$TARGET/||" -e 's|/|.|g' -e 's|\.java$||'
}

main() {
    local max=10 fresh=false dry_run=false
    while [ $# -gt 0 ]; do
        case "$1" in
            --new) fresh=true; shift ;;
            --dry-run) dry_run=true; shift ;;
            [0-9]*) max="$1"; shift ;;
            *)
                echo "usage: loop-characterise.sh [N] [--new] [--dry-run]" >&2
                echo "  N          cap the number of slices (default: 10)" >&2
                echo "  --new      fresh Claude context each slice (default: one resumed session)" >&2
                echo "  --dry-run  one slice, verified but never committed" >&2
                exit 2
                ;;
        esac
    done
    [ "$dry_run" = true ] && max=1

    cd "$root"

    # One loop per worktree: local-ci.sh bind-mounts the repo and writes .m2/ and
    # target/ inside it, so two concurrent runs corrupt each other's build state.
    # --git-dir, not "$root/.git": in a linked worktree that path is a FILE.
    exec 9> "$(git -C "$root" rev-parse --git-dir)/loop-characterise.lock"
    if ! flock -n 9; then
        echo "loop-characterise: another run holds the lock in this worktree" >&2
        exit 1
    fi

    local session
    session="$(uuidgen 2> /dev/null || cat /proc/sys/kernel/random/uuid)"

    echo "loop-characterise: target=$TARGET slices<=$max session=$session"
    echo "  verify per slice: local-ci.sh -t + mutation gate + base + rows 8/11/17"
    echo "  commit: each green slice, no tags$([ "$dry_run" = true ] && echo ' (DISABLED: --dry-run)')"
    echo "  context: $([ "$fresh" = true ] && echo 'fresh per slice (--new)' || echo 'one resumed session')"
    echo "  options: [N] cap slices | --new fresh context | --dry-run verify without committing"

    preflight

    local iteration=0 first=true
    while [ "$iteration" -lt "$max" ]; do
        local step=$((iteration + 1))
        local target_row target_method target_lines
        target_row="$(next_target)" || { echo; echo "loop-characterise: worklist exhausted"; reminder; return 0; }
        target_method="$(printf '%s' "$target_row" | awk '{print $1}')"
        target_lines="$(printf '%s' "$target_row" | cut -d' ' -f2-)"

        echo
        echo "loop-characterise: slice $step of $max — target $target_method"
        : > "$log"
        rm -f "$msg_file" "$declined"

        if [ "$fresh" = true ] || [ "$first" = true ]; then
            first=false
            [ "$fresh" = true ] && session="$(uuidgen 2> /dev/null || cat /proc/sys/kernel/random/uuid)"
            claude --print --permission-mode acceptEdits --session-id "$session" \
                "$(slice_prompt "$target_method" "$target_lines")"
        else
            claude --print --permission-mode acceptEdits --resume "$session" \
                "$(slice_prompt "$target_method" "$target_lines")"
        fi

        if [ -z "$(changed_paths)" ]; then
            if [ -s "$declined" ]; then
                echo "loop-characterise: pass declined $target_method —" >&2
                cat "$declined" >&2
                iteration="$step"
                continue
            fi
            echo "loop-characterise: no test proposed and no reason given; stopping" >&2
            return 1
        fi

        guard_no_main
        guard_strays
        guard_new_files_only
        guard_quirk_pairing

        (cd "$root" && ./mvnw -q -B spotless:apply)

        if verify_all "$target_method"; then
            if [ "$dry_run" = true ]; then
                echo "loop-characterise: slice $step verified; left uncommitted (--dry-run)"
                return 0
            fi
            commit_slice "$step" "$target_method"
            iteration="$step"
            continue
        fi

        echo "loop-characterise: slice $step failed verification — one repair attempt"
        claude --print --permission-mode acceptEdits --resume "$session" "$(repair_prompt)"

        if [ -z "$(changed_paths)" ]; then
            echo "loop-characterise: repair withdrew the slice; stopping" >&2
            rollback
            return 1
        fi
        guard_no_main
        guard_strays
        guard_new_files_only
        guard_quirk_pairing
        (cd "$root" && ./mvnw -q -B spotless:apply)

        if ! verify_all "$target_method"; then
            echo "loop-characterise: repair failed verification; rolling back and stopping" >&2
            echo "  full log: $LOG_REL" >&2
            rollback
            return 1
        fi
        if [ "$dry_run" = true ]; then
            echo "loop-characterise: repaired slice $step verified; left uncommitted (--dry-run)"
            return 0
        fi
        commit_slice "$step" "$target_method"
        iteration="$step"
    done

    echo
    echo "loop-characterise: stopped after $iteration slice(s) (cap $max)"
    coverage_delta
    reminder
}

# Prove the gates work and the tree is already green, then build the worklist, so
# any later failure is attributable to a slice rather than inherited.
preflight() {
    echo
    echo "loop-characterise: preflight"
    if ! docker info > /dev/null 2>&1; then
        echo "loop-characterise: docker is not responding — local-ci.sh cannot run" >&2
        exit 1
    fi
    if [ -n "$(changed_paths)" ]; then
        echo "loop-characterise: working tree is dirty; commit or stash first" >&2
        changed_paths >&2
        exit 1
    fi
    : > "$log"
    if ! run_logged "$root/.devcontainer/ci/local-ci.sh"; then
        echo "loop-characterise: the tree fails verification before any slice" >&2
        exit 1
    fi
    echo "loop-characterise: measuring coverage to build the worklist"
    if ! run_logged "$root/scripts/coverage.sh"; then
        echo "loop-characterise: coverage run failed; no worklist can be built" >&2
        exit 1
    fi
    build_worklist
    local n
    n="$(wc -l < "$worklist")"
    if [ "$n" -eq 0 ]; then
        echo "loop-characterise: every allowlisted target is fully covered — nothing to do"
        exit 0
    fi
    echo "loop-characterise: preflight green; $n allowlisted target(s) with missed lines"
}

# allowlist ∩ coverage worklist, worst branches first. The allowlist decides what
# is in scope; coverage decides the order and supplies the exact missed lines.
build_worklist() {
    WORKLIST=true python3 "$root/scripts/coverage-summary.py" "$root/$JACOCO_XML" \
        | awk '/^  thinlet\.Thinlet\./ { print }' \
        | sed -E 's/^  thinlet\.Thinlet\.([A-Za-z0-9_]+)\(.*lines=([0-9,+]+).*$/\1 \2/' \
        | grep -Fwf <(logic_targets | tr ' ' '\n' | grep -v '^$') > "$worklist" || true
}

# The next target not yet attempted this run. Attempted rows are struck from the
# file, so a resumed slice never re-picks one and the file doubles as progress.
next_target() {
    [ -s "$worklist" ] || return 1
    local row
    row="$(head -n 1 "$worklist")"
    sed -i '1d' "$worklist"
    printf '%s\n' "$row"
}

# Every gating CI job plus the mutation gate, cheapest-failing-first. japicmp is
# absent by construction: guard_no_main has already proved main source is
# untouched, so the published API cannot have moved.
verify_all() {
    local target_method="$1" cls
    cls="$(new_test_class)" || { echo "loop-characterise: no new test class found" >&2; return 1; }

    run_logged "$root/.devcontainer/ci/local-ci.sh" -t "$cls" || return 1
    run_logged "$root/scripts/mutation.sh" "$cls" --gate "$target_method" || return 1
    run_logged "$root/.devcontainer/ci/local-ci.sh" || return 1
    local jdk
    for jdk in 8 11 17; do
        run_logged "$root/.devcontainer/ci/local-ci.sh" "$jdk" || return 1
    done
}

# The load-bearing guard. A loop that can edit both the code and its tests can
# make a test pass by changing the code, and nothing else here would catch it.
# The pathspec needs the trailing /*: a bare '*/src/main/java' is a wildmatch
# against the whole path, so it matches the directory name and NOTHING under it —
# the guard silently passed a modified Renderer.java until a deliberate-violation
# test caught it.
guard_no_main() {
    local spec=':(glob)*/src/main/java/**'
    if ! git -C "$root" diff --quiet -- "$spec" \
        || [ -n "$(git -C "$root" status --porcelain=v1 --untracked-files=all -- "$spec")" ]; then
        echo "loop-characterise: slice touched main source — rolling back and stopping" >&2
        git -C "$root" status --porcelain=v1 -- "$spec" >&2
        # Main source is out of scope by construction, and preflight proved the
        # tree was clean, so anything here is this slice's and must go back too —
        # otherwise the run stops leaving the library edited.
        git -C "$root" checkout -- "$spec" > /dev/null 2>&1 || true
        git -C "$root" clean -fdq -- "$spec" > /dev/null 2>&1 || true
        rollback
        exit 1
    fi
}

guard_strays() {
    local strays
    strays="$(changed_paths | grep -v -e "^$TARGET/" -e '^KNOWN-QUIRKS\.md$' || true)"
    if [ -n "$strays" ]; then
        echo "loop-characterise: change outside $TARGET — rolling back and stopping" >&2
        echo "$strays" >&2
        rollback
        exit 1
    fi
}

# New files only. Modifying an existing suite could weaken it, and the fixtures
# under src/test/resources (goldens, corpus, the verbatim DTD) are frozen: D44
# and D52 both say new files, never edits.
guard_new_files_only() {
    local modified count
    modified="$(git -C "$root" status --porcelain=v1 --untracked-files=all -- "$TARGET" | grep -v '^??' || true)"
    if [ -n "$modified" ]; then
        echo "loop-characterise: existing test file modified — new files only" >&2
        echo "$modified" >&2
        rollback
        exit 1
    fi
    count="$(new_test_file | wc -l)"
    if [ "$count" -ne 1 ]; then
        echo "loop-characterise: expected exactly one new test file, found $count" >&2
        new_test_file >&2
        rollback
        exit 1
    fi
}

# A pinned quirk and its catalogue entry travel together, and KNOWN-QUIRKS.md is
# append-only here: the loop records a finding, it never revises one.
guard_quirk_pairing() {
    local tagged entry deletions
    tagged="$(grep -l 'documents-current-behavior' $(new_test_file) 2> /dev/null || true)"
    entry="$(git -C "$root" diff --cached --numstat -- KNOWN-QUIRKS.md; git -C "$root" diff --numstat -- KNOWN-QUIRKS.md)"
    deletions="$(printf '%s\n' "$entry" | awk '{s+=$2} END {print s+0}')"
    if [ "$deletions" -gt 0 ]; then
        echo "loop-characterise: KNOWN-QUIRKS.md is append-only for this loop" >&2
        rollback
        exit 1
    fi
    if [ -n "$tagged" ] && [ -z "$entry" ]; then
        echo "loop-characterise: test tagged documents-current-behavior with no KNOWN-QUIRKS entry" >&2
        rollback
        exit 1
    fi
    if [ -z "$tagged" ] && [ -n "$entry" ]; then
        echo "loop-characterise: KNOWN-QUIRKS entry with no documents-current-behavior tag" >&2
        rollback
        exit 1
    fi
}

coverage_delta() {
    echo "loop-characterise: re-measuring coverage"
    run_logged "$root/scripts/coverage.sh" || true
}

commit_slice() {
    local step="$1" target_method="$2" subject
    if [ -s "$msg_file" ]; then
        subject="$(head -n 1 "$msg_file")"
    else
        subject="Characterize $target_method (loop pass $step)"
    fi
    rm -f "$msg_file"
    git -C "$root" add -- "$TARGET" KNOWN-QUIRKS.md
    # --no-verify: the pre-commit framework shim is installed only in the dev
    # container, so the hook exits 1 on the host. Its one job is spotless:apply,
    # which this script already ran against the same config.
    git -C "$root" commit --no-verify \
        -m "$subject" \
        -m "Co-Authored-By: $COAUTHOR <noreply@anthropic.com>"
    echo "loop-characterise: committed slice $step — $subject"
}

reminder() {
    echo "loop-characterise: before opening a PR, run scripts/comment-pass.sh, do the review,"
    echo "  then scripts/comment-pass.sh done (D60 blocks gh pr create on Java branches)."
}

slice_prompt() {
    local method="$1" lines="$2"
    cat << EOF
You are one pass of an automated characterization-test loop over the 2005 Thinlet
source. Write ONE new test suite for a single assigned target, then stop. Earlier
passes are in this branch's git log — do not redo them.

YOUR TARGET: thinlet.Thinlet.$method
Source lines JaCoCo reports as still missed: $lines

Read the method, work out what those lines do, and write tests that reach them and
assert what the code ACTUALLY does today. D91 made the pure-logic surface
package-private static, so most targets are a direct call taking a Thinlet as the
first argument; the rest are reached through the public parse/accessor/tree API.

Hard rules. A violation fails the pass and the slice is rolled back:

1. Java 8 floor. Banned: var, switch expressions, records, text blocks,
   List.of/Map.of/Set.of, Files.readString, String.repeat/strip/isBlank,
   instanceof patterns. Lambdas, streams, try-with-resources, the diamond
   operator and @Override are fine.
2. Create exactly ONE new file, under $TARGET. Never modify an existing test
   file, and never touch src/test/resources — the goldens, the XML corpus and
   thinlet.dtd are frozen (D44/D52).
3. NEVER edit anything under src/main/java. You are testing that code, not
   changing it. A slice that touches main source is rolled back and the run stops.
4. Characterization, not specification: assert what the code does, bugs included.
   If the behavior looks wrong, still pin it — tag the test
   @Tag("documents-current-behavior") AND append a KNOWN-QUIRKS.md entry in the
   established format with "Enhanced Thinlet disposition: undecided" citing no
   D-entry. The disposition is the maintainer's to make, never yours. If you add
   that tag you must add the entry, and vice versa.
5. Style: package thinlet, @ExtendWith(XvfbDisplayExtension.class), AssertJ,
   sentence-named test methods. Follow ParserSaxModeTest as the exemplar —
   one-line file header, <=3-line class javadoc with a DECISIONS.md pointer.
6. Your test must have TEETH. It is checked by mutation testing scoped to
   $method: every mutant of that method your test reaches must be detected, and
   it must detect at least one. Asserting that a call merely does not throw will
   not pass — PIT scores a thrown exception as a kill, so an assertion-free test
   already scores about 50% and that floor earns nothing here. Assert on returned
   values and on model state read back through the getters.

If this target genuinely cannot be reached without synthesising an AWT input
event, do not write a bad test: write one or two lines explaining why to
$DECLINED_REL, create no file, and stop. That file is the worklist for the later
event-driven phase.

Finally, write a single-line imperative commit subject (<=72 chars, no trailing
period) to $MSG_REL. Nothing else belongs in that file.
EOF
}

repair_prompt() {
    cat << EOF
The previous pass failed verification. Your test is still in the working tree and
the tail of the log is below. Work out what failed and fix it.

If the mutation gate failed, the log names each surviving mutant with its line and
the change that was not detected: add assertions that would catch exactly those.
If a golden or input test failed, your test has side effects on shared state —
that is your bug, never the net's, and NEVER edit main source or a golden to fix
it. The same hard rules apply. Rewrite the commit subject in $MSG_REL.

--- verification log (tail) ---
$(tail -n 60 "$log")
EOF
}

main "$@"
