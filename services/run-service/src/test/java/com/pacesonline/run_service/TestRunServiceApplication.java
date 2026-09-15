package com.pacesonline.run_service;

import org.springframework.boot.SpringApplication;

public class TestRunServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(RunServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
