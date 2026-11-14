#!/bin/sh
# 커밋 메시지 규칙: {종류}({범위}): {설명}   범위는 생략 가능
#   예) feat(server): add aT price trend API
msg_file="$1"
subject=$(grep -v '^#' "$msg_file" | sed -n '/[^[:space:]]/{p;q;}')

# git이 자동으로 만드는 merge·revert 메시지와 fixup·squash는 검사하지 않음
case "$subject" in
  "Merge "*|"Revert "*|"fixup! "*|"squash! "*|"amend! "*) exit 0 ;;
esac

pattern='^(feat|fix|refactor|style|docs|test|chore|perf|build|ci|revert)(\([a-z0-9-]+\))?!?: [^ ].*$'
if echo "$subject" | grep -Eq "$pattern" && [ ${#subject} -le 100 ]; then
  exit 0
fi

cat >&2 <<MSG
커밋 메시지가 규칙에 맞지 않습니다: $subject

  {종류}({범위}): {설명}     범위는 생략 가능, 첫 줄 100자 이하
  종류: feat | fix | refactor | style | docs | test | chore | perf | build | ci | revert
  범위: server | frontend | ai-server 등 영문 소문자, 숫자, -
  예)   feat(server): add aT price trend API
        chore: add husky commit-msg hook

메시지 고치기: git commit --amend
MSG
exit 1
