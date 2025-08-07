package com.media.sslmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DomainSslMonitorApplication {

	public static void main(String[] args) {
		SpringApplication.run(DomainSslMonitorApplication.class, args);
	}
}