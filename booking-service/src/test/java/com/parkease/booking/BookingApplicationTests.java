package com.parkease.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		// Disable config server lookup
		"spring.cloud.config.enabled=false",
		"spring.config.import=optional:configserver:",

		// Use H2 in-memory instead of real PostgreSQL
		"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"spring.jpa.hibernate.ddl-auto=create-drop",

		// Disable Eureka registration
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",

		// Disable RabbitMQ auto-connect
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",

		// Random port
		"server.port=0"
})
class BookingApplicationTests {

	@Test
	void contextLoads() {
	}

}
