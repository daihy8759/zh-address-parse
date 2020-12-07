FROM mhart/alpine-node:15

MAINTAINER sdyf dai875939260@163.com

WORKDIR /app

COPY package.json package-lock.json ./

RUN npm ci --prod --registry=https://registry.npm.taobao.org

FROM mhart/alpine-node:slim-15

WORKDIR /app
COPY --from=0 /app .
COPY . .
CMD ["node", "index.js"]