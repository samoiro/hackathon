# Hackathon Python AI Server (사전 준비용)

이 폴더는 기존 저장소 루트에 **`ai-server/`** 이름으로 추가하는 독립 FastAPI 실행 환경입니다.
기존 `frontend/`, `server/`, 루트 Git 훅/배포 설정은 수정하지 않습니다.

## 현재 포함 범위
- FastAPI 앱 초기화 및 `/health` 상태 확인
- Python 패키지 설정, Dockerfile, 테스트
- Python 개발 AI 지시서(문서만)
- **행사·원가 분석, AI 추천, DB/API 연동 등의 핵심 서비스 로직은 포함하지 않음**

## CMD 실행 (Windows)

```cmd
cd /d E:\project\hackathon\ai-server
py -3 -m venv .venv
.venv\Scripts\activate
python -m pip install -e ".[dev]"
python -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

확인: http://localhost:8000/health 및 http://localhost:8000/docs

테스트 (다른 CMD 창이나 서버 중지 후):

```cmd
python -m pytest -q
```

Docker (선택):

```cmd
docker build -t hackathon-ai ./ai-server
docker run --rm -p 8000:8000 hackathon-ai
```

위 Docker 명령은 **저장소 루트**에서 실행합니다. 현재 루트 `compose.yml`은 일부러 변경하지 않았습니다.

## Java 연동 계획 (본선 구현 대상)
- 권장 흐름: React → Spring Boot → Python FastAPI
- Python은 AI 분석 처리만 담당; DB와 공공데이터 수집 책임은 Java에 유지
- 실제 API 경로와 JSON 입출력은 `docs/API_CONTRACT_DRAFT.md`를 검토한 후 팀과 확정
- Python에서 Java의 DB에 직접 접근하지 않음

## 본선 규정 관련 주의
이 ZIP에는 기본 헬스체크를 제외한 핵심 기능 구현이 없습니다.
대회 운영진에게 FastAPI 템플릿 및 배포용 Dockerfile 사전 준비 허용 여부를 확인하세요.
본선 전 작성한 핵심 기능을 사후에 숨기기 위해 Git 이력을 조작하지 마세요.
