FROM python:3.13.0a4-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

WORKDIR /app

COPY frontend/requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

COPY frontend .

ENV BACKEND_URL=http://backend-service.prod.svc.cluster.local:8000/

CMD ["gunicorn", "-w", "2", "-b", "0.0.0.0:5000", "app.__init__:create_app()"]
