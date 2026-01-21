#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

LOADER="${LOADER:-${TARGET_LOADER:-}}"
MC_VERSION="${MC_VERSION:-${mcVersion:-}}"
BUILD_TAG="${UEBLICHE_BUILD_TAG:-${TAG:-${BUILD_TAG:-}}}"
if [[ -z "$BUILD_TAG" ]]; then
  git_hash="$(git -C "$ROOT_DIR" rev-parse --short=8 HEAD 2>/dev/null || true)"
  if [[ -n "$git_hash" ]]; then
    BUILD_TAG="$(date +%Y.%m.%d)-$git_hash"
  fi
fi
if [[ -z "$BUILD_TAG" ]]; then
  BUILD_TAG="dev"
fi

if [[ -z "$LOADER" ]]; then
  echo "Missing LOADER (set via Uebliche.dev targetSelect)." >&2
  exit 1
fi

IFS=',' read -r -a LOADER_LIST <<< "$LOADER"
rm -rf "$ROOT_DIR/release"
mkdir -p "$ROOT_DIR/release"

resolve_velocity_version() {
  local mc_version="$1"
  local version=""
  version="$(./gradlew -q :loader-velocity:properties \
    | awk -F': ' '/velocityVersion:/ {print $2; exit}' \
    | tr -d '\r')"
  if [[ -z "$version" && -n "$mc_version" ]]; then
    version="$(./gradlew -q :loader-velocity:properties "-PmcVersion=$mc_version" \
      | awk -F': ' '/velocityVersion:/ {print $2; exit}' \
      | tr -d '\r')"
  fi
  echo "$version"
}

build_loader() {
  local loader="$1"
  local loader_dir="$ROOT_DIR/loader-$loader"

  if [[ ! -d "$loader_dir" ]]; then
    echo "Loader directory not found: $loader_dir" >&2
    return 1
  fi

  local gradle_args=("--build-cache" "--parallel")
  if [[ -n "$MC_VERSION" ]]; then
    gradle_args+=("-PmcVersion=$MC_VERSION")
  fi
  local resolved_tag=""
  if [[ -n "$BUILD_TAG" ]]; then
    resolved_tag="$BUILD_TAG"
    if [[ "$resolved_tag" == *"+"* ]]; then
      :
    elif [[ "$loader" == "velocity" ]]; then
      local velocity_version=""
      velocity_version="$(resolve_velocity_version "$MC_VERSION" || true)"
      if [[ "$velocity_version" == velocity-* ]]; then
        velocity_version="${velocity_version#velocity-}"
      fi
      if [[ -z "$velocity_version" && "$MC_VERSION" == velocity-* ]]; then
        velocity_version="${MC_VERSION#velocity-}"
      fi
      if [[ -z "$velocity_version" && -n "$MC_VERSION" ]]; then
        velocity_version="$MC_VERSION"
      fi
      if [[ -n "$velocity_version" ]]; then
        resolved_tag="${resolved_tag}+velocity+${velocity_version}"
      else
        resolved_tag="${resolved_tag}+${loader}"
      fi
    elif [[ -n "$MC_VERSION" ]]; then
      local normalized_version="$MC_VERSION"
      if [[ "$loader" == "paper" && "$normalized_version" == paper-* ]]; then
        normalized_version="${normalized_version#paper-}"
      fi
      resolved_tag="${resolved_tag}+${loader}+${normalized_version}"
    else
      resolved_tag="${resolved_tag}+${loader}"
    fi
    gradle_args+=("-Ptag=$resolved_tag")
    gradle_args=("--rerun-tasks" "--no-build-cache" "--parallel")
    if [[ -n "$MC_VERSION" ]]; then
      gradle_args+=("-PmcVersion=$MC_VERSION")
    fi
    gradle_args+=("-Ptag=$resolved_tag")
  fi

  pushd "$ROOT_DIR" >/dev/null
  ./gradlew ":loader-$loader:build" "${gradle_args[@]}"
  popd >/dev/null

  shopt -s nullglob
  local jar_candidates=("$loader_dir"/build/libs/*.jar)
  shopt -u nullglob

  if [[ ${#jar_candidates[@]} -eq 0 ]]; then
    echo "No jar artifacts found in $loader_dir/build/libs" >&2
    return 1
  fi

  local jar_path
  jar_path="$(ls -t "${jar_candidates[@]}" | grep -vE '(-sources|-javadoc)\\.jar$' | head -n 1)"
  if [[ -z "$jar_path" ]]; then
    echo "No release jar found in $loader_dir/build/libs" >&2
    return 1
  fi

  local release_dir="$ROOT_DIR/release/$loader"
  rm -rf "$release_dir"
  mkdir -p "$release_dir"

  local jar_name
  jar_name="$(basename "$jar_path")"
  if [[ "$loader" == "velocity" && "$jar_name" == *"+velocity+"*"-velocity-"* ]]; then
    local velocity_prefix="${jar_name%%-velocity-*}"
    if [[ "$velocity_prefix" == *"+velocity+"* ]]; then
      jar_name="${velocity_prefix}.jar"
    fi
  fi
  if [[ "$loader" == "paper" && "$jar_name" == *"+paper+"*"-paper-"* ]]; then
    local paper_prefix="${jar_name%%-paper-*}"
    if [[ "$paper_prefix" == *"+paper+"* ]]; then
      jar_name="${paper_prefix}.jar"
    fi
  fi
  cp -f "$jar_path" "$release_dir/$jar_name"
  local release_file="$release_dir/$jar_name"

  printf "Release prepared:\n- %s\n" "$release_file"
}

for raw_loader in "${LOADER_LIST[@]}"; do
  loader="$(echo "$raw_loader" | xargs)"
  if [[ -z "$loader" ]]; then
    continue
  fi
  build_loader "$loader"
done
