#!/usr/bin/env bash
set -euo pipefail
hnn_project=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
hnn_jdk="$hnn_project/../../.dev/jdk-25.0.5+8"
if [[ -n "${HNN_JAVA_HOME:-}" ]]; then
    export JAVA_HOME="$HNN_JAVA_HOME"
elif [[ -x "$hnn_jdk/bin/java" ]]; then
    export JAVA_HOME="$hnn_jdk"
fi
hnn_proxy_args=()
hnn_proxy="${HTTPS_PROXY:-${https_proxy:-}}"
if [[ "$hnn_proxy" =~ ^https?://([^/:@]+):([0-9]+)/?$ ]]; then
    hnn_proxy_args+=("-Dhttps.proxyHost=${BASH_REMATCH[1]}" "-Dhttps.proxyPort=${BASH_REMATCH[2]}")
    hnn_proxy_args+=("-Dhttp.proxyHost=${BASH_REMATCH[1]}" "-Dhttp.proxyPort=${BASH_REMATCH[2]}")
fi
cd -- "$hnn_project"
hnn_tasks=()
for hnn_arg in "$@"; do
    case "$hnn_arg" in
        runClient|runServer|build|test|clean|devHarnessJar) hnn_tasks+=(":$hnn_arg") ;;
        *) hnn_tasks+=("$hnn_arg") ;;
    esac
done
exec ./gradlew "${hnn_proxy_args[@]}" "${hnn_tasks[@]}"
