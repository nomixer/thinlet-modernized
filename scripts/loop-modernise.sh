#!/usr/bin/env bash
# Autonomous behavior-preserving modernisation loop over the 2005 Thinlet source.
# Each pass: Claude modernises ONE slice of thinlet-core/src/main/java, the script
# runs every gating CI job locally, and commits only if all of them pass. The
# behavior net (goldens + input suites + pinned quirks) is the proof that nothing
# observable changed; a golden diff is a regression signal, never a re-record.
#
#   scripts/loop-modernise.sh              # up to 10 slices, one resumed Claude session
#   scripts/loop-modernise.sh 3            # cap at 3 slices
#   scripts/loop-modernise.sh --new        # fresh Claude context each slice
#   scripts/loop-modernise.sh --dry-run    # one slice, verified but never committed
#
# Runs on the bare HOST, not in the dev container: it shells out to `docker run`
# via local-ci.sh, and the container has no docker CLI or socket.
#
# The japicmp baseline needs GitHub Packages read auth (D4). No credential is kept
# at rest: a settings.xml is minted from `gh auth token` per run and deleted on
# exit, and is skipped entirely once the baseline jar is cached.
#
# MODERNISE_COAUTHOR overrides the commit trailer (default: Claude Opus 5).
#
# The whole body lives in main() so bash parses the file before executing any of
# it — a rollback must never rewrite the script out from under the interpreter.
set -euo pipefail

readonly TARGET="thinlet-core/src/main/java"
readonly MSG_REL=".modernise-msg"
readonly LOG_REL=".modernise-verify.log"
readonly COAUTHOR="${MODERNISE_COAUTHOR:-Claude Opus 5}"
readonly BASELINE_JAR=".m2/repository/com/nomixer/thinlet/thinlet-core/0.1.0/thinlet-core-0.1.0.jar"

# The XML parser is fenced off pending the ROADMAP 3c decision on whether it
# survives at all (recorded 2026-09-02): modernising ~207 lines that may be
# deleted is wasted work, and its whitespace semantics are the delicate part of
# any JAXP replacement. Matched against the enclosing method of each changed
# line, so it tracks the code rather than line numbers.
readonly FENCED_FILE="thinlet-core/src/main/java/thinlet/Thinlet.java"

root="$(git rev-parse --show-toplevel)"
msg_file="$root/$MSG_REL"
log="$root/$LOG_REL"

# Set by maven_settings() when it mints a credential file; "" means none exists.
settings_file=""

drop_settings() {
    [ -n "$settings_file" ] && rm -f "$settings_file"
    settings_file=""
}
trap drop_settings EXIT

new_uuid() {
    if command -v uuidgen > /dev/null 2>&1; then
        uuidgen
    else
        cat /proc/sys/kernel/random/uuid
    fi
}

# Working-tree changes, one path per line, excluding this script's own scratch
# files (.modernise-msg is not gitignored; the log matches the *.log rule).
changed_paths() {
    git -C "$root" status --porcelain=v1 --untracked-files=all \
        | awk '{ if ($0 ~ / -> /) { sub(/.* -> /, ""); print } else { print substr($0, 4) } }' \
        | grep -vx -e "$MSG_REL" -e "$LOG_REL" || true
}

# Scoped rollback. Never `git reset --hard`: that is unbounded and would revert
# the maintainer's unrelated work and this script alike.
rollback() {
    git -C "$root" checkout -- "$TARGET" > /dev/null 2>&1 || true
    git -C "$root" clean -fdq -- "$TARGET"
    rm -f "$msg_file"
}

# Run a command, tee its output to the iteration log, return its real status.
# `set +e` around the pipeline so PIPESTATUS survives and set -e cannot abort a
# verification step we intend to handle.
run_logged() {
    local status=0
    set +e
    "$@" 2>&1 | tee -a "$log"
    status="${PIPESTATUS[0]}"
    set -e
    return "$status"
}

# The v0.1.0 baseline lives in GitHub Packages, which requires a token even for
# public reads (D4). Mint a settings.xml for the run rather than leaving a
# credential on disk; drop_settings deletes it on exit. Returns without minting
# when the jar is already cached (the resolve is then offline) or when `gh`
# cannot supply a token — japicmp's own baseline check reports the miss, so a
# failure here must never masquerade as a passed gate.
maven_settings() {
    [ -f "$root/$BASELINE_JAR" ] && return 0
    [ -n "$settings_file" ] && return 0
    command -v gh > /dev/null 2>&1 || return 0
    local token login
    token="$(gh auth token 2> /dev/null)" || return 0
    [ -n "$token" ] || return 0
    # GitHub Packages authenticates on the token; the username must be the GitHub
    # login, not `git config user.name`, which may carry spaces.
    login="$(gh api user --jq .login 2> /dev/null)" || login=""
    [ -n "$login" ] || login="x-access-token"

    settings_file="$(mktemp "${TMPDIR:-/tmp}/loop-modernise-settings.XXXXXX")"
    chmod 600 "$settings_file"
    cat > "$settings_file" << XML
<settings>
  <servers>
    <server>
      <id>github-nomixer</id>
      <username>$login</username>
      <password>$token</password>
    </server>
  </servers>
</settings>
XML
}

