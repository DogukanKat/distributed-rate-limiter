# ---- Build stage ----
FROM gradle:8.8-jdk17 AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :rate-gateway:bootJar --no-daemon

# ---- Run stage ----
# Alpine yerine Debian/Ubuntu tabanlı, multi-arch destekli bir imaj kullan
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# TZ ayarı (opsiyonel)
RUN ln -sf /usr/share/zoneinfo/Europe/Istanbul /etc/localtime
COPY --from=build /workspace/rate-gateway/build/libs/*.jar app.jar
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
