# FROM openjdk:8u181-jdk-slim-stretch AS build
FROM openjdk:8u181-jdk-slim-stretch

WORKDIR /root

COPY . .

RUN ./gradlew installDist


FROM openjdk:8u181-jdk-alpine3.8

LABEL description="Executes clustered-kGenProg."
LABEL maintainer="Hiroyuki Matsuo <h.matsuo.engineer@gmail.com>"

RUN adduser -S kgenprog

USER kgenprog

WORKDIR /home/kgenprog

COPY --from=build /root/node/build/install/node .

EXPOSE 50051
