# ============================================================================
# Dockerfile multi-stage da API "aluguel"
#   1) build   -> compila o jar com o Gradle Wrapper (sem rodar testes)
#   2) runtime -> imagem JRE minima, roda como usuario nao-root
# ============================================================================

# --- 1. Build ---------------------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app

# Copia apenas o necessario para resolver as dependencias primeiro.
# Assim, mudancas no codigo-fonte nao invalidam o cache das dependencias.
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Agora copia o codigo-fonte e gera o jar (testes rodam fora do build da imagem).
COPY src ./src
RUN ./gradlew bootJar -x test --no-daemon

# --- 2. Runtime -------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine

LABEL org.opencontainers.image.title="aluguel-api" \
      org.opencontainers.image.description="API de aluguel de carros (Spring Boot)" \
      org.opencontainers.image.source="https://github.com/arthur/genericapi" \
      org.opencontainers.image.vendor="Arthur"

WORKDIR /app

# Usuario nao-root: o container nao roda como root.
RUN addgroup -S app && adduser -S -G app app

COPY --from=build --chown=app:app /app/build/libs/aluguel-*.jar app.jar

USER app

EXPOSE 8080

# Checa apenas se a porta 8080 esta aceitando conexoes. Evita depender do
# Spring Actuator (nao incluido no projeto). `nc` vem do busybox do alpine.
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD nc -z localhost 8080 || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
