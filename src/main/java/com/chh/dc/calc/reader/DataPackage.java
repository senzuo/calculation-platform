package com.chh.dc.calc.reader;

/**
 * @ClassName: DataPacage
 * @since 1.0
 * @version 1.0
 * @author Niow
 * @date: 2016-6-27
 */
public class DataPackage {

	/**
	 * 数据源类型
	 */
	private int type;

	/** 数据源对象 */
	private Object data;

	/** 数据源描述信息 */
	private String desc;

	public DataPackage(){}

	public DataPackage(Object data){
		this.data = data;
	}

	public DataPackage(Object data, String desc){
		this.data = data;
		this.desc = desc;
	}

	public void setData(Object data){
		this.data = data;
	}

	public Object getData(){
		return this.data;
	}

	/**
	 * @return the desc
	 */
	public String getDesc(){
		return desc;
	}

	/**
	 * @param desc the desc to set
	 */
	public void setDesc(String desc){
		this.desc = desc;
	}



}
