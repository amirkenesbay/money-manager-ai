FROM node:22-alpine AS builder

WORKDIR /webapp

COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run lint && npm test && npm run build

FROM nginx:1.27-alpine

COPY --from=builder /webapp/dist /usr/share/nginx/html
