# Base image with Python
FROM python:3.11-slim AS builder

WORKDIR /app

# Install system dependencies for psycopg2 and other tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Install python dependencies
COPY requirements.txt .
RUN pip install --user --no-cache-dir -r requirements.txt

# Final stage
FROM python:3.11-slim

WORKDIR /app

# Install runtime dependencies for Postgres and curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq5 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy installed packages from builder
COPY --from=builder /root/.local /root/.local
ENV PATH=/root/.local/bin:$PATH

# Copy ONLY necessary scripts
COPY main.py .
COPY ai_service.py .
COPY rss_service.py .
COPY database.py .
COPY models.py .
COPY config.py .
COPY utils.py .
COPY scheduler_task.py .
COPY cache_service.py .
COPY openapi.yml .

# Port for FastAPI
EXPOSE 8085

# Healthcheck
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8085/ || exit 1

# Environment variable to indicate production/docker
ENV IS_DOCKER=true

# Command to run the application
CMD ["python", "main.py"]
