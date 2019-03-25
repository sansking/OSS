package com.demo.oos;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.demo.oss.service.OSSService;

@RunWith(SpringRunner.class)
@SpringBootTest
/*
	@SpringBootApplication
	@PropertySource("classpath:oos.properties")
	@ComponentScan("com.demo.oos.service")
*/
public class DemoOosApplicationTests {
/*
	@Autowired
	OSSService ossService;
	
	@Test
	public void contextLoads() {
		ossService.downLoadFile("bucket-test-demo1", "test1/testDir/testFile.txt", "d://download/testFile.txt");
	}
*/
}
