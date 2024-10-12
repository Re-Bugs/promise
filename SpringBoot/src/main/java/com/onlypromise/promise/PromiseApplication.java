package com.onlypromise.promise;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@Slf4j
@SpringBootApplication
public class PromiseApplication {

	public static void main(String[] args) {
		SpringApplication.run(PromiseApplication.class, args);
	}

	@PostConstruct
	public void init()
	{
		// JVM의 기본 시간대를 Asia/Seoul로 설정
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		log.info("-----------------------------------------------------");
		log.info("---------------------서버 재시작 됨---------------------");
		log.info("-----------------------------------------------------");
	}
}
