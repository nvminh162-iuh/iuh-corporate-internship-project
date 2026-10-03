# ==============================================================================
# Multi-stage Dockerfile for TeleCare Admin Portal (Vite + React)
# ==============================================================================

# Stage 1: Build static assets
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies based on package-lock
COPY package*.json ./
RUN npm ci

# Copy application source
COPY . .

# Build-time environment variables for Vite bundle
ARG VITE_KEYCLOAK_BASE_URL
ARG VITE_KEYCLOAK_REALM
ARG VITE_KEYCLOAK_CLIENT_ID
ARG VITE_GATEWAY_BASE_URL

ENV VITE_KEYCLOAK_BASE_URL=${VITE_KEYCLOAK_BASE_URL} \
    VITE_KEYCLOAK_REALM=${VITE_KEYCLOAK_REALM} \
    VITE_KEYCLOAK_CLIENT_ID=${VITE_KEYCLOAK_CLIENT_ID} \
    VITE_GATEWAY_BASE_URL=${VITE_GATEWAY_BASE_URL}

RUN npm run build

# Stage 2: Production web server (Nginx)
FROM nginx:alpine-slim

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/conf.d/

# Copy built static assets from builder
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
