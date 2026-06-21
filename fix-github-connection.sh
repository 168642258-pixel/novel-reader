#!/usr/bin/env bash
# 修复 GitHub 连接被重置 / Connection was reset 问题
# 用法：在项目目录右键 Git Bash，执行  bash fix-github-connection.sh
cd "$(dirname "$0")"

echo "========================================"
echo "  GitHub 连接修复工具"
echo "========================================"
echo

# ---------- 0. 先测试当前连通性 ----------
echo "[测试] 当前 GitHub 连通性..."
if curl -sI --connect-timeout 8 https://github.com >/dev/null 2>&1; then
  echo "  ✓ 当前能访问 github.com，问题可能是临时波动"
  echo "  建议直接重试：bash push-to-github.sh"
  echo
  read -r -p "要直接重试推送吗？(y/n): " r
  if [ "$r" = "y" ]; then bash push-to-github.sh; fi
  exit 0
else
  echo "  ✗ 当前无法访问 github.com（连接被重置）"
fi
echo

# ---------- 1. 检测本机代理 ----------
echo "[1/4] 检测本机代理..."
PROXY_PORT=""
for port in 7890 7891 1080 10808 10809 1087 8080 2080; do
  if curl -s --connect-timeout 2 -x "http://127.0.0.1:$port" https://www.google.com >/dev/null 2>&1; then
    PROXY_PORT=$port
    echo "  ✓ 检测到本机代理：127.0.0.1:$port"
    break
  fi
done

if [ -n "$PROXY_PORT" ]; then
  echo "  为 Git 配置代理..."
  git config --global http.proxy "http://127.0.0.1:$PROXY_PORT"
  git config --global https.proxy "http://127.0.0.1:$PROXY_PORT"
  echo "  ✓ 已配置 git 走代理 http://127.0.0.1:$PROXY_PORT"
  echo
  echo "[验证] 通过代理测试 GitHub..."
  if curl -sI --connect-timeout 8 -x "http://127.0.0.1:$PROXY_PORT" https://github.com >/dev/null 2>&1; then
    echo "  ✓ 代理可用，现在可以推送了"
    echo
    read -r -p "立即用代理重试推送吗？(y/n): " r
    if [ "$r" = "y" ]; then bash push-to-github.sh; fi
    exit 0
  else
    echo "  ✗ 代理也连不上 GitHub，代理可能未生效"
  fi
else
  echo "  ✗ 未检测到本机代理（Clash/V2Ray/SS 等）"
  git config --global --unset http.proxy 2>/dev/null
  git config --global --unset https.proxy 2>/dev/null
fi
echo

# ---------- 2. 尝试修改 hosts ----------
echo "[2/4] 尝试通过修改 hosts 直连 GitHub..."
echo "  查询 GitHub 最新 IP..."
GITHUB_IP=$(curl -s --connect-timeout 5 https://api.github.com/meta 2>/dev/null | grep -oE '"[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+"' | head -1 | tr -d '"')
if [ -z "$GITHUB_IP" ]; then
  # 用公共 DNS 查询
  GITHUB_IP=$(nslookup github.com 8.8.8.8 2>/dev/null | grep -A1 "Name:" | grep "Address" | awk '{print $2}' | head -1)
fi
if [ -n "$GITHUB_IP" ]; then
  echo "  获取到 GitHub IP: $GITHUB_IP"
  echo
  echo "  需要管理员权限修改 hosts 文件。请按下面手动操作："
  echo
  echo "  1) 按 Win 键搜索 '记事本'，右键 '以管理员身份运行'"
  echo "  2) 文件 → 打开 → 输入路径: C:\\Windows\\System32\\drivers\\etc\\hosts"
  echo "     （右下角文件类型改成 '所有文件' 才能看到 hosts）"
  echo "  3) 在文件最末尾追加这两行："
  echo "       $GITHUB_IP github.com"
  echo "       $GITHUB_IP raw.githubusercontent.com"
  echo "  4) 保存，然后回到这里回车继续"
  echo
  read -r -p "已修改 hosts，按回车继续测试..."
  if curl -sI --connect-timeout 8 https://github.com >/dev/null 2>&1; then
    echo "  ✓ hosts 方案生效！现在可以推送了"
    read -r -p "立即重试推送吗？(y/n): " r
    if [ "$r" = "y" ]; then bash push-to-github.sh; fi
    exit 0
  else
    echo "  ✗ hosts 修改后仍连不上"
  fi
else
  echo "  ✗ 无法获取 GitHub IP（DNS 也被墙了）"
fi
echo

# ---------- 3. 建议 ----------
echo "[3/4] 推荐方案：使用 GitHub 镜像加速"
echo
echo "  国内常用了 GitHub 加速镜像，把仓库地址换成下面任一镜像前缀："
echo
echo "  方案A - ghproxy 加速（最稳定）："
echo "    1. 浏览器打开 https://ghproxy.com 看是否可用"
echo "    2. 若可用，clone/push 都走 https://ghproxy.com/https://github.com/..."
echo
echo "  方案B - 使用国内代码托管（最彻底）："
echo "    1. Gitee（码云，国内版 GitHub）：注册 https://gitee.com"
echo "    2. 在 Gitee 新建仓库"
echo "    3. 仓库地址换成 https://gitee.com/你的用户名/novel-reader.git"
echo "    4. 重新跑  bash push-to-github.sh  粘贴 Gitee 地址"
echo "    5. 但 Gitee 没有免费 Actions，本方案不适用"
echo
echo "[4/4] 终极方案：开代理 / 换网络"
echo
echo "  ✓ 最可靠的办法：开启任意科学上网工具（Clash/V2Ray/SS/Wireguard）"
echo "    开启后重新跑  bash fix-github-connection.sh"
echo "    脚本会自动检测代理并配置 git"
echo
echo "  ✓ 或者：换手机热点试试（有时宽带运营商不同结果不同）"
echo "    手机开热点，电脑连热点，重新跑  bash push-to-github.sh"
echo
echo "  ✓ 或者：换用 SSH 协议（如果已配 SSH key）"
echo "    仓库地址改成  git@github.com:168642258-pixel/novel-reader.git"
echo
echo "========================================"
echo "  建议：先开代理工具，再跑本脚本"
echo "========================================"
