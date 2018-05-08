FROM openjdk:10.0.1-jre-slim
COPY target/reactive-java-chain-0.0.1-SNAPSHOT.jar /usr/local/reactive-chains/reactive-java-chain.jar
WORKDIR /usr/local/reactive-chains
EXPOSE 8080
CMD ["java", "-jar", "reactive-java-chain.jar"]