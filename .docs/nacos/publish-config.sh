#!/usr/bin/env bash
# 把 .docs/nacos/*.yaml 发布到 Nacos 配置中心（dataId = 文件名，group = DEFAULT_GROUP）。
# 用法：bash .docs/nacos/publish-config.sh
# 幂等：会先删除再发布，确保 Nacos 里的内容与本地权威副本一致。
set -euo pipefail

NACOS_URL="${NACOS_URL:-http://127.0.0.1:8848}"
GROUP="${GROUP:-DEFAULT_GROUP}"
# 脚本所在目录 = .docs/nacos/
DOCS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Nacos: ${NACOS_URL}   group: ${GROUP}"
echo "配置目录: ${DOCS_DIR}"
echo "--------------------------------------------"

publish_one() {
  local file="$1"
  local dataId
  dataId="$(basename "$file")"

  # 读文件内容为变量（Windows curl 读不了 MSYS 路径的 @file）
  local content
  content="$(cat "$file")"

  # 先删除（忽略不存在/失败），保证以本地文件内容为准
  curl -s -o /dev/null -X DELETE "${NACOS_URL}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=${dataId}" \
    --data-urlencode "group=${GROUP}"

  local resp
  resp="$(curl -s -X POST "${NACOS_URL}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=${dataId}" \
    --data-urlencode "group=${GROUP}" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content=${content}")"

  if [[ "${resp}" == "true" ]]; then
    echo "  ✓ ${dataId}"
  else
    echo "  ✗ ${dataId} 发布失败: ${resp}" >&2
    return 1
  fi
}

for f in "${DOCS_DIR}"/*.yaml; do
  [[ -e "$f" ]] || continue
  publish_one "$f"
done

echo "--------------------------------------------"
echo "全部发布完成。"
