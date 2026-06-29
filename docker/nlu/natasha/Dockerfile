FROM python:3.11-slim

WORKDIR /app

ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

COPY docker/nlu/natasha/requirements.txt /app/requirements.txt
RUN pip install -r /app/requirements.txt

COPY docker/nlu/natasha/natasha_api.py /app/natasha_api.py

EXPOSE 8011
CMD ["uvicorn", "natasha_api:app", "--host", "0.0.0.0", "--port", "8011"]
