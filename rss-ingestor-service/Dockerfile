# Base image with Python
FROM python:3.11-slim AS builder

WORKDIR /app

# Install system dependencies for psycopg2 and other tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Install python dependencies to a custom location
COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

# Final stage
FROM python:3.11-slim

# Create a non-root user
RUN groupadd -r python && useradd -r -g python -s /bin/false python

WORKDIR /app

# Install runtime dependencies and apply all security patches
RUN apt-get update && apt-get upgrade -y --no-install-recommends \
    && apt-get install -y --no-install-recommends \
    libpq5 \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy installed packages from builder
COPY --from=builder /install /usr/local

# Patch build-tool vulnerabilities
RUN pip install --no-cache-dir --upgrade "wheel>=0.46.2" setuptools

# Copy application source
COPY . .

# Change ownership
RUN chown -R python:python /app

# Port for FastAPI
EXPOSE 8085

# Healthcheck
HEALTHCHECK --interval=30s --timeout=30s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8085/health || exit 1

# Environment variable to indicate production/docker
ENV IS_DOCKER=true

USER python

# Command to run the application
CMD ["python", "main.py"]
