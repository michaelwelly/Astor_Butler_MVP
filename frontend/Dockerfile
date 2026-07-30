FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:22-alpine AS build
WORKDIR /app
ARG NEXT_PUBLIC_SITE_URL=http://localhost:3001
ARG NEXT_PUBLIC_API_BASE_URL=http://localhost:8089
ARG NEXT_PUBLIC_WEB_CHAT_ENDPOINT=/api/chat
ARG NEXT_PUBLIC_MEDIA_BASE_URL=
ENV NEXT_PUBLIC_SITE_URL=$NEXT_PUBLIC_SITE_URL
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_WEB_CHAT_ENDPOINT=$NEXT_PUBLIC_WEB_CHAT_ENDPOINT
ENV NEXT_PUBLIC_MEDIA_BASE_URL=$NEXT_PUBLIC_MEDIA_BASE_URL
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3001
ENV HOSTNAME=0.0.0.0
RUN addgroup -S nextjs && adduser -S nextjs -G nextjs
COPY --from=build /app/public ./public
COPY --from=build --chown=nextjs:nextjs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nextjs /app/.next/static ./.next/static
USER nextjs
EXPOSE 3001
CMD ["node", "server.js"]
