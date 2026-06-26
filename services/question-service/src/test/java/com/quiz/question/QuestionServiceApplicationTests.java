package com.quiz.question;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
	classes = QuestionServiceApplication.class,
	properties = {
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false"
	}
)
class QuestionServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
