/**
 * @author wuyan
 * 
 * 为event bus  传输使用
 */
package com.example.wuyan.testaudiorecord;


public class EventForRecord {
	public int dataType;
	public int dataLength;
	public byte[] data;
	public EventForRecord(int dataType,int dataLength,byte[] data){
		this.dataType = dataType;
		this.dataLength = dataLength;
		this.data = data;
	}
}
