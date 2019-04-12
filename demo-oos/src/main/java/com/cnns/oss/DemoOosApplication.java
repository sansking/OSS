package com.cnns.oss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@PropertySource("classpath:oos.properties")
@EnableAspectJAutoProxy

public class DemoOosApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoOosApplication.class, args);
	}

}
