#!/usr/bin/env bash
# Mutation coverage for one test class (DECISIONS.md D92) — the teeth gate a
# coverage number cannot provide: D90 proved a line can be executed, recorded and
# still unwatched, so "did this test detect a change to the code it covers?" is a
# separate question from "did the line run?".
#
#   scripts/mutation.sh <TestClass>      # PIT scoped to that class; list survivors
#   scripts/mutation.sh <TestClass> --gate <Method> [--lines 12,34]
#                                        # …and pass/fail on those lines of it
#   scripts/mutation.sh --report-only    # re-report from the last run
#
# Scoped by targetTests so PIT only runs mutants on lines the named test covers;
# unscoped it would mutate all 6 000 lines of Thinlet.java.
#
# Runs in the CI container for the same reason local-ci.sh does (D44): the net
# needs the pinned fonts and Xvfb :99. The image is read from local-ci.sh so the
# two never drift. Writes only under target/ — never under .git, which is a FILE
# in a linked worktree (#131/#134).
set -euo pipefail

root="$(git rev-parse --show-toplevel)"
IMAGE="$(sed -n 's/^IMAGE=//p' "$root/.devcontainer/ci/local-ci.sh")"
xml="$root/thinlet-core/target/pit-reports/mutations.xml"

report_only=false
target=""
gate=""
gate_lines=""
want=""
for a in "$@"; do
    if [ -n "$want" ]; then
        case "$want" in gate) gate="$a" ;; lines) gate_lines="$a" ;; esac
        want=""
        continue
    fi
    case "$a" in
        --report-only) report_only=true ;;
        --gate) want=gate ;;
        --lines) want=lines ;;
        -*)
            echo "usage: mutation.sh <TestClass> [--gate <Method>] | --report-only" >&2
            echo "  <TestClass>     run PIT with targetTests scoped to this class" >&2
            echo "  --gate <Method> exit non-zero unless that scope has kills and no survivors" >&2
            echo "  --lines <csv>   narrow the gate to these source lines of that method" >&2
            echo "  --report-only   skip the run; report from the existing mutations.xml" >&2
            exit 2
            ;;
        *) target="$a" ;;
    esac
done
if [ "$report_only" = false ] && [ -z "$target" ]; then
    echo "mutation: name a test class, or pass --report-only" >&2
    echo "  usage: mutation.sh <TestClass> | --report-only" >&2
    exit 2
fi

echo "mutation: image=$IMAGE target=${target:-<report-only>}"
echo "  options: <TestClass> scope the run | --gate <Method> [--lines <csv>] pass/fail | --report-only reuse the last run"

if [ "$report_only" = false ]; then
    rm -f "$xml"
    echo "mutation: running PIT (test-compile + mutationCoverage)"
    # DISPLAY is exported rather than set in the plugin: PIT forks minion JVMs
    # that inherit the environment, and XvfbDisplayExtension boots :99 itself.
    docker run --rm --user vscode \
        -v "$root":/workspaces/thinlet-modernized \
        -w /workspaces/thinlet-modernized \
        -e DISPLAY=:99 \
        "$IMAGE" bash -c "MAVEN_USER_HOME=\"\$PWD/.m2\" ./mvnw -B -ntp \
            -Dmaven.repo.local=.m2/repository -pl thinlet-core -Pmutation \
            -Dpit.targetTests='$target' \
            test-compile org.pitest:pitest-maven:mutationCoverage"
fi

[ -f "$xml" ] || { echo "mutation: no report at $xml" >&2; exit 1; }
GATE_METHOD="$gate" GATE_LINES="$gate_lines" python3 "$root/scripts/mutation-summary.py" "$xml"
echo
echo "mutation: HTML at thinlet-core/target/pit-reports/index.html"
