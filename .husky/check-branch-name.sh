#!/bin/sh
# 브랜치 이름 규칙: {영역}/{종류}/{이니셜}/{YYMMDD}/{작업-설명}
#   예) be/feature/jm/261109/public-data-api
branch=$(git rev-parse --abbrev-ref HEAD)

# main, 그리고 rebase 등으로 브랜치가 없는 상태(HEAD)는 검사하지 않음
case "$branch" in
  main|HEAD) exit 0 ;;
esac

pattern='^(fe|be|common)/(feature|bugfix|hotfix|chore)/[a-z]{2,4}/[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])/[a-z0-9]+(-[a-z0-9]+)*$'
if echo "$branch" | grep -Eq "$pattern"; then
  exit 0
fi

cat >&2 <<MSG
브랜치 이름이 규칙에 맞지 않습니다: $branch

  {영역}/{종류}/{이니셜}/{YYMMDD}/{작업-설명}
  영역: fe | be | common
  종류: feature | bugfix | hotfix | chore
  예)   be/feature/jm/$(date +%y%m%d)/public-data-api

이름 바꾸기: git branch -m <새-이름>
MSG
exit 1
