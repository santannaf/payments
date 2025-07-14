FROM ghcr.io/graalvm/graalvm-community:24 AS builder
WORKDIR /app

COPY . .

RUN chmod 777 ./gradlew && \
    ./gradlew clean build && \
    ./gradlew nativeCompile

FROM container-registry.oracle.com/os/oraclelinux:9-slim
COPY --from=builder /app/build/native/nativeCompile/payment /app/meuapp
RUN chmod 777 /app/meuapp
ENTRYPOINT ["/app/meuapp", "-Xmx145m"]
