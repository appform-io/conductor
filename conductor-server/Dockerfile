FROM azul/zulu-openjdk:17-latest

EXPOSE 8080
EXPOSE 8081
EXPOSE 8082
EXPOSE 8083

ARG VERSION

ADD target/conductor-server-$VERSION.jar conductor-server.jar
ADD entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]

