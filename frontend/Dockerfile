FROM node:18-alpine

WORKDIR /app

COPY app/package*.json ./
RUN npm install
RUN npm install @astrojs/node

EXPOSE 4321

CMD ["sh", "-c", "npm install && npm run build && node dist/server/entry.mjs"]
