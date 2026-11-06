# Optional deployment (requires Docker).
# Build and run with:
#   docker build -t quizly .
#   docker run --rm -p 8080:8080 quizly
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM tomcat:10.1.61-jdk17-temurin
COPY --from=build /workspace/target/quizly.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
