#!/usr/bin/env bash
set -o errexit -o nounset -o pipefail

# Usage:
#   ./gen-cert.sh localhost
# Input:
#   * ca.key
#   * ca.crt
# Output:
#   * cert.key
#   * cert.crt
# Generates cert.key and cert.crt files for a specified domain name

HOSTNAME=$1
#FILENAME=${2:-$CN}

echo Generating SSL certificate for "$HOSTNAME"

openssl genrsa -out cert.key 4096
# Use piping to avoid *.csr file creation
openssl req -new -key cert.key -subj "/CN=$HOSTNAME" \
    | openssl x509 -req -sha256 -days 36526 -CA ca.crt -CAkey ca.key -set_serial 01 -out cert.crt -extensions v3_req
