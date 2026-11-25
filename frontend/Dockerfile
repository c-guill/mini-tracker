# Stage 1: Build de l'application Vue 3
FROM node:22-alpine AS builder

WORKDIR /app

# Mise en cache des dépendances npm
COPY package*.json ./
RUN npm ci

# Déclaration de l'argument de build VITE_API_URL
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

# Copie des sources et compilation de production
COPY . .
RUN npm run build

# Stage 2: Serveur Web Nginx Alpine
FROM nginx:alpine

# Configuration Nginx avec gestion du fallback SPA et port 80
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copie des artefacts de build statiques
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
