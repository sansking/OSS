package com.cnns.oss.util;

//用于标志上传下载的枚举类
public enum FileOperation{
	SIMPLE_UPLOAD,	//简单上传
	FORM_UPLOAD,	//通过HTML的表单进行文件上传
	APPEND_UPLOAD,	//在原有文件后插入的上传
	PART_UPLOAD,	//分片上传,将大文件拆分为几个部分上传,然后再合并
	RESUMABLE_UPLOAD,	//断点续传形式的上传
	
	STREAM_DOWNLOAD,	//流式下载,得到的是IO流
	SIMPLE_DOWNLOAD,	//普通的文件下载
	RANGE_DOWNLOAD,		//范围下载,下载某个文件的某些字节
	CONDITION_DOWNLOAD,	//条件下载,下载满足条件的某些文件
	RESUMABLE_DOWNLOAD	//断点续传下载
}
