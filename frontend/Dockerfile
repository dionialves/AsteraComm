FROM node:18-alpine

WORKDIR /app

RUN npm install
RUN npm run build

EXPOSE 4321

CMD ["node", "dist/server/entry.mjs"]