japicmp() {
    maven_settings
    local settings_arg=()
    [ -n "$settings_file" ] && settings_arg=(-s "$settings_file")
    if ! run_logged env -C "$root" MAVEN_USER_HOME="$root/.m2" "$root/mvnw" \
        -B -ntp "${settings_arg[@]}" -Papicheck -DskipTests -pl thinlet-core -am \
        -Dmaven.repo.local=.m2/repository verify; then
        return 1
    fi
    # An unresolvable baseline is only a WARNING: japicmp reports "Comparing …
    # against " with an empty right-hand side, scores every member as NEW, and
    # the build still succeeds. Exit status alone therefore scores an ungated
    # build as a pass. The cached baseline jar is the proof it had something to
    # compare against. Verified 2026-08-17: without read:packages the resolve
    # 401s and only a .lastUpdated marker is written, never the jar.
    if [ ! -f "$root/$BASELINE_JAR" ]; then
        echo "loop-modernise: japicmp found no v0.1.0 baseline — the API gate did not run" >&2
        return 1
    fi
}

slice_prompt() {
    cat << EOF
You are one pass of an automated, behavior-preserving modernisation loop over the
2005 Thinlet source. Pick ONE small slice of $TARGET, modernise it in place, then
stop. Earlier passes are in this branch's git log — do not redo them.

Hard rules. A violation fails the pass and the slice is rolled back:

1. Java 8 floor (maven.compiler.release=8). Nothing above Java 8 compiles here.
   Banned: var, switch expressions, records, text blocks, List.of/Map.of/Set.of,
   Optional.stream, Files.readString, String.repeat/strip/isBlank, instanceof
   patterns. Lambdas, streams, try-with-resources, the diamond operator, enhanced
   for, StringBuilder and @Override are all available and fine.
2. Behavior-preserving only. The golden trace net, the input suites and the pinned
   quirk tests must stay green with ZERO changes to any golden file. A golden diff
   means your change altered rendering: fix the code, never the golden.
3. Edit only files under $TARGET. Never touch thinlet.dtd, anything under
   src/test/, the XML corpus, *.gif, or scripts/. Do not create new files —
   extracting classes is gated Cut 4/5/6 seam work, not this loop's job.
4. No new or changed public/protected members (D43). japicmp gates the published
   API against v0.1.0 and runs on every pass.
5. Never apply a mechanical substitution across a span containing a string or char
   literal. D52 records the only refactoring regression this repo has had: a
   blanket regex rewrote "font" into "t.font" inside a literal and broke a path
   the goldens did not cover. After any repetitive edit, re-read the literals you
   passed over.
6. Minimal diff. Never reformat lines you are not otherwise changing.
7. Do not touch the XML parser: parse(InputStream, char, Object), parseXML,
   parseDOM, or the getDOM* accessors. Whether it is replaced by JAXP outright is
   an open ROADMAP 3c decision, so modernising it now may be work on deleted code.
   A slice that reaches into it is rolled back.

Use the java-refactor skill with a target floor of Java 8, and take its payoff
judgment seriously — skip anything it rates cosmetic or a false gain.

Finally, write a single-line imperative commit subject (<=72 chars, no trailing
period) to $MSG_REL. Nothing else belongs in that file.
EOF
}

repair_prompt() {
    cat << EOF
The previous pass failed verification. Your changes are still in the working tree
and the tail of the log is below. Work out which change broke it and fix it; if
you cannot fix it safely, revert your edits to the committed state and make no
change at all. The same hard rules apply. Rewrite the commit subject in $MSG_REL.

--- verification log (tail) ---
$(tail -n 60 "$log")
EOF
}

