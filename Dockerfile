FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY src ./src
COPY data ./data
COPY web ./web
RUN mkdir out && javac -encoding UTF-8 -d out src/*.java
EXPOSE 8080
CMD ["sh", "-c", "java -cp out GameApp ${PORT:-8080}"]
