package com.cnns.oss.randomfile;

public enum Language{
	CHINESE(1),ENGLISH(2),BOTH(0);
	private int langFlag;
	public String getLang() {
		return langFlag==1?"Chinese":
			langFlag==2?"English":
				langFlag==0?"Chinese and English"
						:"Illegal Language!";
	}
	Language(int langFlag){
		this.langFlag = langFlag;
	}
}