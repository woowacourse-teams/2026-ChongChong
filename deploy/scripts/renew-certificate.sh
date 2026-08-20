#!/usr/bin/env bash

set -Eeuo pipefail

certbot renew \
    --quiet \
    --webroot \
    --webroot-path /var/www/certbot \
    --deploy-hook "docker exec chongchong-nginx nginx -s reload"
