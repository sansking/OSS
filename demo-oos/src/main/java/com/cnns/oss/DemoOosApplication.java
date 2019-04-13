package com.cnns.oss;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@PropertySource("classpath:oos.properties")
@EnableAspectJAutoProxy
@MapperScan({"com.baomidou.mybatisplus.samples.quickstart.mapper","com.cnns.oss.dao"})
public class DemoOosApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoOosApplication.class, args);
	}

}
