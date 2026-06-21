#!/usr/bin/env bash
# 提交 workflow 改动（错误摘要增强）并重新推送触发构建
cd "$(dirname "$0")"

echo "========================================"
echo "  提交并重新推送（错误日志增强版）"
echo "========================================"
echo

# 确保代理配置还在
if ! git config --global --get http.proxy >/dev/null 2>&1; then
  git config --global http.proxy http://127.0.0.1:7897
  git config --global https.proxy http://127.0.0.1:7897
fi

git add .github/workflows/build.yml
git commit -m "ci: 构建失败时输出错误摘要到 Summary" || echo "（无变更需要提交）"

echo "推送中..."
git push origin main

if [ $? -eq 0 ]; then
  echo
  echo "========================================"
  echo "  推送成功！新构建已触发"
  echo "========================================"
  echo
  echo "  1. 打开 https://github.com/168642258-pixel/novel-reader/actions"
  echo "  2. 等新的 Build APK 跑完（约 5-8 分钟）"
  echo "  3. 点进这次运行 → 点 Summary 标签"
  echo "     错误会直接显示在页面顶部，不用翻日志"
  echo "  4. 截图 Summary 页面发给我"
else
  echo
  echo "[推送失败] 把报错发给我"
fi
