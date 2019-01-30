# Stage 1.
# clustered-kGenProg をコンパイルし，実行バイナリを生成する
FROM openjdk:8u181-jdk-slim-stretch AS builder
WORKDIR /root
COPY . .
RUN ./gradlew installDist

# Stage 2.
# Stage 1 で生成したバイナリを保持する
FROM openjdk:8u181-jdk-alpine3.8

LABEL description="Executes clustered-kGenProg."
LABEL maintainer="Hiroyuki Matsuo <h.matsuo.engineer@gmail.com>"

RUN adduser -S kgenprog
USER kgenprog
WORKDIR /home/kgenprog

COPY --from=builder /root/node/build/install/node .
COPY ./kubernetes/liveness-probe-script-on-coordinator.sh .

EXPOSE 50051

CMD ["./bin/kGenProg-coordinator"]
