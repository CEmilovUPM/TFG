package com.project.goal_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
//@PropertySource("classpath:secrets.properties")
public class GoalTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoalTrackerApplication.class, args);
	}

}
