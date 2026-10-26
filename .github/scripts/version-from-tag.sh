#!/usr/bin/env bash
# Turns a tag like v1.2.3 or v1.2.3-beta.1 into versionName / versionCode / prerelease outputs.
# versionCode = major * 10000 + minor * 100 + patch, so 1.2.3 -> 10203.
set -euo pipefail

tag="${1:?usage: version-from-tag.sh <tag>}"
if [[ ! "$tag" =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)(-[0-9A-Za-z.-]+)?$ ]]; then
  echo "::error::Tag '$tag' is not a version tag like v1.2.3 or v1.2.3-beta.1" >&2
  exit 1
fi
major="${BASH_REMATCH[1]}"; minor="${BASH_REMATCH[2]}"; patch="${BASH_REMATCH[3]}"; suffix="${BASH_REMATCH[4]}"
if (( minor > 99 || patch > 99 )); then
  echo "::error::Minor and patch must be at most 99 (got $tag)" >&2
  exit 1
fi

code=$(( major * 10000 + minor * 100 + patch ))
if (( code == 0 )); then
  echo "::error::Version must be greater than 0.0.0" >&2
  exit 1
fi

echo "version_name=${tag#v}"
echo "version_code=$code"
echo "prerelease=$([[ -n "$suffix" ]] && echo true || echo false)"