main() {
    local max=10 fresh=false dry_run=false
    while [ $# -gt 0 ]; do
        case "$1" in
            --new) fresh=true; shift ;;
            --dry-run) dry_run=true; shift ;;
            [0-9]*) max="$1"; shift ;;
            *)
                echo "usage: loop-modernise.sh [N] [--new] [--dry-run]" >&2
                echo "  N          cap the number of slices (default: 10)" >&2
                echo "  --new      fresh Claude context each slice (default: one resumed session)" >&2
                echo "  --dry-run  one slice, verified but never committed" >&2
                exit 2
                ;;
        esac
    done
    # A dry run that kept going would stack unverifiable slices on an uncommitted tree.
    [ "$dry_run" = true ] && max=1

    # Claude inherits this cwd, and the prompt names .modernise-msg relatively.
    cd "$root"

    # One loop per worktree: local-ci.sh bind-mounts the repo and writes .m2/ and
    # target/ inside it, so two concurrent runs corrupt each other's build state.
    exec 9> "$root/.git/loop-modernise.lock"
    if ! flock -n 9; then
        echo "loop-modernise: another run holds the lock in this worktree" >&2
        exit 1
    fi

    local session
    session="$(new_uuid)"

    echo "loop-modernise: target=$TARGET slices<=$max session=$session"
    echo "  verify per slice: local-ci.sh base + rows 8/11/17 + japicmp (~2 min)"
    echo "  commit: each green slice, no tags$([ "$dry_run" = true ] && echo ' (DISABLED: --dry-run)')"
    echo "  context: $([ "$fresh" = true ] && echo 'fresh per slice (--new)' || echo 'one resumed session')"
    echo "  api baseline: $([ -f "$root/$BASELINE_JAR" ] \
        && echo 'cached, resolves offline' \
        || echo 'fetched with an ephemeral settings.xml from gh auth token')"
    echo "  options: [N] cap slices | --new fresh context | --dry-run verify without committing"

    preflight

    local iteration=0 first=true
    while [ "$iteration" -lt "$max" ]; do
        local step=$((iteration + 1))
        echo
        echo "loop-modernise: slice $step of $max"
        : > "$log"
        rm -f "$msg_file"

        if [ "$fresh" = true ] || [ "$first" = true ]; then
            first=false
            [ "$fresh" = true ] && session="$(new_uuid)"
            claude --print --permission-mode acceptEdits --session-id "$session" "$(slice_prompt)"
        else
            claude --print --permission-mode acceptEdits --resume "$session" "$(slice_prompt)"
        fi

        guard_changes || return 0
        guard_strays
    guard_fenced
        warn_api_shape

        # Same command the container-only pre-commit hook runs. Formatting is not
        # environment-dependent (D44's warning is about golden pixel metrics), so
        # the host is a faithful place to run it.
        (cd "$root" && ./mvnw -q -B spotless:apply)

        if verify_all; then
            if [ "$dry_run" = true ]; then
                echo "loop-modernise: slice $step verified; left uncommitted (--dry-run)"
                return 0
            fi
            commit_slice "$step"
            iteration="$step"
            continue
        fi

        echo "loop-modernise: slice $step failed verification — one repair attempt"
        claude --print --permission-mode acceptEdits --resume "$session" "$(repair_prompt)"

        if [ -z "$(changed_paths)" ]; then
            echo "loop-modernise: repair reverted the slice; stopping" >&2
            rollback
            return 1
        fi
        guard_strays
    guard_fenced
        (cd "$root" && ./mvnw -q -B spotless:apply)

        if ! verify_all; then
            echo "loop-modernise: repair failed verification; rolling back and stopping" >&2
            echo "  full log: $LOG_REL" >&2
            rollback
            return 1
        fi
        if [ "$dry_run" = true ]; then
            echo "loop-modernise: repaired slice $step verified; left uncommitted (--dry-run)"
            return 0
        fi
        commit_slice "$step"
        iteration="$step"
    done

    echo
    echo "loop-modernise: stopped after $iteration committed slice(s) (cap $max)"
    reminder
}

# Prove the gates work and the tree is already green, so any later failure is
# attributable to a slice rather than inherited. Refusing to start beats looping
# with a gate silently skipped.
preflight() {
    echo
    echo "loop-modernise: preflight"
    if ! docker info > /dev/null 2>&1; then
        echo "loop-modernise: docker is not responding — local-ci.sh cannot run" >&2
        exit 1
    fi
    if [ -n "$(changed_paths)" ]; then
        echo "loop-modernise: working tree is dirty; commit or stash first" >&2
        changed_paths >&2
        exit 1
    fi
    : > "$log"
    if ! run_logged "$root/.devcontainer/ci/local-ci.sh"; then
        echo "loop-modernise: the tree fails verification before any slice" >&2
        exit 1
    fi
    if ! japicmp; then
        echo "loop-modernise: japicmp cannot gate the public API (see $LOG_REL)." >&2
        echo "  The v0.1.0 baseline comes from GitHub Packages, which needs read auth (D4)." >&2
        echo "  This run mints a settings.xml from 'gh auth token', so either:" >&2
        echo "    gh auth login && gh auth refresh -s read:packages" >&2
        echo "  or seed the cache once, after which the resolve needs no credential:" >&2
        echo "    ./mvnw install:install-file -Dmaven.repo.local=.m2/repository \\" >&2
        echo "      -Dfile=<thinlet-core-0.1.0.jar> -DpomFile=<thinlet-core-0.1.0.pom>" >&2
        exit 1
    fi
    echo "loop-modernise: preflight green (base verify + japicmp)"
}

