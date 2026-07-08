#!/usr/bin/env bash
# Faithful local CI run (DECISIONS.md D44): execute the same golden+input net CI
# runs, inside the same dev-container image CI uses (pulled from GHCR), so the
# goldens compare against the pinned fonts + Xvfb :99 — not the bare host's
# fonts, which produce false ±2 px diffs (D22/D42). This is the Cut 2 iteration
# loop: ~30 s warm for the full JDK-21 verify.
#
# Usage (from anywhere in the repo):
#   .devcontainer/ci/local-ci.sh          # base row: JDK-21 full verify (lint + net + robot)
#   .devcontainer/ci/local-ci.sh 8        # crossjdk row: tests forked on /opt/jdk8 (robot excluded)
#   .devcontainer/ci/local-ci.sh 11       # ditto JDK 11
#   .devcontainer/ci/local-ci.sh 17       # ditto JDK 17
#
# The image is the exact one CI publishes (pushed by main-branch runs, D23);
# `docker pull "$IMAGE"` refreshes it after the Dockerfile changes. Maven writes
# to the workspace .m2 exactly as CI does, so the host ~/.m2 is untouched. The
# container user `vscode` is uid/gid 1000 — matching the common single-user
# host, so workspace files keep their ownership.
set -euo pipefail

IMAGE=ghcr.io/nomixer/thinlet-modernized/devcontainer-ci:latest
root="$(git rev-parse --show-toplevel)"
jdk="${1:-}"

if [ -z "$jdk" ]; then
  # Mirrors ci.yml `build` (minus the trace-dump knob, which is for the D33 diff)
  cmd='MAVEN_USER_HOME="$PWD/.m2" ./mvnw -B -Dmaven.repo.local=.m2/repository verify'
else
  # Mirrors ci.yml `test` matrix row (same flags, same robot exclusion), scoped
  # to thinlet-core (-pl … -am): the whole suite lives there, and the unscoped
  # reactor `test` only works from CI's always-clean checkout — in a local
  # workspace with populated target/ dirs, surefire in the test-less demos
  # module skips its no-tests early-exit and dies on `excludedGroups` requiring
  # a JUnit engine.
  cmd='MAVEN_USER_HOME="$PWD/.m2" ./mvnw -B -Dmaven.repo.local=.m2/repository -pl thinlet-core -am -Pcrossjdk -Djdk.target='"$jdk"' -DexcludedGroups=robot -t .mvn/toolchains.xml test'
fi

exec docker run --rm --user vscode \
  -v "$root":/workspaces/thinlet-modernized \
  -w /workspaces/thinlet-modernized \
  "$IMAGE" bash -c "$cmd"
