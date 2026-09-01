#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
readonly VALIDATOR="${PROJECT_ROOT}/deploy/scripts/validate.sh"

TEST_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/chongchong-deploy-test.XXXXXX")"
readonly TEST_ROOT
trap 'rm -rf "${TEST_ROOT}"' EXIT

readonly APP_ROOT="${TEST_ROOT}/app"
readonly MOCK_BIN="${TEST_ROOT}/bin"
readonly DOCKER_CALL_LOG="${TEST_ROOT}/docker-calls.log"

mkdir -p "${APP_ROOT}/deploy" "${MOCK_BIN}"

cat >"${APP_ROOT}/.env" <<'EOF'
SERVER_NAME=chongchong.example.com
DOCKERHUB_USERNAME=test-user
DOCKERHUB_TOKEN=test-token
DB_URL=jdbc:postgresql://localhost:5432/test
DB_USERNAME=test-user
DB_PASSWORD=test-password
STUDY_INVITE_JWT_SECRET=test-study-secret
FRONTEND_BASE_URL=https://chongchong.example.com
AUTH_JWT_ISSUER=test-issuer
AUTH_JWT_AUDIENCE=test-audience
AUTH_JWT_SECRET_BASE64=dGVzdC1zZWNyZXQ=
AUTH_KAKAO_REST_API_KEY=test-rest-api-key
AUTH_KAKAO_CLIENT_SECRET=test-client-secret
AUTH_KAKAO_REDIRECT_URI=https://chongchong.example.com/auth/kakao/callback
EOF

cat >"${APP_ROOT}/deploy/image.env" <<'EOF'
BACKEND_IMAGE=docker.io/example/chongchong-backend
BACKEND_IMAGE_TAG=test-tag
EOF

cat >"${MOCK_BIN}/docker" <<'EOF'
#!/usr/bin/env bash

set -Eeuo pipefail

case "${1:-}" in
    inspect)
        printf 'true\n'
        ;;
    exec)
        exit 0
        ;;
    image)
        printf '%s\n' "$*" >>"${DOCKER_CALLS_FILE}"
        ;;
    *)
        echo "Unexpected docker command: $*" >&2
        exit 1
        ;;
esac
EOF

cat >"${MOCK_BIN}/curl" <<'EOF'
#!/usr/bin/env bash

set -Eeuo pipefail

if [[ "${MOCK_CURL_EXIT_CODE}" -ne 0 ]]; then
    exit "${MOCK_CURL_EXIT_CODE}"
fi

printf '%s' "${MOCK_HEALTH_STATUS}"
EOF

chmod +x "${MOCK_BIN}/docker" "${MOCK_BIN}/curl"

run_validate() {
    local health_status="$1"
    local curl_exit_code="$2"

    CHONGCHONG_APP_ROOT="${APP_ROOT}" \
    DOCKER_CALLS_FILE="${DOCKER_CALL_LOG}" \
    MOCK_HEALTH_STATUS="${health_status}" \
    MOCK_CURL_EXIT_CODE="${curl_exit_code}" \
    PATH="${MOCK_BIN}:${PATH}" \
        bash "${VALIDATOR}"
}

expect_cleanup_after_successful_health_check() {
    : >"${DOCKER_CALL_LOG}"

    run_validate 200 0

    local actual
    actual="$(cat "${DOCKER_CALL_LOG}")"
    if [[ "${actual}" != "image prune --all --force" ]]; then
        echo "Expected one image cleanup after a successful health check, got: ${actual:-<none>}" >&2
        exit 1
    fi
}

expect_no_cleanup_after_http_failure() {
    : >"${DOCKER_CALL_LOG}"

    if run_validate 500 0 >/dev/null 2>&1; then
        echo "Expected validation to fail for HTTP 500" >&2
        exit 1
    fi

    if [[ -s "${DOCKER_CALL_LOG}" ]]; then
        echo "Image cleanup ran after a failed HTTP health check" >&2
        exit 1
    fi
}

expect_no_cleanup_after_curl_failure() {
    : >"${DOCKER_CALL_LOG}"

    if run_validate 000 7 >/dev/null 2>&1; then
        echo "Expected validation to fail when curl fails" >&2
        exit 1
    fi

    if [[ -s "${DOCKER_CALL_LOG}" ]]; then
        echo "Image cleanup ran after curl failed" >&2
        exit 1
    fi
}

expect_cleanup_after_successful_health_check
expect_no_cleanup_after_http_failure
expect_no_cleanup_after_curl_failure

echo "배포 검증 스크립트 테스트를 통과했습니다."
