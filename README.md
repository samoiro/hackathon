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

```text
type(scope): 설명
```

| type | 사용 기준 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 오류 수정 |
| `docs` | 문서 추가·수정 |
| `style` | 동작 변경 없는 포맷·스타일 수정 |
| `refactor` | 동작을 유지하는 코드 구조 개선 |
| `test` | 테스트 추가·수정 |
| `chore` | 빌드·의존성·환경 설정·유지보수 |

- `scope`는 `server`, `frontend`, `common` 중 변경 영역을 적습니다. 저장소 전체에 걸친 변경은 생략할 수 있습니다.
- 설명은 한국어로 변경 내용을 간결하게 적고, 끝에 마침표를 붙이지 않습니다.
- 한 커밋에는 하나의 목적을 담습니다. 변경 이유와 검증 결과가 필요하면 제목 아래 빈 줄을 두고 본문에 적습니다.

예시:

```text
feat(server): 원자재 가격 조회 기능 추가
fix(frontend): 로그인 후 화면 이동 오류 수정
chore(server): DB 마이그레이션 설정 정리
docs: 로컬 실행 방법 수정
```

## 커밋할 때 자동 검사

- 브랜치 이름 검사 (커밋, 푸시)
- 프론트 파일: 커밋하는 파일만 Prettier 정리 + ESLint (`frontend/.lintstagedrc.json`)
