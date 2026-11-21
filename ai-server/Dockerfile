FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app
COPY pyproject.toml /app/
COPY app /app/app
RUN pip install --no-cache-dir .

EXPOSE 8000
# Render는 PORT(기본 10000)로 헬스체크한다. 로컬 docker run은 8000
CMD ["sh", "-c", "exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
