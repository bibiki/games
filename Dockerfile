FROM openjdk:8-alpine

COPY target/uberjar/games.jar /games/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/games/app.jar"]
