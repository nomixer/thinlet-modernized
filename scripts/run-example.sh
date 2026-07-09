#!/usr/bin/env bash
# Launch a bundled Thinlet example (thinlet-demos + thinlet-drafts) in an AWT Frame.
# These are the original 2005 sample apps, preserved as-is; they need a display.
# Full guide: project-docs/RUNNING-EXAMPLES.md
#
#   scripts/run-example.sh              # list the examples
#   scripts/run-example.sh demo         # launch by short name
#   scripts/run-example.sh --build drafts   # force a rebuild first
#
# Display: uses $DISPLAY if set (in the Dev Container it is :1, the noVNC desktop
# on forwarded port 6080). Override per run, e.g.  DISPLAY=:0 scripts/run-example.sh demo
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
  echo "Usage: scripts/run-example.sh [--build] <name>"
  echo
  echo "Examples:"
  while IFS='|' read -r name module cls desc; do
    [[ -z "$name" ]] && continue
    printf '  %-11s %s\n' "$name" "$desc"
  done <<< "$CATALOGUE"
  echo
  echo "Display: DISPLAY=${DISPLAY:-<unset>} (Dev Container desktop is :1 -> noVNC on port 6080)."
}

build=false
if [[ "${1:-}" == "--build" || "${1:-}" == "-b" ]]; then
  build=true
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

# Build the module (and its deps) if the class is missing, or when forced.
classfile="$module/target/classes/${mainclass//.//}.class"
if $build || [[ ! -f "$classfile" ]]; then
  echo ">> building $module (and thinlet-core) ..."
  ./mvnw -q -pl "$module" -am -DskipTests package
fi

if [[ -z "${DISPLAY:-}" ]]; then
  export DISPLAY=:1
  echo ">> DISPLAY was unset; defaulting to :1 (the Dev Container noVNC desktop)."
fi

cp="thinlet-core/target/classes:$module/target/classes"
echo ">> launching $mainclass on DISPLAY=$DISPLAY  ($desc)"
exec java -cp "$cp" "$mainclass"
