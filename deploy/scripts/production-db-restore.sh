#!/usr/bin/env bash
set -euo pipefail

SSH_TARGET="${SSH_TARGET:-root@115.29.229.188}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-yanxitong-mysql}"
BACKUP_FILE="${BACKUP_FILE:-}"
RESTORE_DATABASE="${RESTORE_DATABASE:-yanxitong_restore_$(date +%Y%m%d%H%M%S)}"
CONFIRM_RESTORE="${CONFIRM_RESTORE:-}"
DROP_TARGET_FIRST="${DROP_TARGET_FIRST:-0}"

if [[ -z "${BACKUP_FILE}" ]]; then
  echo "BACKUP_FILE is required. It must be a remote path on ${SSH_TARGET}." >&2
  exit 1
fi

if [[ "${RESTORE_DATABASE}" == "yanxitong" && "${CONFIRM_RESTORE}" != "RESTORE_PRODUCTION_YANXITONG" ]]; then
  echo "Refusing to restore into production database 'yanxitong' without CONFIRM_RESTORE=RESTORE_PRODUCTION_YANXITONG." >&2
  exit 1
fi

ssh "${SSH_TARGET}" \
  "MYSQL_CONTAINER='${MYSQL_CONTAINER}' BACKUP_FILE='${BACKUP_FILE}' RESTORE_DATABASE='${RESTORE_DATABASE}' DROP_TARGET_FIRST='${DROP_TARGET_FIRST}' bash -s" <<'REMOTE'
set -euo pipefail

if [[ ! -f "${BACKUP_FILE}" ]]; then
  echo "Backup file not found: ${BACKUP_FILE}" >&2
  exit 1
fi

if [[ -f "${BACKUP_FILE}.sha256" ]]; then
  sha256sum -c "${BACKUP_FILE}.sha256"
fi

if ! docker ps --format '{{.Names}}' | grep -qx "${MYSQL_CONTAINER}"; then
  echo "MySQL container is not running: ${MYSQL_CONTAINER}" >&2
  exit 1
fi

if [[ "${DROP_TARGET_FIRST}" == "1" ]]; then
  docker exec -e RESTORE_DATABASE="${RESTORE_DATABASE}" "${MYSQL_CONTAINER}" \
    sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "DROP DATABASE IF EXISTS \`$RESTORE_DATABASE\`; CREATE DATABASE \`$RESTORE_DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"'
else
  docker exec -e RESTORE_DATABASE="${RESTORE_DATABASE}" "${MYSQL_CONTAINER}" \
    sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "CREATE DATABASE IF NOT EXISTS \`$RESTORE_DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"'
fi

gzip -dc "${BACKUP_FILE}" | docker exec -i -e RESTORE_DATABASE="${RESTORE_DATABASE}" "${MYSQL_CONTAINER}" \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$RESTORE_DATABASE"'

docker exec -e RESTORE_DATABASE="${RESTORE_DATABASE}" "${MYSQL_CONTAINER}" \
  sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "SELECT TABLE_SCHEMA, COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = \"$RESTORE_DATABASE\" GROUP BY TABLE_SCHEMA;"'

printf 'Restore target database: %s\n' "${RESTORE_DATABASE}"
REMOTE
