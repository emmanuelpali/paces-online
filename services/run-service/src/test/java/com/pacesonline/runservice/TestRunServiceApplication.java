package com.pacesonline.runservice;

import org.springframework.boot.SpringApplication;

public class TestRunServiceApplication {

	public static void main(String[] args) {
		SpringApplication
				.from(RunServiceApplication::main)
				.with(
						TestcontainersConfiguration.class,
						SecurityTestConfiguration.class
				)
				.withAdditionalProfiles("test")
				.run(args);
	}
}
