package com.cnns.oss.service;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.cnns.oss.util.FileExecutor;
import com.cnns.oss.util.FileOperation;
import com.cnns.oss.util.OSSFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
/**
 * 该类提供以下方法以供操作OSS服务器
 * 1.创建文件夹的方法
 * 2.上传单个文件到指定目录的方法
 * 3.下载单个文件到指定本地目录的方法
 * 4.批量下载文件的方法
 * 5.批量上传文件的方法
 * 6.关闭OSSClient的方法	//如果大量的线程被启用,导致某个OSSClient创建的
 * 
 *问题:
 *	1.批量下载时,可能造成大量的线程被启用,可能造成CPU负担过大
 *		//使用线程池改进
 * @author wangp
 *
 */
@Service
@Api(value="oss service")
public class OSSService {
	
	private static Executor executor;
	
	//注入一个静态的factory以产生ossClient对象
	private static OSSFactory ossFactory;
	@Autowired
	public void setOssFactory(OSSFactory ossFactory) {
		OSSService.ossFactory = ossFactory;
	}
	private static OSSClient staticClient;	//使用静态的OSSClient以供所有方法使用
	
	@Value("${aliyun.threadPoolNum}")
	private int threadPoolNum;
	
	/**
	 * 由于必须要构造函数加载后,才能进行自动注入;
	 * 因此,使用PostConstruct注解来指定得到OssClient的时机 
	 */
	@PostConstruct
	public void injectParams() {
		staticClient = ossFactory.getStaticOssClient();
		if(threadPoolNum <= 1)
			threadPoolNum=1;
		if(threadPoolNum >= 50)
			threadPoolNum = 50;	//为了避免线程数量超出限制,最大线程数设置为50
		executor = Executors.newFixedThreadPool(threadPoolNum);
	}
	
	/**
	 * 对于静态的OssClient而言,只需要在该类的对象被销毁之前关闭即可
	 */
	@PreDestroy
	public void destoryClient() {
		if(staticClient != null)
			staticClient.shutdown();
	}
	
	
	//上传文件方法的重载,使用String形式的文件路径
	public void uploadFile(String bucketName,String objectName,String upFile) {
		uploadFile(bucketName,objectName,new File(upFile));
	}
	
	@ApiOperation(value = "用于上传单个文件/文件夹的方法")
	public void uploadFile(String bucketName,String objectName,File upFile) {
		if(!upFile.exists())
			throw new RuntimeException("要上传的文件不存在!请检查文件");
		if(upFile.isDirectory()) {
			String[] children = upFile.list();
			for (String child : children) {
				File childFile = new File(upFile,child);
				objectName = objectName.endsWith("\\")?objectName+"/"+child:objectName+child;
				uploadFile(bucketName,objectName,childFile);
			}
		}else {
			FileExecutor fileExecutor = OSSFactory.getFileExecutor(staticClient, upFile, bucketName, objectName, FileOperation.RESUMABLE_UPLOAD);
			executor.execute(fileExecutor);
		}
	}
	
	
	/**
	 * 下载文件的方法,使用断点续传的方式进行下载
	 * @param bucketName : oos服务器上的bucket的名称
	 * @param objectName : 下载对象的名称
	 * @param downFile : 文件下载完成后,将要存放的路径
	 */
	public void downLoadFile(String bucketName,String objectName,File downFile) {
		FileExecutor fileExecutor = OSSFactory.getFileExecutor(staticClient, downFile, bucketName, objectName, FileOperation.RESUMABLE_DOWNLOAD);
		executor.execute(fileExecutor);
	}
	
	/**
	 * 用于下载文件夹的方法
	 * @param bucketName
	 * @param prefix	OOS服务器上的文件前缀,即某个文件的目录
	 * @param dir 		本地的文件夹名,以指定要存放的位置
	 */
	public void batchDownload(String bucketName,String prefix,File dir) {
		if(!dir.exists()) dir.mkdirs();
		ObjectListing objectListing = staticClient.listObjects(bucketName, prefix);
		List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
		for (OSSObjectSummary s : sums) {
			String key = s.getKey();
			File f = new File(dir,key.substring(prefix.length(),key.length()));
			if(!key.endsWith("/")) {
				downLoadFile(bucketName,s.getKey(),f);
			}else {
				if(!f.exists())
					f.mkdirs();
			}
		}
	}
	
	/**
	 * 以上方法的重载方法,使用文件名指定下载文件的位置
	 */
	public void downLoadFile(String bucketName,String objectName,String downFile) {
		downLoadFile(bucketName,objectName,new File(downFile));
	}
	
	
	
}
