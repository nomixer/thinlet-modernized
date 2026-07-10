#!/usr/bin/env bash
# Build and launch a bundled Thinlet example (thinlet-demos + thinlet-drafts) in an
# AWT Frame. These are the original 2005 sample apps, preserved as-is; they need a
# display. The script ALWAYS compiles the needed module first (incrementally, ~a
# couple of seconds) and then launches it — you never build separately, and it is
# never stale. Full guide: project-docs/RUNNING-EXAMPLES.md
#
#   scripts/example.sh                   # list the examples
#   scripts/example.sh demo              # build (incrementally) + launch by short name
#   scripts/example.sh --no-build demo   # skip the rebuild for a fast relaunch
#   DISPLAY=:0 scripts/example.sh demo   # override the display
#
# Display: uses $DISPLAY if set (in the Dev Container it is :1, the noVNC desktop
# on forwarded port 6080).
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

# The catalogue: "short-name | module | fully.qualified.MainClass | description".
# Single source of truth — the listing and the resolver both read this.
CATALOGUE='
demo|thinlet-demos|thinlet.demo.Demo|widget showcase (the main demo)
calculator|thinlet-demos|thinlet.demo.Calculator|small calculator
amazon|thinlet-demos|thinlet.amazon.AmazonExplorer|Amazon explorer — UI only; its 2005 web service is long gone
drafts|thinlet-drafts|thinlet.drafts.Drafts|draft-samples showcase (lists, tree, tabs, MDI, chart, choosers, ...)
swing|thinlet-drafts|thinlet.drafts.SwingProperties|Swing-look reproduction
'

list() {
  echo "Usage: scripts/example.sh [--no-build] <name>"
  echo
  echo "Builds the example's module (incrementally) and launches it — no separate build step."
  echo "  --no-build   skip the rebuild for a fast relaunch (builds anyway if never built)"
  echo
  echo "Examples:"
  while IFS='|' read -r name module cls desc; do
    [[ -z "$name" ]] && continue
    printf '  %-11s %s\n' "$name" "$desc"
  done <<< "$CATALOGUE"
  echo
  echo "Display: DISPLAY=${DISPLAY:-<unset>} (Dev Container desktop is :1 -> noVNC on port 6080)."
}

# Build by default; --no-build is an opt-out for speed, never needed for correctness.
build=true
if [[ "${1:-}" == "--no-build" || "${1:-}" == "-n" ]]; then
  build=false
  shift
fi

name="${1:-}"
if [[ -z "$name" || "$name" == "-h" || "$name" == "--help" || "$name" == "--list" ]]; then
  list
  [[ -z "$name" ]] && exit 1 || exit 0
fi

# Resolve the short name against the catalogue.
module=""; mainclass=""; desc=""
while IFS='|' read -r n m c d; do
  if [[ "$n" == "$name" ]]; then module="$m"; mainclass="$c"; desc="$d"; fi
done <<< "$CATALOGUE"

if [[ -z "$module" ]]; then
  echo "Unknown example: '$name'" >&2
  echo >&2
  list >&2
  exit 1
fi

# Always build (incrementally) so a launch is never stale — no flag to remember. The
# hint is printed *before* the build kicks off, so the --no-build escape hatch is
# discoverable in the normal output (no --help / source-reading needed). --no-build
# still builds if the class was never compiled, so it can never fail confusingly.
classfile="$module/target/classes/${mainclass//.//}.class"
if $build; then
  echo ">> building $module (incremental; thinlet-core + module) — pass --no-build to skip"
  ./mvnw -q -pl "$module" -am -DskipTests package
elif [[ ! -f "$classfile" ]]; then
  echo ">> --no-build given, but $mainclass isn't built yet — building it once ..."
  ./mvnw -q -pl "$module" -am -DskipTests package
else
  echo ">> skipping build (--no-build)"
fi

if [[ -z "${DISPLAY:-}" ]]; then
  export DISPLAY=:1
  echo ">> DISPLAY was unset; defaulting to :1 (the Dev Container noVNC desktop)."
fi

cp="thinlet-core/target/classes:$module/target/classes"
echo ">> launching $mainclass on DISPLAY=$DISPLAY  ($desc)"
exec java -cp "$cp" "$mainclass"
