"""APK의 권한이 허용 목록 안에 있는지 검사한다. 목록에 없는 권한이 하나라도 있으면 실패한다.

사용법: check_permissions.py <aapt2 경로> <APK 경로> <허용 목록 파일> <패키지 이름>
"""
import re
import subprocess
import sys

aapt2, apk, allowed_path, package = sys.argv[1:5]
badging = subprocess.run(
    [aapt2, "dump", "badging", apk], capture_output=True, text=True, check=True
).stdout

package_line = next((line for line in badging.splitlines() if line.startswith("package:")), "")
print(package_line)
if f"name='{package}'" not in package_line:
    sys.exit(f"aapt2 출력에서 패키지 {package}를 찾지 못했다. 검사 결과를 믿을 수 없어서 실패시킨다.")

used = sorted(set(re.findall(r"^uses-permission(?:-sdk-23)?: name='([^']+)'", badging, re.M)))
with open(allowed_path, encoding="utf-8") as f:
    allowed = {line.strip() for line in f if line.strip() and not line.startswith("#")}

print("APK 권한:", ", ".join(used) if used else "없음")
extra = [p for p in used if p not in allowed]
if extra:
    sys.exit("허용 목록에 없는 권한: " + ", ".join(extra))
