package com.cnns.oss.util;

import java.io.File;

import com.aliyun.oss.OSSClient;

/**
 * 用于保存文件上传下载基本信息的类
 * @author wangp
 *
 */
public class OSSFileInfo{
	//便于本包下的类进行操作,不设置为private
	File localFile;		//由于要保证与destFile对应,因此,localFile必须为一个文件;并且当操作为上传时,该文件还必须存在
	String bucketName;	
	String destFile;	//一个OOS服务器上的文件路径
	OSSClient client;	//用于操作服务器的client对象
	FileOperation operation;	//用于标志是上传还是下载文件的一个枚举对象
	
	/**
	 * 以上是进行文件的上传和下载必要的信息,因此使用构造方法注入上述信息
	 */
	public OSSFileInfo(OSSClient client,File localFile,String bucketName,String destFile,FileOperation operation) {
		
		this.client = client;
		this.bucketName = bucketName;
		this.destFile = destFile;
		this.localFile = localFile;
		this.operation  = operation;
	};
	
	
}