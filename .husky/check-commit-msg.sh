#!/bin/sh
# 커밋 메시지 규칙: {종류}({범위}): {설명}   범위는 생략 가능, 설명은 한글
#   예) feat(server): aT 가격 추이 API 추가
msg_file="$1"
subject=$(grep -v '^#' "$msg_file" | sed -n '/[^[:space:]]/{p;q;}')

# git이 자동으로 만드는 merge·revert 메시지와 fixup·squash는 검사하지 않음
case "$subject" in
  "Merge "*|"Revert "*|"fixup! "*|"squash! "*|"amend! "*) exit 0 ;;
esac

pattern='^(feat|fix|refactor|style|docs|test|chore|perf|build|ci|revert)(\([a-z0-9-]+\))?!?: [^ ].*$'
# 설명에 한글이 있는지: 한글 음절(UTF-8)의 첫 바이트 0xEA~0xED로 확인 (로케일과 무관하게 동작)
hangul=$(printf '[\352-\355]')
# 글자 수: UTF-8 이어지는 바이트(0x80~0xBF)를 빼고 센다 (${#subject}는 로케일에 따라 바이트로 세서 한글이 3배로 잡힘)
length=$(printf '%s' "$subject" | LC_ALL=C tr -d '\200-\277' | wc -c)
if echo "$subject" | grep -Eq "$pattern" && [ "$length" -le 100 ] \
  && echo "${subject#*: }" | LC_ALL=C grep -q "$hangul"; then
  exit 0
fi

cat >&2 <<MSG
커밋 메시지가 규칙에 맞지 않습니다: $subject

  {종류}({범위}): {설명}     범위는 생략 가능, 설명은 한글, 첫 줄 100자 이하
  종류: feat | fix | refactor | style | docs | test | chore | perf | build | ci | revert
  범위: server | frontend | ai-server 등 영문 소문자, 숫자, -
  예)   feat(server): aT 가격 추이 API 추가
        chore: husky 커밋 메시지 훅 추가

메시지 고치기: git commit --amend
MSG
exit 1
