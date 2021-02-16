#!/bin/bash
set -euo pipefail

ARCHIVE_ROOT="/Volumes/Multimedia/Photos/"
FAVORITES_DIR="Favorites"

FAVORITES_FILES=$(osascript scripts/osa/list-favorite-photos.js 2>&1 | jq -r '.[].archivePath')

cd "$ARCHIVE_ROOT"

for file in $FAVORITES_FILES; do
  link="${FAVORITES_DIR}/$(basename $file)"

  if [ -f "$file" ] && [ ! -h "$link" ]; then
    ln -sv "../$file" "$link"
  fi
done
