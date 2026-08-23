#!/usr/bin/env bash

set -Eeuo pipefail

: "${PR_ACTION:?PR_ACTION is required}"
: "${PR_DATA_PATH:?PR_DATA_PATH is required}"

case "${PR_ACTION}" in
  opened | reopened | closed)
    ;;
  *)
    echo "Unsupported pull request action: ${PR_ACTION}" >&2
    exit 1
    ;;
esac

payload_file="$(mktemp)"
trap 'rm -f "${payload_file}"' EXIT

jq \
  --arg action "${PR_ACTION}" \
  '
    def truncate(max):
      if length > max then .[0:(max - 3)] + "..." else . end;
    def preview:
      gsub("\\r"; "")
      | gsub("[[:space:]]+"; " ")
      | if length == 0 then "본문 없음" else truncate(500) end;

    (
      if $action == "opened" then {text: "열림", color: 5763719}
      elif $action == "reopened" then {text: "다시 열림", color: 5763719}
      elif .merged then {text: "병합됨", color: 10181046}
      else {text: "종료됨", color: 15548997}
      end
    ) as $status
    |
    {
      username: "ChongChong GitHub",
      embeds: [
        {
          title: (.title | truncate(256)),
          url: .html_url,
          description: ((.body // "") | preview),
          color: $status.color,
          fields: [
            {name: "상태", value: $status.text, inline: true},
            {name: "작성자", value: ("@" + .user.login), inline: true},
            {
              name: "브랜치",
              value: (.head.label + " → " + .base.label | truncate(1024)),
              inline: false
            },
            {
              name: "라벨",
              value: (
                [.labels[].name]
                | if length == 0 then "없음" else join(", ") | truncate(1024) end
              ),
              inline: false
            }
          ],
          timestamp: .updated_at,
          footer: {text: ("GitHub Pull Request #" + (.number | tostring))}
        }
      ]
    }
  ' "${PR_DATA_PATH}" > "${payload_file}"

if [[ "${DISCORD_WEBHOOK_DRY_RUN:-false}" == "true" ]]; then
  cat "${payload_file}"
  exit 0
fi

: "${DISCORD_PR_WEBHOOK_URL:?DISCORD_PR_WEBHOOK_URL is required}"

curl \
  --silent \
  --show-error \
  --fail-with-body \
  --request POST \
  --header "Content-Type: application/json" \
  --data-binary "@${payload_file}" \
  "${DISCORD_PR_WEBHOOK_URL}" \
  > /dev/null
