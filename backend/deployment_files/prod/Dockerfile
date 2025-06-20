FROM openjdk:21-jdk-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

#Build context should be the the goal tracker folder

WORKDIR /app

COPY backend/target/goal-tracker-0.0.1-SNAPSHOT.jar goal-tracker.jar

CMD ["java", "-jar", "goal-tracker.jar"]