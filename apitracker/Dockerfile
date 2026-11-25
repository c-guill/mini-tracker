# Stage 1: Build Maven
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Mise en cache des dépendances Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compilation et packaging de l'application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime JRE léger
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Exécution en utilisateur non-root pour la sécurité
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Récupération du jar exécutable depuis l'étape de build
COPY --from=builder /build/target/apitracker-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
