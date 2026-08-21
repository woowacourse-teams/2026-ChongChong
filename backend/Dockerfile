FROM eclipse-temurin:25-jre-noble

WORKDIR /app

RUN groupadd --system chongchong \
    && useradd --system --gid chongchong --no-create-home chongchong

COPY --chown=chongchong:chongchong build/libs/*.jar app.jar

USER chongchong

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
