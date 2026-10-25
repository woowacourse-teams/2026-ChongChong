#!/usr/bin/env bash
# 인증서 갱신 timer의 실행 파일. 실제 인증서가 갱신된 경우에만 Nginx를 reload한다.

set -Eeuo pipefail

certbot renew \
    --quiet \
    --webroot \
    --webroot-path /var/www/certbot \
    --deploy-hook "docker exec chongchong-nginx nginx -s reload"
