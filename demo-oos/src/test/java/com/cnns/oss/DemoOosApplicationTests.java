package com.cnns.oss;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.aliyun.oss.model.OSSObjectSummary;
import com.cnns.oss.service.OSSService;

@RunWith(SpringRunner.class)
@SpringBootTest
/*
	@SpringBootApplication
	@PropertySource("classpath:oos.properties")
	@ComponentScan("com.demo.oos.service")
*/
public class DemoOosApplicationTests {
	
	@Autowired
	OSSService ossService;
	
	@Test
	public void testService() {
		
		//ossService.downLoadFile("bucket-test-demo1", "test1randomFile.txt", "d://download/testRandomFile.txt");
		//ossService.batchDownload("bucket-test-demo1", "", new File("d:/download"));
		//ossService.uploadFile("bucket-test-demo1", "test1", "D:\\TestDir\\upfiles");
		//ossService.closeClient();
		//ossService.showSample("bucket-test-demo1", "");
		
		
	}
	
	//@Test
	public void testRandomFile() {
		/*
		RandomFileConfig config = new RandomFileConfig();
		config.setFilePath("D:\\TestDir\\upfiles\\randomFile.txt");
		config.setMinLength(10);
		config.setMaxLength(30);
		config.setUnit(Unit.MB);
		config.setLang(Language.BOTH);
		GenRandomFile.genRandomFile(config);
		*/
		//GenRandomFile.repeatFile(new File("D:\\TestDir\\upfiles\\randomFile.txt"),10);
	}
	
}
