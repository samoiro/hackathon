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

## 커밋할 때 자동 검사

- 브랜치 이름 검사 (커밋, 푸시)
- 프론트 파일: 커밋하는 파일만 Prettier 정리 + ESLint (`frontend/.lintstagedrc.json`)
