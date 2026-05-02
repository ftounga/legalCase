# Le contexte de build est la racine du repo (cf. .dockerignore racine)
# pour permettre la copie de docs/ au runtime — F-178 SF-178-02 lit
# docs/PRODUCT_SPEC.md et docs/MARKETING_BACKLOG.md depuis /app/docs/.

# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN ./mvnw dependency:go-offline -q

COPY backend/src/ src/
RUN ./mvnw clean package -DskipTests -q

# ---- Run stage ----
FROM eclipse-temurin:21-jre-alpine AS run
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar app.jar
COPY docs/ docs/

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
