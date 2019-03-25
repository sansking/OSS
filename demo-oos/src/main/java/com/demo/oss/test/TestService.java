package com.demo.oss.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.demo.oss.service.OSSService;

@Service
public class TestService {

	@Autowired
	OSSService ossService;
	
	@Scheduled( fixedDelay=1000000)
	public void test() {
		//ossService.downLoadFile("bucket-test-demo1", "test1/testDir/testFile.txt", "d://download/testFile.txt");
		//ossService.batchDownload("bucket-test-demo1", "test1", new File("d:/download"));
		ossService.uploadFile("bucket-test-demo1", "test1", "D:\\TestDir\\upfiles");
		//ossService.closeClient();
	}
}
