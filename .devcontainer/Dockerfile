# Dev Container image for thinlet-modernized.
#
# The CI matrix reuses this exact Dockerfile per JDK target, passing
# --build-arg JDK_VERSION=<8|11|17|21|25>; only the JDK switches, the rest of
# the image (fonts, fontconfig, Xvfb, pre-commit) is identical so font metrics
# share one glyph source across the matrix.
ARG JDK_VERSION=21
FROM mcr.microsoft.com/devcontainers/java:1-${JDK_VERSION}-bookworm

# Fonts + fontconfig + headless display (Xvfb) + the X11 client libraries the
# JDK's AWT (libawt_xawt.so) dynamically links at Toolkit init + pre-commit.
# Without libXtst/libXi/etc. ANY AWT program (the demos, and the Phase 1
# headless trace tests) dies with UnsatisfiedLinkError before it opens a display.
# The font *package set* is pinned by name; exact apt version pinning is an open
# item (see DECISIONS.md / README). DTD and source bytes are unaffected.
#
# The base image ships a third-party yarn apt source (dl.yarnpkg.com) whose
# signing key is not present on CI runners, so a plain `apt-get update` fails on
# it (exit 100) before our packages install. We do not use yarn, so drop any
# yarn apt source (matched by content, filename-agnostic) before updating.
RUN find /etc/apt/sources.list.d -type f -exec grep -lq 'dl\.yarnpkg\.com' {} \; -delete ; \
    apt-get update \
 && apt-get install -y --no-install-recommends \
        xvfb \
        libxtst6 \
        libxi6 \
        libxrender1 \
        libxext6 \
        libx11-6 \
        libxrandr2 \
        fontconfig \
        fonts-dejavu-core \
        fonts-dejavu-extra \
        fonts-liberation \
        pipx \
 && rm -rf /var/lib/apt/lists/*

# Deterministic font rendering: pin a fontconfig drop-in so hinting/antialias
# behavior does not vary by host. (Metric *math* still varies by JDK — that is
# what the trace tolerance model absorbs; see DECISIONS.md D7.)
COPY fonts/10-thinlet-pinned.conf /etc/fonts/conf.d/10-thinlet-pinned.conf
RUN fc-cache -f

# pre-commit framework, installed to /usr/local/bin so it is on PATH for the
# vscode user (pipx's default would install under root's home, invisible to
# vscode). The git hook itself is defined in .pre-commit-config.yaml.
RUN PIPX_HOME=/usr/local/pipx PIPX_BIN_DIR=/usr/local/bin pipx install pre-commit

# CLI `mvn` delegates to the project's Maven wrapper, so an interactive `mvn` is
# byte-for-byte the pinned version CI runs (no separate SDKMAN Maven to drift).
# Resolves the wrapper from the repo root, so it works from any subdirectory.
RUN printf '%s\n' \
      '#!/usr/bin/env bash' \
      'root="$(git -C "$PWD" rev-parse --show-toplevel 2>/dev/null || true)"' \
      'exec "${root:-$PWD}/mvnw" "$@"' \
      > /usr/local/bin/mvn \
 && chmod +x /usr/local/bin/mvn
