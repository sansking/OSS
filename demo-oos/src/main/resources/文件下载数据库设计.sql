CREATE TABLE remote_dir(
	id INT COMMENT '远程目录id',
	dir_name VARCHAR(100) COMMENT'远程目录名,不计算bucketName',
	parent_id INT COMMENT'直接上级目录id',
	rank INT COMMENT'当前目录层级,从1开始',
	include_dir_num INT COMMENT'该目录的直接子目录数',
	include_file_num INT COMMENT'该目录的直接子文件数')
	COMMENT 'OSS服务器上的目录'
SHOW FULL COLUMNS FROM remote_dir;
DROP TABLE remote_dir;
CREATE TABLE remote_file(
	id INT COMMENT '远程文件id',
	file_full_name VARCHAR(100) COMMENT'远程文件全路径名,不计算bucketName',
	file_name VARCHAR(100) COMMENT'远程文件名,不含路径',
	parent_id INT COMMENT'直接上级目录id',
	size INT COMMENT'文件大小',
	`hash` VARCHAR(160) COMMENT'文件hash值',
	last_modify_time DATE COMMENT'最后修改时间')
	COMMENT 'OSS服务器上的文件'
CREATE TABLE local_dir(
	id INT COMMENT'本地文件夹id',
	full_name INT COMMENT'本地文件夹绝对路径',
	remote_id INT COMMENT'对应的远程目录的id',
	is_success INT COMMENT'是否创建成功,0为未创建,1为已创建,2为创建出现异常')
	COMMENT '该表实际上没有太大作用,只是为了在下载开始之前,先创建好相应的文件夹,以提高效率'

CREATE TABLE local_file(
	id INT COMMENT '本地文件id',
	file_full_name VARCHAR(100) COMMENT'文件全路径名',
	remote_id INT COMMENT'对应的远程文件的id',
	size INT COMMENT'文件大小',
	`hash` VARCHAR(160) COMMENT'文件的hash值',
	download_state INT COMMENT'下载状态,0为未下载,1为正在下载,2为下载完成,3为下载中断',
	start_time DATE COMMENT'下载开始时间',
	end_time DATE COMMENT'下载完成时间',local_dirlocal_dirlocal_file
	resume_file VARCHAR(100) COMMENT'断点文件全路径名')
	COMMENT '本地文件,用于保存下载的相关信息'
	
