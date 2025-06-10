FROM openjdk:21-jdk-slim

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

#It is assumed that the volume passed will have the git repo cloned in /app
WORKDIR /app/backend
CMD ["./mvnw", "spring-boot:run"]
