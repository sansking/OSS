package com.demos.oss.genfile;

/**
 * 该类用于对随机生成的文件进行一些配置
 * @author wangp
 *
 */
public class RandomFileConfig {

	//注:由于1<<40大于了long的最大值,因此TB无法加入在此枚举中
	public enum Unit{
		BYTE(1),KB(1<<10),MB(1<<20),GB(1<<30);
		private long rate;
		public long getRate() {
			return rate;
		}
		Unit(long rate) {
			this.rate = rate;
		}
	}
	
	private Integer minLength;
	private Integer maxLength;
	private Unit unit = Unit.KB;
	private Language lang = Language.ENGLISH;
	private String filePath;
	private String charset = "utf-8";
	
	/**
	 * 判断一个FileConfig对象中,是否有重要的参数没有设置
	 * @return
	 */
	public boolean isEmpty() {
		if(minLength==null || maxLength==null || lang==null||filePath==null)
			return false;
		return true;
	}
	
	public Integer getMinLength() {
		return minLength;
	}
	public void setMinLength(Integer minLength) {
		this.minLength = minLength;
	}
	public Integer getMaxLength() {
		return maxLength;
	}
	public void setMaxLength(Integer maxLength) {
		this.maxLength = maxLength;
	}
	public Unit getUnit() {
		return unit;
	}
	public void setUnit(Unit unit) {
		this.unit = unit;
	}
	public Language getLang() {
		return lang;
	}
	public void setLang(Language lang) {
		this.lang = lang;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getCharset() {
		return charset;
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}
	
	
	public RandomFileConfig(Integer minLength, Integer maxLength, Unit unit, Language lang, String filePath, String charset) {
		super();
		this.minLength = minLength;
		this.maxLength = maxLength;
		this.unit = unit;
		this.lang = lang;
		this.filePath = filePath;
		this.charset = charset;
	}
	public RandomFileConfig() {
		super();
		// TODO Auto-generated constructor stub
	}
	@Override
	public String toString() {
		return "RandomFileConfig [minLength=" + minLength + ", maxLength=" + maxLength + ", unit=" + unit + ", lang="
				+ lang + ", filePath=" + filePath + ", charset=" + charset + "]";
	}
	
}
