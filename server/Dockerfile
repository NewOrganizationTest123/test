FROM openjdk:17-alpine

RUN apk --no-cache add gcompat
ENV LD_PRELOAD=/lib/libgcompat.so.0

ENV PORT= \
    CERT= \
    KEY= \
    LOG_LEVEL=

ARG NAME
ARG VERSION

RUN mkdir /usr/app
WORKDIR /usr/app

COPY ./$NAME-$VERSION.jar ./wizard-server.jar
COPY ./docker-entrypoint.sh ./docker-entrypoint.sh

RUN chmod +x docker-entrypoint.sh

EXPOSE 50051
ENTRYPOINT ["./docker-entrypoint.sh"]
