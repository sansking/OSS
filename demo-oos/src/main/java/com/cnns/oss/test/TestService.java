package com.cnns.oss.test;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.cnns.oss.service.OSSService;

@Service
public class TestService {

	@Autowired
	OSSService ossService;
	
	@Scheduled( fixedDelay=1000000)
	public void test() {
		//ossService.downLoadFile("bucket-test-demo1", "test1randomFile.txt", "d://download/testRandomFile.txt");
		ossService.batchDownload("bucket-test-demo1", "", new File("d:/download"));
		//ossService.uploadFile("bucket-test-demo1", "test1", "D:\\TestDir\\upfiles");
		//ossService.closeClient();
	}
}
