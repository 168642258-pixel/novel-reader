#!/usr/bin/env bash
# 一键把轻阅项目推送到 GitHub，触发 Actions 构建 APK
# 用法：在 Git Bash 里 cd 到项目根目录，然后  bash push-to-github.sh
set -e

cd "$(dirname "$0")"

echo "========================================"
echo "  轻阅 · 推送到 GitHub 触发云端构建"
echo "========================================"
echo

# 1. 检查 git
if ! command -v git >/dev/null 2>&1; then
  echo "[错误] 未检测到 git，请先安装 Git for Windows: https://git-scm.com/download/win"
  exit 1
fi

# 2. 初始化仓库
if [ ! -d .git ]; then
  echo "[1/5] 初始化 git 仓库..."
  git init -b main
else
  echo "[1/5] 已是 git 仓库，跳过 init"
fi

# 3. 配置提交者（若未设置）
if ! git config user.name >/dev/null 2>&1 || [ -z "$(git config user.name)" ]; then
  git config user.name "novel-reader"
  echo "   已设置本地 user.name = novel-reader"
fi
if ! git config user.email >/dev/null 2>&1 || [ -z "$(git config user.email)" ]; then
  git config user.email "reader@local"
  echo "   已设置本地 user.email = reader@local"
fi

# 4. 提交
echo "[2/5] 添加文件并提交..."
git add .
git commit -m "轻阅小说阅读器初始版本" || echo "   无变更需要提交"

# 5. 询问远程仓库地址
echo
echo "[3/5] 需要你的 GitHub 仓库地址"
echo "   示例: https://github.com/你的用户名/你的仓库名.git"
echo "   或:  git@github.com:你的用户名/你的仓库名.git"
echo
read -r -p "请粘贴仓库地址（直接回车跳过，稍后手动 push）: " REMOTE

if [ -n "$REMOTE" ]; then
  echo "[4/5] 配置远程仓库..."
  git remote remove origin 2>/dev/null || true
  git remote add origin "$REMOTE"

  echo "[5/5] 推送到 GitHub..."
  git push -u origin main || {
    echo
    echo "[提示] 推送失败常见原因："
    echo "  - 未配置 GitHub 凭证：首次推送时 Git 会弹窗要求登录 GitHub"
    echo "  - 仓库非空有冲突：可改用  git push -u origin main --force"
    echo "  - 用 SSH 地址但未配 SSH key：改用 https 地址"
    exit 1
  }
  echo
  echo "========================================"
  echo "  推送成功！"
  echo "========================================"
  echo "  打开你的 GitHub 仓库 → Actions 标签页"
  echo "  等待 Build APK 工作流跑完（约 5-8 分钟）"
  echo "  完成后点击对应运行 → 页面底部 Artifacts 下载 novel-reader-debug-apk.zip"
else
  echo "[4/5] 跳过远程配置"
  echo "[5/5] 你稍后手动执行："
  echo "   git remote add origin <你的仓库地址>"
  echo "   git push -u origin main"
fi
