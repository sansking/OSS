package com.cnns.oss.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cnns.oss.common.GlobalCounter;
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
	
	//private static CompletionService<Map<String,Object>> completionService;
	private static ExecutorService executor;
	private static int totalTaskNum = 0;
	private static List<Future<Map<String,Object>>> taskList = new ArrayList<>();
	
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
		if(threadPoolNum > 100)
			threadPoolNum = 100;	//为了避免线程数量超出限制,最大线程数设置为50
		System.err.println("当前线程池数量为"+threadPoolNum);
		executor = Executors.newFixedThreadPool(threadPoolNum);
		//completionService = new ExecutorCompletionService<>(executor);
	}
	
	/**
	 * 对于静态的OssClient而言,只需要在该类的对象被销毁之前关闭即可
	 */
	@PreDestroy
	public void destoryClient() {
		if(staticClient != null)
			staticClient.shutdown();
	}
	
	
	/**
	 * 该方法的流程为:
	 * 	1.通过参数下载或者上传文件
	 *  2.将下载下来的文件信息保存在对应的数据库表中
	 */
	public void getFullInfo(String operation,String bucketName,String key,String localFile) {
		if("upload".equals(operation)) {
			uploadFile(bucketName,key,localFile);
		}else if("download".equals(operation)) {
			File f = new File(localFile);
			if(f.isDirectory()) {
				batchDownload(bucketName,key,f);
			}else {
				downLoadFile(bucketName,key,localFile);
			}
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		/**
		 * 注:此处的sleep会导致后续任务不执行,原因尚不明确
		 *  可能的情况是: 由于当前线程池可用线程数量较少,
		 *  	待执行任务过多,导致某些待执行任务在await方法之后执行
		 */
		/*
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		*/
		StringBuffer sb = new StringBuffer();
		sb.append("全局计数:").append(GlobalCounter.count).append("\r\n")
			.append("总任务数").append(taskList.size()).append("\r\n")
			.append("全局计数1:").append(GlobalCounter.count1).append("\r\n")
			.append("全局计数2:").append(GlobalCounter.count2);
		logger.info(sb.toString());
		
		try {
			for (Future<Map<String, Object>> future : taskList) {
				if(future.isDone()) {
					Map<String,Object> map = future.get(10,TimeUnit.SECONDS);
					Date finishTime = new java.sql.Date(((Date)map.get("finishTime")).getTime());
					
					String remoteFileName = (String) map.get("key");
					String local = (String) map.get("localFile");
					String bucket = (String) map.get("bucketName");
					QueryWrapper<RemoteFile> query = new QueryWrapper<RemoteFile>()
							.eq(remoteFileName != null && !"".equals(remoteFileName), "file_full_name", remoteFileName)
							.eq(bucket !=null && !"".equals(bucket),"bucket_name", bucket);
					RemoteFile remoteFile = remoteFileMapper.selectOne(query);
					logger.info("远程文件下载完成,远程文件信息为:"+remoteFile);
					remoteFile.setDownloadState(2);
					remoteFileMapper.updateById(remoteFile);
					
				}else {
					System.err.println("任务未完成!");
				}
			}
			
		}catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		
	}
	
	
	
	//上传文件方法的重载,使用String形式的文件路径
	public void uploadFile(String bucketName,String objectName,String upFile) {
		uploadFile(bucketName,objectName,new File(upFile));
	}
	
	@ApiOperation(value = "用于上传单个文件/文件夹的方法")
	/**
	 * 以上将upFile文件/文件夹,上传到 key 所在的文件夹中
	 * 	如果是文件夹,请注意 key 需要以 / 结尾
	 */
	public void uploadFile(String bucketName,String key,File upFile) {
		if(!upFile.exists())
			throw new RuntimeException("要上传的文件不存在!请检查文件");
		if(upFile.isDirectory()) {
			String[] children = upFile.list();
			for (String child : children) {
				File childFile = new File(upFile,child);
				key = key.endsWith("/")?key+child:key+"/"+child;
				uploadFile(bucketName,key,childFile);
			}
		}else {
			FileExecutor fileExecutor = OSSFactory.getFileExecutor(staticClient, upFile, bucketName, key, FileOperation.RESUMABLE_UPLOAD);
			synchronized(getClass()) {
				taskList.add(executor.submit(fileExecutor));
				totalTaskNum ++;
				logger.info("添加任务,当前任务数{}",totalTaskNum);
			}
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
		
		synchronized(getClass()) {
			taskList.add(executor.submit(fileExecutor));
			totalTaskNum ++;
			logger.info("添加任务,当前任务数{}",totalTaskNum);
		}
	}
	
	/**
	 * 用于下载文件夹的方法
	 * @param bucketName
	 * @param prefix	OOS服务器上的文件前缀,即某个文件的目录
	 * @param dir 		本地的文件夹名,以指定要存放的位置
	 */
	public void batchDownload(String bucketName,String prefix,File dir) {
		if(!dir.exists()) dir.mkdirs();
		
		
		ListObjectsRequest listRequest = new ListObjectsRequest();
		listRequest.setBucketName(bucketName);
		listRequest.setKey(prefix);
		listRequest.setMaxKeys(1000);
		
		ObjectListing objectListing = staticClient.listObjects(listRequest);
		
		List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
		for (OSSObjectSummary s : sums) {
			String key = s.getKey();
			File f = new File(dir,key.substring(prefix.length(),key.length()));
			if(!key.endsWith("/")) {
				downLoadFile(bucketName,s.getKey(),f);
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
		
		ListObjectsRequest listRequest = new ListObjectsRequest();
		listRequest.setBucketName(bucketName);
		listRequest.setKey(remotePrefix);
		listRequest.setMaxKeys(1000);
		
		ObjectListing listObjects = staticClient.listObjects(listRequest);
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
		
		Executor executorService = Executors.newCachedThreadPool();
		for (OSSObjectSummary summary : list) {
			if(flagNum % 10000 ==0) {
				List<RemoteDir> dirList = new ArrayList<RemoteDir>();
				List<RemoteFile> fileList = new ArrayList<RemoteFile>();
				dirOuterList.add(dirList);
				fileOuterList.add(fileList);
				
				//每隔10000次,启用另一个线程把远程文件和文件夹插入数据库中
				if(flagNum != 0) {
					executeInsert(dirOuterList, fileOuterList, executorService);
				}
			}
			FileAble f = getFile(summary);
			if(f instanceof RemoteDir) {
				dirOuterList.getLast().add((RemoteDir)f);
			}else if(f instanceof RemoteFile) {
				fileOuterList.getLast().add((RemoteFile)f);
			}
			if(flagNum==list.size()-1 && flagNum %10000!=0) {
				executeInsert(dirOuterList, fileOuterList, executorService);
			}
			flagNum ++;
			
		}
		
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		logger.info("插入任务全部完成!");
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
			remoteDir.setBucketName(summary.getBucketName());
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
			remoteFile.setDownloadState(0);
			remoteFile.setBucketName(summary.getBucketName());
			return remoteFile;
		}
	}
	
	public void insertSummaries(String bucketName,String key) {
		List<OSSObjectSummary> list = getSummary(bucketName,key);
		System.err.println(list);
		insertRemoteFiles(list);
	}
	
	
	
	/*****************	以下为测试方法,都放在最后面 *********************/
	
	/**
	 * 1.getAllSummaries
	 * 		该方法用于测试大量的文件列表的分页获取方法
	 *  测试得到的结果为:
	 *  	list当中有300条数据
	 *  		通过nextMarker的方式断点得到文件信息,会导致得到的ObjectSummary中有重复的信息
	 *  		因此,通过转换成TreeSet 来去掉所有重复的信息
	 */
	public void getAllSummaryies(String bucketName,String key) {

		List<OSSObjectSummary> list = new ArrayList<>();
		String nextMarker =  null;
		ObjectListing listObjects = null;
		do {
			
			ListObjectsRequest listRequest = new ListObjectsRequest();
			listRequest.setBucketName(bucketName);
			listRequest.setKey(key);
			listRequest.setMaxKeys(10);
			
			listObjects = staticClient.listObjects(listRequest.withMarker(nextMarker));
			list.addAll(listObjects.getObjectSummaries());
			nextMarker = listObjects.getNextMarker();
		}while(listObjects.isTruncated());
		
		Set<OSSObjectSummary> set = new TreeSet<>((OSSObjectSummary o1,OSSObjectSummary o2)->{
			int len = o1.getKey().length() - o2.getKey().length();
			if(len==0) {
				return o1.getKey().compareTo(o2.getKey());
			}else {
				return len;
			}
		});
		
		set.addAll(list);
		int flag = 0;
		for (OSSObjectSummary ossObjectSummary : set) {
			flag++;
			System.out.println(flag+"-->"+ossObjectSummary.getKey());
		}
	}
	
	
}
