package com.demo.oss.test;

import com.demos.oss.genfile.GenRandomFile;
import com.demos.oss.genfile.Language;
import com.demos.oss.genfile.RandomFileConfig;
import com.demos.oss.genfile.RandomFileConfig.Unit;

public class TestGenFile {
	public static void main(String[] args) {
		RandomFileConfig config = new RandomFileConfig();
		config.setFilePath("D:\\TestDir\\upfiles\\randomFile.txt");
		config.setMinLength(10);
		config.setMaxLength(30);
		config.setUnit(Unit.MB);
		config.setLang(Language.BOTH);
		GenRandomFile.genRandomFile(config);
	}
}
