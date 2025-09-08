#!/bin/bash
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)" # Contents/MacOS
APP_RES="$DIR/../Resources/app"
APP_JRE="$DIR/../Resources/jre"
USER_JRE_BASE="$HOME/Library/Application Support/Ramus/jre"

# Resolve Java
JAVA_BIN=""

# Prefer embedded JRE if present
ARCH="$(uname -m)"
case "$ARCH" in
  arm64)
    [[ -x "$APP_JRE/arm64/bin/java" ]] && JAVA_BIN="$APP_JRE/arm64/bin/java"
    ;;
  x86_64)
    [[ -x "$APP_JRE/x86_64/bin/java" ]] && JAVA_BIN="$APP_JRE/x86_64/bin/java"
    ;;
esac

ensure_user_jre() {
  local arch="$1"
  local dest_dir="$USER_JRE_BASE/$arch"
  local bin_java="$dest_dir/bin/java"
  if [[ -x "$bin_java" ]]; then echo "$bin_java"; return 0; fi
  mkdir -p "$dest_dir" || true
  # Select Adoptium API URLs
  local jre_url jdk_url
  if [[ "$arch" == "arm64" ]]; then
    jre_url="https://api.adoptium.net/v3/binary/latest/11/ga/mac/aarch64/jre/hotspot/normal/adoptium"
    jdk_url="https://api.adoptium.net/v3/binary/latest/11/ga/mac/aarch64/jdk/hotspot/normal/adoptium"
  else
    jre_url="https://api.adoptium.net/v3/binary/latest/11/ga/mac/x64/jre/hotspot/normal/adoptium"
    jdk_url="https://api.adoptium.net/v3/binary/latest/11/ga/mac/x64/jdk/hotspot/normal/adoptium"
  fi
  local tmpd
  tmpd="$(mktemp -d)"
  local archive="$tmpd/jre.tgz"
  echo "Fetching Java Runtime for ${arch}..."
  if ! curl -Lsf "$jre_url" -o "$archive"; then
    echo "JRE fetch failed, trying JDK..."
    if ! curl -Lsf "$jdk_url" -o "$archive"; then
      echo "Failed to download Java runtime. Falling back to system Java if available." >&2
      rm -rf "$tmpd"
      return 1
    fi
  fi
  mkdir -p "$tmpd/extract"
  if ! tar -xzf "$archive" -C "$tmpd/extract" 2>/dev/null; then
    # maybe it's a zip
    if command -v unzip >/dev/null 2>&1; then
      unzip -q "$archive" -d "$tmpd/extract" || true
    fi
  fi
  # Find home with bin/java (direct or Contents/Home)
  local candidate
  candidate=""
  while IFS= read -r -d '' d; do
    if [[ -x "$d/bin/java" ]]; then candidate="$d"; break; fi
    if [[ -x "$d/Contents/Home/bin/java" ]]; then candidate="$d/Contents/Home"; break; fi
  done < <(find "$tmpd/extract" -type d -maxdepth 3 -print0)
  if [[ -z "$candidate" ]]; then
    echo "Could not locate Java home in downloaded archive" >&2
    rm -rf "$tmpd"
    return 1
  fi
  # Copy to user location
  rsync -a "$candidate/" "$dest_dir/" 2>/dev/null || cp -R "$candidate/"* "$dest_dir/"
  rm -rf "$tmpd"
  if [[ -x "$bin_java" ]]; then echo "$bin_java"; return 0; fi
  return 1
}

# If no embedded JRE, try user-level JRE (auto-download if missing)
if [[ -z "$JAVA_BIN" ]]; then
  case "$ARCH" in
    arm64)
      JAVA_BIN="$(ensure_user_jre arm64)" || true ;;
    x86_64)
      JAVA_BIN="$(ensure_user_jre x86_64)" || true ;;
  esac
fi

# Finally system Java
if [[ -z "$JAVA_BIN" ]]; then
  if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    JAVA_BIN="$JAVA_HOME/bin/java"
  elif /usr/libexec/java_home >/dev/null 2>&1; then
    JAVA_BIN="$((/usr/libexec/java_home))/bin/java"
  else
    JAVA_BIN="java"
  fi
fi

exec "$JAVA_BIN" -XstartOnFirstThread \
  -Dapple.laf.useScreenMenuBar=true \
  -Dapple.awt.application.name=Ramus \
  -Xdock:name=Ramus \
  -jar "$APP_RES/bin/ramus-startup.jar" --close-startup
