FROM sbtscala/scala-sbt:eclipse-temurin-focal-17.0.10_7_1.9.9_2.13.13
WORKDIR /home
COPY . /home

RUN sbt compile
RUN sbt assembly
RUN mv target/scala-2.13/smartcloud-prices.jar smartcloud-prices.jar
EXPOSE 8080
# CMD ["java", "-jar", "smartcloud-prices.jar"]