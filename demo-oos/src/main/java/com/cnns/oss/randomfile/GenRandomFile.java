package com.cnns.oss.randomfile;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GenRandomFile {

	private static List<Character> englishChars;
	private static List<Character> chineseChars;
	private static Random random = new Random();
	//使用静态代码块初始化englishChars和chineseChars
	static{
		englishChars = new ArrayList<>();
		for(char c='A';c<'Z';c++) {
			englishChars.add(c);
		}
		for(char c='a';c<'z';c++) {
			englishChars.add(c);
		}
		englishChars.add('-');
		englishChars.add('_');
		englishChars.add(',');
		englishChars.add('.');
		englishChars.add('\'');
		englishChars.add('"');
		
		chineseChars = new ArrayList<>();
		for(char c='\u4e00';c<'\u9fa5';c++) {
			chineseChars.add(c);
		}
	}
	/**
	 * 生成一个指定长度的,指定文本类型,指定字符集的文件,使用随机字符填充
	 * @param config
	 */
	public static void genRandomFile(RandomFileConfig config){
		List<Character> charList = getCharList(config.getLang());
		long minLength = config.getMinLength() *  config.getUnit().getRate();
		long maxLength  = config.getMaxLength() * config.getUnit().getRate();
		ByteBuffer minBuffer = getByteBuffer(charList, minLength,minLength);
		ByteBuffer extendBuffer = getByteBuffer(charList,0,maxLength-minLength);
		File file = new File(config.getFilePath());
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(file,false));
			bos.write(minBuffer.array());
			bos.write(extendBuffer.array());
			bos.flush();
		}catch(IOException e) {
			throw new RuntimeException(e);
		}finally {
			try {
				if(bos!=null)bos.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				bos = null;
			}
		}
	}

	

	//由于1个字符占用两个字节,需要保证字节个数为偶数
	private static long ensureEven(long length) {
		if(length<0) return 0;
		return length%2==0?length:length-1;
	}
	/**
	 * 通过字符集合(charList),得到对应的随机字节数组
	 * @param charList
	 * @param length
	 * @return
	 */
	private static ByteBuffer getByteBuffer(List<Character> charList, long minLength,long maxLength) {
		if(minLength > maxLength) throw new RuntimeException("最小长度超过最大长度");
		long length;
		if(minLength == maxLength) length = minLength;
		else {
			long temp = random.nextLong();
			length = minLength + temp > maxLength-minLength ? maxLength-minLength : temp;
		}
		length = ensureEven(length);
		if(length>Integer.MAX_VALUE)
			throw new RuntimeException("文件过大");
		ByteBuffer buffer = ByteBuffer.allocate((int)length);
		char c;
		for(int i=0;i<length/2;i++) {
			c = charList.get(random.nextInt(charList.size()-1));
			buffer.putChar(c);
		}
		return buffer;
	}
	

	/**
	 * 通过语言参数得到字符集的范围
	 * @param lang
	 */
	private static List<Character> getCharList(Language lang) {
		List<Character> charList = new ArrayList<>();
		switch(lang) {
		case CHINESE:
			charList.addAll(chineseChars);
			break;
		case ENGLISH:
			charList.addAll(englishChars);
			break;
		case BOTH:
			charList.addAll(chineseChars);
			charList.addAll(englishChars);
			break;
		default:
			throw new RuntimeException("选择的参数有误");
		}
		return charList;
	}
	
	
	/**
	 * 翻倍一个文件的大小
	 * @param fileName
	 */
	public static void duplicateFile(String fileName) {
		duplicateFile(new File(fileName));
	}
	public static void duplicateFile(File file) {
		repeatFile(file,2);
	}
	/**
	 * 用于对一个文件进行重复的方法,主要用了测试
	 * 传入的次数应该从2开始,2代表翻倍
	 * @param file
	 * @param times
	 */
	public static void repeatFile(File file,int times) {
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		
		try {
			bis = new BufferedInputStream(new FileInputStream(file));
			byte[] buffer = new byte[bis.available()];
			bis.read(buffer);
			try {
				bis.close();
			}catch(IOException e) {
				throw new RuntimeException(e);
			}finally {
				bis = null;
			}
			bos = new BufferedOutputStream(new FileOutputStream(file,true));
			for(int i=2;i<times;i++) {
				bos.write(buffer);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}finally {
			try {
				bos.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}finally {
				bos = null;
			}
		}
		
	}
	
	public static void main(String[] args) {
		/*for(char c='A';c<'z';c++) {
			System.out.println("char:"+c+";int:"+(int)c+";hex:"+Integer.toHexString(c));
		}*/
	}
	
}
