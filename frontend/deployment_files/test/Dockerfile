FROM python:3.13.0a4-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

WORKDIR /app/frontend

ENV FLASK_APP=main.py
ENV FLASK_ENV=development
ENV FLASK_RUN_HOST=0.0.0.0
ENV BACKEND_URL=http://backend-service.testing.svc.cluster.local:8000/

CMD pip install --no-cache-dir -r requirements.txt && \
    flask run --host=0.0.0.0