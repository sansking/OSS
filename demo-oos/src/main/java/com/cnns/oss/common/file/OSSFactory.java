package com.cnns.oss.common.file;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.aliyun.oss.OSSClient;
import com.cnns.oss.common.enumeration.FileOperation;
/**
 * 该类用于提供动态或静态的OssClient
 * @author wangp
 *
 */
@Component
public class OSSFactory {
	
	
	private OSSFactory() {}
	/*
	 * 用于产生OSSClient的各种参数
	 */
	static private String accessKeyId;		// = "LTAIrqwsxxdfR2Um";
	static private String accessKeySecret;	// = "pejfPWkUGreCIqm7yUnnlEBI40DbXi";
	static private String endpoint;			// = "http://oos-cn-shanghai.aliyuncs.com";
	
	@Value("${aliyun.accessKeyId}")
	public void setAccessKeyId(String accessKeyId) {
		OSSFactory.accessKeyId = accessKeyId;
	}
	@Value("${aliyun.accessKeySecret}")
	public void setAccessKeySecret(String accessKeySecret) {
		OSSFactory.accessKeySecret = accessKeySecret;
	}
	@Value("${aliyun.endpoint}")
	public void setEndpoint(String endpoint) {
		OSSFactory.endpoint = endpoint;
	}
	
	static private OSSClient staticOssClient;
	/**
	 * 可以 通过service得到OSSClient可以让不同的任务使用不同的客户端
	 * 由于OSSClient具有shutdown方法关闭自身,因此无需提供额外的关闭方法
	 * @return 一个新的OSSClient
	 */
	public OSSClient getOSSClient() {
		if(accessKeyId==null) throw new RuntimeException("accessKey值为null");
		return new OSSClient(endpoint,accessKeyId,accessKeySecret);
	}
	
	/**
	 * 提供返回静态的OSSClient的方法,为避免多次创建,使用synchronized锁定创建过程
	 * @return
	 */
	public synchronized OSSClient getStaticOssClient() {
		if(staticOssClient==null) {
			staticOssClient = new OSSClient(endpoint,accessKeyId,accessKeySecret);
		}
		return staticOssClient;
	}
	
	/**
	 * 该方法用于生成 FileExecutor 以便于在多线程的情况下,提高上传下载效率
	 * @author wangp
	 *
	 */
	public static FileExecutor getFileExecutor(OSSClient client,File localFile,
			String bucketName,String destFile,FileOperation operation) {
		OSSFileInfo fileInfo = new OSSFileInfo(client,localFile,bucketName,destFile, operation);
		return new FileExecutor(fileInfo);
	}
	
}
