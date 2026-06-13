# Dev Container image for thinlet-modernized.
#
# The CI matrix reuses this exact Dockerfile per JDK target, passing
# --build-arg JDK_VERSION=<8|11|17|21|25>; only the JDK switches, the rest of
# the image (fonts, fontconfig, Xvfb, pre-commit) is identical so font metrics
# share one glyph source across the matrix.
ARG JDK_VERSION=21
FROM mcr.microsoft.com/devcontainers/java:1-${JDK_VERSION}-bookworm

# Fixed font package set + fontconfig + headless display + pre-commit.
# The font *package set* is pinned by name here; exact apt version pinning is an
# open item (see DECISIONS.md / README). DTD and source bytes are unaffected.
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
        xvfb \
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

# pre-commit framework (runs `mvn spotless:apply` before commit).
RUN pipx install pre-commit \
 && pipx ensurepath
