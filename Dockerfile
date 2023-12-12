FROM eclipse-temurin:17-jdk-alpine
LABEL authors="chulkov-alex"
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
