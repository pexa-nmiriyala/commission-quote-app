# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency files first for layer caching
COPY package.json package-lock.json* ./
RUN npm ci --ignore-scripts

# Copy source
COPY . .

# Build-time Keycloak config — these are baked into the static bundle by Vite.
# Defaults point to the Keycloak container exposed on the host at port 9090.
ARG VITE_KEYCLOAK_URL=http://localhost:9090
ARG VITE_KEYCLOAK_REALM=commission-app
ARG VITE_KEYCLOAK_CLIENT_ID=commission-app-client

ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID

RUN npm run build

# ── Stage 2: Serve with Nginx ─────────────────────────────────────────────────
FROM nginx:alpine AS runtime

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy nginx config (proxies /api/* to backend container)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
