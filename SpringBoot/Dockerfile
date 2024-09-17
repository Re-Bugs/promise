# 베이스 이미지로 OpenJDK 17 사용
FROM openjdk:17-jdk

# 애플리케이션을 위한 작업 디렉토리 설정
WORKDIR /spring-boot

# 빌드된 JAR 파일을 컨테이너로 복사
COPY build/libs/*SNAPSHOT.jar promise.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/spring-boot/promise.jar"]