# 多阶段构建：builder 阶段打包（含 Maven 依赖缓存），运行时只带 JRE 镜像，镜像体积小
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 先复制 pom 单独拉依赖：源码不变时依赖层可复用，构建提速
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
