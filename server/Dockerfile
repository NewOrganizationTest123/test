FROM openjdk:17-jdk-slim

ARG NAME
ARG VERSION

RUN mkdir /usr/app
WORKDIR /usr/app

COPY ./$NAME-$VERSION.jar /usr/app/wizard-server.jar

EXPOSE 50051
ENTRYPOINT ["java", "-jar", "wizard-server.jar"]
