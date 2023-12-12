FROM eclipse-temurin:17-jdk-alpine
LABEL authors="chulkov-alex"
VOLUME /tmp
COPY mafia-backend-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
