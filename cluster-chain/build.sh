#!/bin/bash
(cd ../reactive-java-chain && ./mvnw clean verify)
(cd ../reactive-kotlin-chain && ./gradlew clean build)

docker-compose build