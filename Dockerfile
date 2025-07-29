FROM eclipse-temurin:17-jdk AS build

WORKDIR /build
COPY ./sbtx ./build.sbt ./
COPY ./project/ ./project/
RUN ./sbtx update
COPY ./src ./src
RUN ./sbtx test assembly

FROM eclipse-temurin:17-jre AS bash-prod

WORKDIR /app
COPY --from=build /build/target/scala*/*.jar /app/sum-searcher.jar

CMD [ "java", "-jar", "sum-searcher.jar" ]

