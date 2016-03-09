/**
 * @author wuyan
 * 专门用于写录音文件的对象
 * 
 * */

package com.example.wuyan.testaudiorecord;


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class RecordFileWirter   implements AudioRecordHelper.recordDataRecevicer {
	private int mRecordFileType = 1;
	private FileOutputStream fos = null;  
	private	DataOutputStream dos = null;
	private File currFile;
	private String currSongSaveName;
	private static RecordFileWirter wirter = new RecordFileWirter();

	public static RecordFileWirter getInstance() {
		return wirter;
	}
	public synchronized void start(){

		try {
			File pcmFile = getRecSongFilepath();
			currFile = pcmFile;
			if (pcmFile.exists()) {
				pcmFile.delete();
			}
			LogUtil.i("will create record file");
			fos = new FileOutputStream(pcmFile);
			dos = new DataOutputStream(fos);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if(mRecordFileType== 2){
			AudioRecordHelper.getInstance().addAACRecevicer(this);
		}else{
			AudioRecordHelper.getInstance().addPcmRecevicer(this);
		}

	}
	private File getRecSongFilepath() {
		File pcmFile = new File("/sdcard/"+ currSongSaveName);
		LogUtil.i("record song path ==>" + pcmFile.getAbsolutePath());
		return pcmFile;
	}

	public void setCurrSongSaveName(String currSongSaveName,int mRecordFileType) {
		this.currSongSaveName = currSongSaveName;
		this.mRecordFileType = mRecordFileType;
		start();
	}
	public synchronized void stop(){
		AudioRecordHelper.getInstance().removeAACmRecevicer(this);
		AudioRecordHelper.getInstance().removePcmRecevicer(this);
		if(fos!=null){
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			fos =null;
		}
		if(dos!=null){
			try {
				dos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			dos = null;
		}
	}
	public int getmRecordFileType() {
		return mRecordFileType;
	}
	public void setmRecordFileType(int mRecordFileType) {
		this.mRecordFileType = mRecordFileType;
	}
	
	@Override
	public void recevice(EventForRecord data) {
		try {
			dos.write(data.data,0,data.dataLength);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
