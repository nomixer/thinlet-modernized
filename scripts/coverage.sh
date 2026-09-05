#!/usr/bin/env bash
# Line/branch coverage for thinlet-core's library classes, measured over the WHOLE
# net (DECISIONS.md D90) — thinlet-core's suites plus the thinlet-drafts
# playthrough, which D89 found is the only thing exercising some lines.
#
#   scripts/coverage.sh              # run the net under the agent, then report
#   scripts/coverage.sh --report-only  # re-report from an existing target/jacoco.exec
#   scripts/coverage.sh --uncovered    # …and list every wholly-uncovered method
#   scripts/coverage.sh --worklist     # …and list partially-covered methods with
#                                      #   the exact lines still missed (D93)
#
# Runs in the CI container for the same reason local-ci.sh does (D44): the net
# needs the pinned fonts and Xvfb :99. The image is read from local-ci.sh so the
# two never drift.
#
# Writes only under target/ — never under .git, which is a FILE in a linked
# worktree (#131/#134).
set -euo pipefail

root="$(git rev-parse --show-toplevel)"
IMAGE="$(sed -n 's/^IMAGE=//p' "$root/.devcontainer/ci/local-ci.sh")"
exec_file="target/jacoco.exec"
xml="$root/thinlet-core/target/site/jacoco/jacoco.xml"

report_only=false
uncovered=false
worklist=false
for a in "$@"; do
    case "$a" in
        --report-only) report_only=true ;;
        --uncovered) uncovered=true ;;
        --worklist) worklist=true ;;
        *)
            echo "usage: coverage.sh [--report-only] [--uncovered]" >&2
            echo "  --report-only  skip the test run; report from the existing $exec_file" >&2
            echo "  --uncovered    also list every method with zero covered instructions" >&2
            echo "  --worklist     also list partially-covered methods + their missed lines" >&2
            exit 2
            ;;
    esac
done

echo "coverage: image=$IMAGE exec=$exec_file"
echo "  options: --report-only reuse the last run | --uncovered list dead methods | --worklist list missed lines"

run_in_container() {
    docker run --rm --user vscode \
        -v "$root":/workspaces/thinlet-modernized \
        -w /workspaces/thinlet-modernized \
        "$IMAGE" bash -c "$1"
}

if [ "$report_only" = false ]; then
    rm -f "$root/$exec_file"
    echo "coverage: running the net under the agent (-Pcoverage)"
    run_in_container 'MAVEN_USER_HOME="$PWD/.m2" ./mvnw -B -Dmaven.repo.local=.m2/repository -pl thinlet-core,thinlet-drafts -am -Pcoverage test'
fi

if [ ! -f "$root/$exec_file" ]; then
    echo "coverage: no $exec_file — run without --report-only first" >&2
    exit 1
fi

echo "coverage: writing the report for thinlet-core"
run_in_container 'MAVEN_USER_HOME="$PWD/.m2" ./mvnw -q -B -Dmaven.repo.local=.m2/repository -pl thinlet-core -Pcoverage \
    org.jacoco:jacoco-maven-plugin:report -Djacoco.dataFile=$PWD/target/jacoco.exec'

[ -f "$xml" ] || { echo "coverage: report missing at $xml" >&2; exit 1; }
UNCOVERED="$uncovered" WORKLIST="$worklist" python3 "$root/scripts/coverage-summary.py" "$xml"
echo
echo "coverage: HTML at thinlet-core/target/site/jacoco/index.html"
