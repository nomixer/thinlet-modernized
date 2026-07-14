#!/usr/bin/env bash
# The Java comment-pass gate (DECISIONS.md D60). Every PR that changes Java gets
# a comment review (D57 rules) before it is opened: a Claude Code hook blocks
# `gh pr create` until the pass is attested for the current HEAD, so the pass
# cannot be forgotten. Re-attest after every new commit (the marker is per-SHA).
#
#   scripts/comment-pass.sh          # show the checklist + the Java files to review
#   scripts/comment-pass.sh done     # attest: the pass was completed for the current HEAD
#   scripts/comment-pass.sh hook     # (internal) Claude Code PreToolUse hook entry point
#
# COMMENT_PASS_BASE overrides the diff base (default: main).
set -euo pipefail

root="$(git rev-parse --show-toplevel)"
marker="$root/.git/java-comment-pass"
base="${COMMENT_PASS_BASE:-main}"

java_files() { git -C "$root" diff --name-only "$base"...HEAD -- '*.java'; }

checklist() {
    cat << 'EOF'
Java comment pass (DECISIONS.md D57/D60) — review the changed files, then attest:
  1. New/changed code: comment ONLY what code cannot say (invariants, interning
     anchors, preserved quirks, evaluation-order hazards, dead-2005 params) —
     pin-or-tag: checkable beneath, a cited test, or "// UNVERIFIED:".
  2. Staleness: did this change make any nearby comment wrong? Fix or delete it.
  3. Touched files carrying pre-D57 verbose javadoc (Renderer, IconTextSpec,
     Thinlet.is): trim opportunistically to <=3 lines + a D-pointer.
  4. New files: license header + <=3-line class doc + DECISIONS.md D-pointer.
  5. Prose docs in the diff: present-tense claims true today, history in past
     tense — section labels exempt nothing; only DECISIONS.md keeps its
     entries' original tense (D66).
Attest with: scripts/comment-pass.sh done
EOF
}

case "${1:-show}" in
    show)
        files="$(java_files)"
        if [ -z "$files" ]; then
            echo "No Java changes vs $base — no comment pass needed."
        else
            checklist
            echo
            echo "Java files changed vs $base:"
            echo "$files"
        fi
        ;;
    done)
        git -C "$root" rev-parse HEAD > "$marker"
        echo "Attested: comment pass done for $(git -C "$root" rev-parse --short HEAD)." \
            "New commits invalidate this — re-attest before gh pr create."
        ;;
    hook)
        cmd="$(jq -r '.tool_input.command // empty')"
        case "$cmd" in *"gh pr create"*) ;; *) exit 0 ;; esac
        [ -z "$(java_files)" ] && exit 0
        if [ -f "$marker" ] && [ "$(cat "$marker")" = "$(git -C "$root" rev-parse HEAD)" ]; then
            exit 0
        fi
        reason="Java comment pass not attested for this HEAD (D60). Run scripts/comment-pass.sh to see the checklist and the changed Java files, do the review (apply any comment fixes and commit them), then run scripts/comment-pass.sh done and retry gh pr create."
        printf '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":%s}}\n' \
            "$(printf '%s' "$reason" | jq -Rs .)"
        ;;
    *)
        echo "unknown mode: $1 (modes: <none> = show checklist+files, done = attest, hook = internal)" >&2
        exit 1
        ;;
esac
