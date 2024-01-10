package com.hellostranger.chessserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChessServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChessServerApplication.class, args);
	}

}