# Every gating CI job, cheapest-failing-first. ci.yml's trace-diff job is
# deliberately absent: it is informational and never gates.
verify_all() {
    run_logged "$root/.devcontainer/ci/local-ci.sh" || return 1
    local jdk
    for jdk in 8 11 17; do
        run_logged "$root/.devcontainer/ci/local-ci.sh" "$jdk" || return 1
    done
    japicmp || return 1
}

guard_changes() {
    if [ -z "$(changed_paths)" ]; then
        echo "loop-modernise: no changes proposed — nothing left to modernise"
        reminder
        return 1
    fi
}

# Anything outside the target tree is out of scope by construction, which is what
# keeps the frozen artifacts safe: thinlet.dtd, the XML corpus, the golden traces
# under src/test/resources/trace/, the 2005 *.gif icons, and scripts/ itself.
fenced_methods() {
    cat << 'NAMES'
Object parse(InputStream inputstream, char mode
void parseXML(
Object parseDOM(
String getDOMAttribute(
String getDOMText(
int getDOMCount(
Object getDOMNode(
NAMES
}

# Line numbers changed in FENCED_FILE, one per line, from the unified-0 hunks.
changed_lines() {
    git -C "$root" diff -U0 -- "$FENCED_FILE" | awk '
        /^@@/ {
            split($3, a, ",")
            sub(/^\+/, "", a[1])
            count = (a[2] == "" ? 1 : a[2])
            for (i = 0; i < count; i++) print a[1] + i
        }'
}

# The enclosing method signature of one line, or "" above the first method.
enclosing_method() {
    awk -v n="$1" '
        NR <= n && /^    (private|protected|public).*\(/ { m = $0 }
        NR == n { print m; exit }' "$root/$FENCED_FILE"
}

# Refuses a slice that reached into the fenced parser. Advisory prompt rules are
# not enough on their own — this is the same distrust guard_strays encodes.
guard_fenced() {
    if ! git -C "$root" diff --quiet -- "$FENCED_FILE"; then
        local line method hit=""
        while read -r line; do
            [ -z "$line" ] && continue
            method="$(enclosing_method "$line")"
            [ -z "$method" ] && continue
            if printf '%s\n' "$method" | grep -qFf <(fenced_methods); then
                hit="$hit
  line $line: $(echo "$method" | sed 's/^ *//')"
            fi
        done <<< "$(changed_lines)"
        if [ -n "$hit" ]; then
            echo "loop-modernise: slice touched the fenced XML parser — rolling back and stopping" >&2
            echo "  the parser's fate is an open ROADMAP 3c decision; do not modernise it yet" >&2
            echo "$hit" >&2
            rollback
            exit 1
        fi
    fi
}

guard_strays() {
    local strays new
    strays="$(changed_paths | grep -v "^$TARGET/" || true)"
    if [ -n "$strays" ]; then
        echo "loop-modernise: change outside $TARGET — rolling back and stopping" >&2
        echo "$strays" >&2
        rollback
        exit 1
    fi
    new="$(git -C "$root" status --porcelain=v1 --untracked-files=all -- "$TARGET" | grep '^??' || true)"
    if [ -n "$new" ]; then
        echo "loop-modernise: new file under $TARGET — extraction is gated Cut 4/5/6 work" >&2
        echo "$new" >&2
        rollback
        exit 1
    fi
}

# A heads-up, not a gate: reformatting an existing declaration reads as an
# addition here. japicmp in verify_all is the authority on the published API.
warn_api_shape() {
    local added
    added="$(git -C "$root" diff -U0 -- "$TARGET" | grep -E '^\+[[:space:]]*(public|protected)[[:space:]]' || true)"
    if [ -n "$added" ]; then
        echo "loop-modernise: note — diff adds public/protected declarations (D43); japicmp decides"
        echo "$added"
    fi
}

commit_slice() {
    local step="$1" subject
    if [ -s "$msg_file" ]; then
        subject="$(head -n 1 "$msg_file")"
    else
        subject="Modernise a Java-8 idiom slice (loop pass $step)"
    fi
    rm -f "$msg_file"
    git -C "$root" add -- "$TARGET"
    # --no-verify: the pre-commit framework shim is installed only in the dev
    # container, so the hook exits 1 on the host. Its one job is spotless:apply,
    # which this script already ran against the same config.
    git -C "$root" commit --no-verify \
        -m "$subject" \
        -m "Co-Authored-By: $COAUTHOR <noreply@anthropic.com>"
    echo "loop-modernise: committed slice $step — $subject"
}

reminder() {
    echo "loop-modernise: before opening a PR, run scripts/comment-pass.sh, do the review,"
    echo "  then scripts/comment-pass.sh done (D60 blocks gh pr create on Java branches)."
}

main "$@"
