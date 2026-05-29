# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build -- --configuration production

# Stage 2: Serve
FROM nginx:alpine
# Copiar el build al directorio de Nginx
# NOTA: Ajusta la ruta si Angular usa dist/frontend-angular/browser o similar
COPY --from=build /app/dist/frontend-angular/browser /usr/share/nginx/html

# Configuración personalizada de Nginx para manejar rutas de Angular (SPA)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
