# Imagen del servidor FlanBross (compilacion en dos etapas).
FROM eclipse-temurin:21-jdk AS compilar
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
# mvnw puede traer saltos de linea de Windows (CRLF); aqui se normalizan.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilar /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
