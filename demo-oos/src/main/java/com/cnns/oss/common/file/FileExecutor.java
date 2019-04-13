package com.cnns.oss.common.file;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.oss.model.DownloadFileRequest;
import com.aliyun.oss.model.DownloadFileResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.UploadFileRequest;
import com.cnns.oss.common.DownloadListener;
import com.cnns.oss.common.ThreadConfig;
import com.cnns.oss.common.UploadListener;
import com.cnns.oss.common.enumeration.FileOperation;

public class FileExecutor implements Runnable{
	
	private static Logger logger = LoggerFactory.getLogger(FileExecutor.class);	//打印日志的logger对象
	private OSSFileInfo  fileInfo;	//包含文件上传下载基本信息的类
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	FileExecutor(){}
	
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
		System.err.println("Before:当前线程数:"+Thread.activeCount());
		switch(fileInfo.operation) {
			case RESUMABLE_UPLOAD:
				fileUpload();
				break;
			case RESUMABLE_DOWNLOAD:
				fileDownLoad();
				break;
			default:
				throw new RuntimeException("未知的上传下载选项,请参见:"+FileOperation.class);
		}
		System.err.println("After:当前线程数:"+Thread.activeCount());
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
			
			UploadFileRequest uploadReqeust = new UploadFileRequest(fileInfo.bucketName,fileInfo.destFile);
			uploadReqeust.setPartSize(ThreadConfig.getPartSize());
			uploadReqeust.setEnableCheckpoint(true);
			uploadReqeust.setTaskNum(ThreadConfig.getTaskNum());
			uploadReqeust.setProgressListener(new UploadListener());
			File f = fileInfo.localFile;
			String backPath = new File(f.getParentFile(),f.getName()+".bak").getAbsolutePath();
			logger.debug("备份文件存储在: "+backPath);
			uploadReqeust.setCheckpointFile(backPath);
			uploadReqeust.setUploadFile(f.getAbsolutePath());
			fileInfo.client.uploadFile(uploadReqeust);
			double interval = (System.currentTimeMillis()-time)/1000.0;
			logger.info("文件{}上传成功,耗时{}秒,当前时间{}",f.getAbsolutePath(),interval,sdf.format(new Date()));
		} catch (Throwable e) {
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
		request.setPartSize(ThreadConfig.getPartSize());
		request.setTaskNum(ThreadConfig.getTaskNum());
		request.setEnableCheckpoint(true);
		String downFileStr = downFile.getAbsolutePath();
		request.setCheckpointFile(downFileStr.substring(0,downFileStr.lastIndexOf("."))+".bak");
		request = request.<DownloadFileRequest>withProgressListener(new DownloadListener());
		/*
		 * 下载文件
		 */
		logger.info("下载文件开始!");
		try {
			DownloadFileResult downloadFile = fileInfo.client.downloadFile(request);
			ObjectMetadata metadata = downloadFile.getObjectMetadata();
		} catch (Throwable e) {
			throw new RuntimeException("下载文件时出现错误",e);
		}
	}
	
	
	/**
	 * 从checkpoint文件中恢复断点内容
	 * 返回一个HashMap,包含该断点的内容
	 * @param checkpoint
	 * @throws IOException
	 */
	public Map<String,String> resume(File checkpoint) throws IOException {
		BufferedReader br = new BufferedReader(
				new FileReader(checkpoint));
		Map<String,String> hm = new HashMap<>();
		String line = null;
		while((line=br.readLine())!=null) {
			line = line.replace("\\s", "");
			if(line.isEmpty())
				continue;
			if(!line.contains("=")) {
				throw new RuntimeException("不正确的checkpoint文件格式: 没有=号");
			}
			if(line.contains("#"))	//如果包含#号,则去掉#后的内容
				line = line.substring(0, line.indexOf("#"));
			String[] split = line.split("=");
			if(split.length>2)
				throw new RuntimeException("不正确的checkpoint文件格式: =多余一个");
			hm.put(split[0].toLowerCase(),split[1]);
		}
		br.close();
		logger.info("从文件{}中,恢复了断点信息",checkpoint.getAbsolutePath());
		return hm;
	}
	
	/**
	 * 该方法用于实现一个可恢复的下载
	 * 可恢复的下载需要在下载过程中,保存如下信息:
	 * 	1.重新连接到服务端的相关参数:
	 * 		bucketName,endpoint等
	 *  2.已下载到本地的文件的相关信息,对于每一个部分,都应该有如下信息:
	 *  	a.对应的远程文件的绝对文件名
	 *  	b.对应的远程文件的哪个字节部分
	 *  	c.还需要下载哪些部分
	 *  	-以类似于{key:key,remoteFileStart:0,remoteFileEnd:1024*1024,needDownStart:1024,needDownEnd:2048}
	 *  		的格式存储
	 */
	public void resumableDownload() {
		
	}
	
	/**
	 * 该方法用于保存当前下载进度;该方法应该每隔5~10秒被调用一次
	 */
	public void saveCheckPoint() {
		
	}
	
	
}
