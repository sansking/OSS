package com.cnns.oss.test;

import java.io.File;

import com.cnns.oss.randomfile.GenRandomFile;

public class TestGenFile {
	public static void main(String[] args) {
		/*
		RandomFileConfig config = new RandomFileConfig();
		config.setFilePath("D:\\TestDir\\upfiles\\randomFile.txt");
		config.setMinLength(10);
		config.setMaxLength(30);
		config.setUnit(Unit.MB);
		config.setLang(Language.BOTH);
		GenRandomFile.genRandomFile(config);
		*/
		GenRandomFile.repeatFile(
				new File("D:\\TestDir\\upfiles\\randomFile.txt"),10);
	}
}
