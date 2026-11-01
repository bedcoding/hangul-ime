#!/usr/bin/env bash
# 가상 폰(에뮬레이터)에 release APK를 깔고, 폰에서 하던 확인 중 사람 손이 필요 없는 것을 대신한다.
# 1. 설치된다.
# 2. 키보드 목록에 있고, 켜서 기본 키보드로 고를 수 있다.
# 3. 설정 화면에 이번 빌드 번호가 뜬다.
# 4. 시험 칸을 누르면 키보드가 뜬다.
# 5. 그동안 앱이 죽지 않았다.
# 화면 캡처와 화면 구조는 결과 폴더에 남긴다. CI가 아티팩트로 올리니 실행 결과에서 내려받아 볼 수 있다.
#
# 사용법: emulator_check.sh <APK 경로> <결과 폴더>
# 기대하는 빌드 번호는 android/build.gradle.kts와 같은 규칙으로 GITHUB_RUN_NUMBER와 GITHUB_SHA에서 만든다.
set -euo pipefail

apk="$1"
out="$2"
package=io.github.bedcoding.hangulime
ime="$package/.HangulImeService"
build="빌드 $GITHUB_RUN_NUMBER (${GITHUB_SHA::7})"
mkdir -p "$out"

fail() {
  echo "실패: $1" >&2
  exit 1
}

# 화면 구조를 XML로 받는다. 화면이 바뀌는 중이면 비어 있을 때가 있어서 몇 번 다시 해 본다.
dump_ui() {
  for _ in 1 2 3 4 5; do
    adb shell rm -f /data/local/tmp/ui.xml
    adb shell uiautomator dump /data/local/tmp/ui.xml > /dev/null 2>&1 || true
    if adb shell test -s /data/local/tmp/ui.xml; then
      adb exec-out cat /data/local/tmp/ui.xml > "$1"
      return 0
    fi
    sleep 2
  done
  fail "화면 구조를 받지 못했다"
}

# 화면 구조에 글자가 정확히 같은 칸이 있는지 본다.
has_text() {
  python3 - "$1" "$2" << 'PY'
import sys
import xml.etree.ElementTree as ET

nodes = ET.parse(sys.argv[1]).iter("node")
sys.exit(0 if any(node.get("text") == sys.argv[2] for node in nodes) else 1)
PY
}

# 화면 구조에서 처음 나오는 입력 칸의 가운데 좌표를 "x y"로 출력한다.
edit_text_center() {
  python3 - "$1" << 'PY'
import re
import sys
import xml.etree.ElementTree as ET

for node in ET.parse(sys.argv[1]).iter("node"):
    if node.get("class") == "android.widget.EditText":
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        sys.exit(0)
sys.exit(1)
PY
}

screenshot() {
  adb exec-out screencap -p > "$out/$1.png"
}

echo "== 설치"
adb install "$apk"

echo "== 키보드 목록에 있고, 켜서 고를 수 있는지"
# 설치 직후에는 시스템이 키보드 목록을 아직 다시 읽지 않았을 수 있어서 잠깐 기다린다.
listed=false
for _ in $(seq 20); do
  imes="$(adb shell ime list -a -s | tr -d '\r' | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
  if grep -qxF "$ime" <<< "$imes"; then
    listed=true
    break
  fi
  sleep 1
done
if [ "$listed" != true ]; then
  printf '키보드 목록:\n%s\n' "$imes"
  fail "키보드 목록에 $ime 이(가) 없다"
fi
adb shell ime enable "$ime"
adb shell ime set "$ime"
current="$(adb shell settings get secure default_input_method | tr -d '\r')"
[ "$current" = "$ime" ] || fail "기본 키보드가 $current 이다"

echo "== 설정 화면에 '$build'가 뜨는지"
adb shell am start -W -n "$package/.SettingsActivity"
sleep 2
dump_ui "$out/settings.xml"
screenshot 1-settings
has_text "$out/settings.xml" "$build" || fail "설정 화면에 '$build'가 없다"

echo "== 시험 칸을 누르면 키보드가 뜨는지"
center="$(edit_text_center "$out/settings.xml")" || fail "설정 화면에 시험 칸이 없다"
read -r x y <<< "$center"
adb shell input tap "$x" "$y"
shown=false
for _ in $(seq 10); do
  adb shell dumpsys input_method > "$out/input_method.txt"
  # 앞의 것은 시스템 쪽 기록, 뒤의 것은 키보드 서비스 쪽 기록이다. 안드로이드 버전에 따라 한쪽만 나올 수 있다.
  if grep -qE 'mInputShown=true|mDecorViewVisible=true' "$out/input_method.txt"; then
    shown=true
    break
  fi
  sleep 1
done
screenshot 2-keyboard
[ "$shown" = true ] || fail "시험 칸을 눌러도 키보드가 뜨지 않았다"

echo "== 앱이 죽지 않았는지"
crash="$(adb logcat -d -b crash)"
if grep -qF "Process: $package" <<< "$crash"; then
  printf '%s\n' "$crash"
  fail "앱이 죽었다"
fi

echo "모두 통과"
