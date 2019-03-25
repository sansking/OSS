package com.demo.oss.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.oss.model.DownloadFileRequest;
import com.aliyun.oss.model.DownloadFileResult;
import com.aliyun.oss.model.ObjectMetadata;

public class FileExecutor implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(FileExecutor.class);	//打印日志的logger对象
	private OSSFileInfo  fileInfo;	//包含文件上传下载基本信息的类
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//只允许本包下的FileExecutorUtils创建本对象,因此,将构造方法设为friendly
	FileExecutor(OSSFileInfo fileInfo) {
		if(fileInfo.client==null||fileInfo.localFile==null||fileInfo.destFile==null||fileInfo.operation==null)
			throw new RuntimeException("信息不足,无法创建FileExecutor对象!");
		this.fileInfo = fileInfo;
	}
	
	/**
	 * 需要测试oos是否可以复用,即同一个oos对象,当某个线程正在使用该对象时,另一个线程是否可以继续使用同一个oos对象而不会阻塞,
	 * 以决定多个本类对象使用同一个OSSClient 还是对每一个线程创建一个不同的 OSSClient
	 */
	@Override
	public void run() {
		if(fileInfo.operation==FileOperation.SIMPLE_UPLOAD) {
			fileUpload();
		}else if(fileInfo.operation==FileOperation.SIMPLE_DOWNLOAD) {
			fileDownLoad();
		}
	}
	
	/**
	 * 使用IO流的方式进行文件上传,仅上传单个文件
	 */
	private void fileUpload() {
		InputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(fileInfo.localFile));
			long time = System.currentTimeMillis();
			logger.info("上传文件{}中",fileInfo.localFile.getName());
			fileInfo.client.putObject(fileInfo.bucketName,fileInfo.destFile, inputStream);
			double interval = (System.currentTimeMillis()-time)/1000.0;
			logger.info("文件上传成功,耗时{}秒,当前时间{}",interval,sdf.format(new Date()));
		} catch (FileNotFoundException e) {
			logger.error("待上传的文件不存在");
			throw new RuntimeException(e);
		}finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}finally {
				inputStream = null;
			}
		}
	}
	
	/**
	 * 使用断点续传的方式下载文件的方法,仅下载单个文件
	 */
	private void fileDownLoad() {
		File downFile = fileInfo.localFile;
		/*
		 * 如果父路径不存在,则创建父文件夹
		 */
		if(!downFile.getParentFile().exists())
			downFile.getParentFile().mkdirs();
		
		/*
		 * 创建下载请求,并设置参数
		 */
		DownloadFileRequest request = new DownloadFileRequest(fileInfo.bucketName,fileInfo.destFile);
		request.setDownloadFile(downFile.getAbsolutePath());
		request.setPartSize(1 * 1024 * 1024);
		request.setTaskNum(10);
		request.setEnableCheckpoint(true);
		String downFileStr = downFile.getAbsolutePath();
		request.setCheckpointFile(downFileStr.substring(0,downFileStr.lastIndexOf("."))+".bak");
		
		/*
		 * 下载文件
		 */
		logger.info("下载文件开始!");
		try {
			DownloadFileResult downloadFile = fileInfo.client.downloadFile(request);
			Thread.sleep(5000);
			logger.info("文件正在下载中!-->当前时间为:"+System.currentTimeMillis());
			ObjectMetadata metadata = downloadFile.getObjectMetadata();
			logger.info("文件下载完成,下载的文件大小为{}Byte!\r\n\t当前时间为{}",
					metadata==null?0:metadata.getContentLength(),System.currentTimeMillis());
		} catch (Throwable e) {
			throw new RuntimeException("下载文件时出现错误",e);
		}
	}
	
}
