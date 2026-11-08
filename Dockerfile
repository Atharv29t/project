# Optional deployment (requires Docker).
# Build and run with:
#   docker build -t quizly .
#   docker run --rm -e PORT=8080 -p 8080:8080 quizly
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

FROM tomcat:10.1.61-jdk17-temurin
COPY --from=build /workspace/target/quizly.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 80
CMD ["sh", "-c", "port=\"${PORT:-80}\"; sed -i \"s/port=\\\"8080\\\"/port=\\\"$port\\\"/\" \"$CATALINA_HOME/conf/server.xml\"; exec catalina.sh run"]
