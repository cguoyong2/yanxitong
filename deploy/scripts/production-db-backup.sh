#!/usr/bin/env bash
set -euo pipefail

SSH_TARGET="${SSH_TARGET:-root@115.29.229.188}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-yanxitong-mysql}"
DATABASE="${DATABASE:-yanxitong}"
REMOTE_BACKUP_DIR="${REMOTE_BACKUP_DIR:-/opt/backups/yanxitong/mysql}"
RUN_ID="$(date +%Y%m%d%H%M%S)"
BACKUP_BASENAME="${BACKUP_BASENAME:-${DATABASE}-${RUN_ID}.sql.gz}"
RETENTION_DAYS="${RETENTION_DAYS:-}"
LOCAL_COPY_DIR="${LOCAL_COPY_DIR:-}"

REMOTE_BACKUP_PATH="${REMOTE_BACKUP_DIR%/}/${BACKUP_BASENAME}"
REMOTE_SHA_PATH="${REMOTE_BACKUP_PATH}.sha256"
REMOTE_MANIFEST_PATH="${REMOTE_BACKUP_PATH}.manifest.json"

ssh "${SSH_TARGET}" \
  "MYSQL_CONTAINER='${MYSQL_CONTAINER}' DATABASE='${DATABASE}' REMOTE_BACKUP_DIR='${REMOTE_BACKUP_DIR}' REMOTE_BACKUP_PATH='${REMOTE_BACKUP_PATH}' REMOTE_SHA_PATH='${REMOTE_SHA_PATH}' REMOTE_MANIFEST_PATH='${REMOTE_MANIFEST_PATH}' RETENTION_DAYS='${RETENTION_DAYS}' bash -s" <<'REMOTE'
set -euo pipefail

mkdir -p "${REMOTE_BACKUP_DIR}"

if ! docker ps --format '{{.Names}}' | grep -qx "${MYSQL_CONTAINER}"; then
  echo "MySQL container is not running: ${MYSQL_CONTAINER}" >&2
  exit 1
fi

tmp_path="${REMOTE_BACKUP_PATH}.tmp"
rm -f "${tmp_path}" "${REMOTE_SHA_PATH}" "${REMOTE_MANIFEST_PATH}"

docker exec \
  -e BACKUP_DATABASE="${DATABASE}" \
  "${MYSQL_CONTAINER}" \
  sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --quick --routines --triggers --events --hex-blob --set-gtid-purged=OFF "$BACKUP_DATABASE"' \
  | gzip -9 > "${tmp_path}"

mv "${tmp_path}" "${REMOTE_BACKUP_PATH}"
sha256sum "${REMOTE_BACKUP_PATH}" > "${REMOTE_SHA_PATH}"

size_bytes="$(wc -c < "${REMOTE_BACKUP_PATH}" | tr -d ' ')"
sha256_value="$(cut -d' ' -f1 "${REMOTE_SHA_PATH}")"
created_at="$(date -Iseconds)"
cat > "${REMOTE_MANIFEST_PATH}" <<JSON
{
  "createdAt": "${created_at}",
  "database": "${DATABASE}",
  "mysqlContainer": "${MYSQL_CONTAINER}",
  "backupPath": "${REMOTE_BACKUP_PATH}",
  "sha256Path": "${REMOTE_SHA_PATH}",
  "sha256": "${sha256_value}",
  "sizeBytes": ${size_bytes}
}
JSON

if [[ -n "${RETENTION_DAYS}" ]]; then
  find "${REMOTE_BACKUP_DIR}" -type f -name '*.sql.gz' -mtime "+${RETENTION_DAYS}" -print -delete
  find "${REMOTE_BACKUP_DIR}" -type f \( -name '*.sql.gz.sha256' -o -name '*.sql.gz.manifest.json' \) -mtime "+${RETENTION_DAYS}" -print -delete
fi

printf 'Backup path: %s\n' "${REMOTE_BACKUP_PATH}"
printf 'Checksum: %s\n' "${REMOTE_SHA_PATH}"
printf 'Manifest: %s\n' "${REMOTE_MANIFEST_PATH}"
REMOTE

if [[ -n "${LOCAL_COPY_DIR}" ]]; then
  mkdir -p "${LOCAL_COPY_DIR}"
  scp "${SSH_TARGET}:${REMOTE_BACKUP_PATH}" "${LOCAL_COPY_DIR}/"
  scp "${SSH_TARGET}:${REMOTE_SHA_PATH}" "${LOCAL_COPY_DIR}/"
  scp "${SSH_TARGET}:${REMOTE_MANIFEST_PATH}" "${LOCAL_COPY_DIR}/"
  echo "Local copy: ${LOCAL_COPY_DIR}/$(basename "${REMOTE_BACKUP_PATH}")"
fi
