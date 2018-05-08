FROM openjdk:10.0.1-jre-slim
COPY build/libs/reactive-kotlin-chain-0.0.1-SNAPSHOT.jar /usr/local/reactive-chains/reactive-kotlin-chain.jar
WORKDIR /usr/local/reactive-chains
EXPOSE 8090
CMD ["java", "-jar", "reactive-kotlin-chain.jar"]