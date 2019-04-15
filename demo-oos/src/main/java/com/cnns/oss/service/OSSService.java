package com.cnns.oss.service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.cnns.oss.common.enumeration.FileOperation;
import com.cnns.oss.common.file.FileExecutor;
import com.cnns.oss.common.file.OSSFactory;
import com.cnns.oss.dao.RemoteDirMapper;
import com.cnns.oss.dao.RemoteFileMapper;
import com.cnns.oss.dao.dto.FileAble;
import com.cnns.oss.dao.dto.RemoteDir;
import com.cnns.oss.dao.dto.RemoteFile;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
/**
 * 该类提供以下方法以供操作OSS服务器
 * 1.上传单个文件到指定目录的方法
 * 2.下载单个文件到指定本地目录的方法
 * 3.批量下载文件的方法
 * 4.批量上传文件的方法
 * 5.获取某个bucket上某个key的所有文件信息的方法
 * 
 *问题:
 *	1.批量下载时,可能造成大量的线程被启用,可能造成CPU负担过大
 *	2.如果某个OSSClient对象创建了过多的连接,可能会出问题
 *		//可以使用OSSClient对象池
 *	等实际测试之后再考虑改进
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
	
	
	/**
	 * 得到key对应的远程文件夹的所有内容的摘要
	 * @param bucketName
	 * @param remotePrefix
	 */
	public List<OSSObjectSummary> getSummary(String bucketName,String remotePrefix) {

		List<OSSObjectSummary> list = new ArrayList<>();
		
		ObjectListing listObjects = staticClient.listObjects(bucketName, remotePrefix);
		List<OSSObjectSummary> objectSummaries = listObjects.getObjectSummaries();
		for (OSSObjectSummary ossObjectSummary : objectSummaries) {
			list.add(ossObjectSummary);
		}
		return list;
		/*
		for (OSSObjectSummary summary : objectSummaries) {
			System.out.println("key:"+bucketName+"/"+summary.getKey());	//该文件在OSS上的路径
			System.out.println("\tsize:"+summary.getSize());
			System.out.println("\tmd5:"+summary.getETag());	//该文件的hash值
			System.out.println("\tstorageClass:"+summary.getStorageClass());
			System.out.println("\tlastModifiedTime:"+
			sdf.format(summary.getLastModified()));
			System.out.println("\towner:"+summary.getOwner());
		}
		*/
	}
	
	@Autowired
	private RemoteFileMapper remoteFileMapper;
	@Autowired
	private RemoteDirMapper remoteDirMapper;
	
	/**
	 * 从远程服务器上得到文件信息之后,将文件信息插入到数据库中
	 * @param list
	 */
	public void insertRemoteFiles(List<OSSObjectSummary> list) {
		int flagNum = 0;	//由于文件数量可能非常多,为了避免过多的信息同时传到数据库,使用多线程的方式逐步插入
		LinkedList<List<RemoteDir>> dirOuterList = new LinkedList<List<RemoteDir>>();
		LinkedList<List<RemoteFile>> fileOuterList = new LinkedList<List<RemoteFile>>();
		
		Executor executor = Executors.newCachedThreadPool();
		for (OSSObjectSummary summary : list) {
			if(flagNum % 10000 ==0) {
				List<RemoteDir> dirList = new ArrayList<RemoteDir>();
				List<RemoteFile> fileList = new ArrayList<RemoteFile>();
				dirOuterList.add(dirList);
				fileOuterList.add(fileList);
				
				//每隔10000次,启用另一个线程把远程文件和文件夹插入数据库中
				if(flagNum != 0) {
					executeInsert(dirOuterList, fileOuterList, executor);
				}
			}
			FileAble f = getFile(summary);
			if(f instanceof RemoteDir) {
				dirOuterList.getLast().add((RemoteDir)f);
			}else if(f instanceof RemoteFile) {
				fileOuterList.getLast().add((RemoteFile)f);
			}
			if(flagNum==list.size()-1 && flagNum %10000!=0) {
				executeInsert(dirOuterList, fileOuterList, executor);
			}
			flagNum ++;
			
		}
	}

	
	private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());
	/**
	 * 将从OSS得到的文件摘要转换成数据库中的RemoteFile的方法
	 * 如果在表中已存在相同内容,则直接放弃本次插入
	 * 如果只有部分相等,则直接抛出一个异常
	 */
	private void executeInsert(LinkedList<List<RemoteDir>> dirOuterList, LinkedList<List<RemoteFile>> fileOuterList,
			Executor executor) {
		executor.execute(()->{
			List<RemoteFile> innerFileList = fileOuterList.getLast();
			List<RemoteDir> innerDirList = dirOuterList.getLast();
			
			logger.info("文件长度为:"+innerFileList.size());
			logger.info("文件夹长度为:"+innerFileList.size());
			
			if(innerDirList.size()!=0) {
				int exist = remoteDirMapper.isExist(innerDirList);
				if(exist==0) {
					remoteDirMapper.insertByBatch(innerDirList);
					logger.info("目录信息插入成功!");
				}else if(exist != innerDirList.size()) {
					throw new RuntimeException("部分远程目录已存在与数据库中");
				}else {
					logger.warn("远程目录与数据库中的内容完全一样!");
				}
			}
			
			if(innerFileList.size()!=0) {
				int exist = remoteFileMapper.isExist(innerFileList);
				if(exist==0) {
					remoteFileMapper.insertByBatch(innerFileList);
					logger.info("文件信息插入成功!");
				}else if(exist != innerFileList.size()) {
					throw new RuntimeException("部分远程文件已存在与数据库中");
				}else {
					logger.warn("远程文件与数据库中的内容完全一样!");
				}
			}
			
		});
	}
	
	/**
	 * 把OSS服务的文件摘要转换成相关的文件/文件夹对象
	 * @param summary
	 * @return
	 */
	public FileAble getFile(OSSObjectSummary summary) {
		String key = summary.getKey();
		if(key.endsWith("/")) {
			RemoteDir remoteDir = new RemoteDir();
			remoteDir.setDirName(key);
			remoteDir.setHash(summary.getETag());
			remoteDir.setRank(key.split("/").length);
			return remoteDir;
		}else {
			RemoteFile remoteFile = new RemoteFile();
			remoteFile.setFileFullName(key);
			if(key.contains("/"))
				remoteFile.setFileName(key.substring(key.lastIndexOf("/"),key.length()));
			else 
				remoteFile.setFileName(key);
			remoteFile.setSize(summary.getSize());
			remoteFile.setLastModifyTime(summary.getLastModified());
			remoteFile.setHash(summary.getETag());
			return remoteFile;
		}
	}
	
	public void insertSummaries(String bucketName,String key) {
		List<OSSObjectSummary> list = getSummary(bucketName,key);
		System.err.println(list);
		insertRemoteFiles(list);
	}
	
}
