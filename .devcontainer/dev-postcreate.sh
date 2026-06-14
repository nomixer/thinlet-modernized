#!/usr/bin/env bash
# Post-create for the FULL dev image only (.devcontainer/devcontainer.json).
# The lean CI config (.devcontainer/ci/devcontainer.json) does NOT run this.
set -u

# 1) Make the persisted ~/.m2 named volume writable by vscode (D19). Runs on
#    every rebuild, so it also repairs an already-root-owned volume.
sudo chown -R vscode:vscode /home/vscode/.m2 || true

# 2) Install the pre-commit git hook, but only when git is usable — a linked
#    worktree's git is non-functional inside the container (D20).
if git rev-parse --git-dir >/dev/null 2>&1; then
  pre-commit install || true
fi

# 3) Print the noVNC desktop hint once per interactive terminal (D22). Installed
#    into /etc/bash.bashrc (sourced by interactive non-login bash, which is what
#    VS Code terminals are), idempotently.
hint='if [ -z "${THINLET_NOVNC_HINT-}" ] && [ -t 1 ]; then export THINLET_NOVNC_HINT=1; printf "\n  noVNC desktop: open forwarded port 6080 in a browser (password: vscode)\n  (if 6080 is remapped, see the VS Code Ports panel)\n\n"; fi'
if ! sudo grep -qF THINLET_NOVNC_HINT /etc/bash.bashrc 2>/dev/null; then
  printf '\n# thinlet: one-time noVNC desktop hint per interactive shell (DECISIONS.md D22)\n%s\n' "$hint" \
    | sudo tee -a /etc/bash.bashrc >/dev/null
fi
