# hackathon
Samoiro AI Hackathon

## 처음 클론하면

레포 루트에서 한 번 실행해 git 훅(husky)을 켭니다.

```sh
npm install
```

- `server/`: Spring Boot 백엔드 ([server/README.md](server/README.md))
- `frontend/`: React 프론트엔드 ([frontend/README.md](frontend/README.md))

## 브랜치 이름 규칙

```
{영역}/{종류}/{이니셜}/{YYMMDD}/{작업-설명}
```

| 자리 | 값 |
| --- | --- |
| 영역 | `fe` 프론트, `be` 백엔드, `common` 공통·루트 설정 |
| 종류 | `feature`, `bugfix`, `hotfix`, `chore` |
| 이니셜 | 영문 소문자 2~4자 |
| 날짜 | 브랜치를 만든 날 (예: `261109`) |
| 작업 설명 | 영문 소문자, 숫자, `-` |

예: `be/feature/jm/261109/public-data-api`, `fe/bugfix/dw/261110/login-redirect`

규칙에 맞지 않으면 커밋·푸시가 막힙니다 (`main` 제외). 이름 바꾸기: `git branch -m <새-이름>`

## 커밋 메시지 규칙

```
{종류}({범위}): {설명}
```

| 자리 | 값 |
| --- | --- |
| 종류 | `feat` 기능, `fix` 버그 수정, `refactor`, `style` 포맷, `docs`, `test`, `chore` 설정·잡무, `perf`, `build`, `ci`, `revert` |
| 범위 | 생략 가능. `server`, `frontend`, `ai-server` 등 영문 소문자, 숫자, `-` |
| 설명 | **한글**로 씀. 첫 줄 100자 이하. 호환이 깨지는 변경은 `feat(server)!: ...`처럼 `!`를 붙임 |

예: `feat(server): aT 가격 추이 API 추가`, `fix(frontend): 로그인 후 원래 페이지로 이동`, `chore: husky 훅 추가`

커밋 본문과 PR 제목·본문도 한글로 씁니다. 종류·범위, 코드·파일 이름 같은 고유명사는 영문 그대로 둡니다.

규칙에 맞지 않으면 커밋이 막힙니다. git이 만드는 `Merge ...`, `Revert ...` 메시지는 검사하지 않습니다. 메시지 고치기: `git commit --amend`

## 커밋할 때 자동 검사

- 브랜치 이름 검사 (커밋, 푸시)
- 커밋 메시지 검사 (`.husky/check-commit-msg.sh`)
- 프론트 파일: 커밋하는 파일만 Prettier 정리 + ESLint (`frontend/.lintstagedrc.json`)
