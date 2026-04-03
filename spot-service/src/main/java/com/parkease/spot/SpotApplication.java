package com.parkease.spot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
		exclude = { UserDetailsServiceAutoConfiguration.class }
)
public class SpotApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpotApplication.class, args);
	}

}
