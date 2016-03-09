/**
 * @author wuyan
 * 专门录音对象
	－1-获取pcm数据
	－2.分发pcm数据
	－3.pcmToaac
	－4.分发aac数据
 */
package com.example.wuyan.testaudiorecord;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class AudioRecordHelper {
	public static final AudioRecordHelper  mAudioRecordHelper = new AudioRecordHelper();
    private static int RECORD_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
  //private static int RECORD_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    public Queue<byte[]> queue = new LinkedList<byte[]>();
    public Semaphore stopSemaphore = new Semaphore(1);
    public static int TYPE_PCM = 1;
    public static int TYPE_AAC = 2;
    public interface recordDataRecevicer {
    	void recevice(EventForRecord data);
    }
    private Object recevicerStack = new Object();
    private volatile boolean  isRun = false;

    public static int Simple = 16000;
    private AudioEncoder encoder;
    public static final List<recordDataRecevicer> pcmRecevicers = new ArrayList<recordDataRecevicer>();
    public static final List<recordDataRecevicer> aacRecevicers = new ArrayList<recordDataRecevicer>();
    
    
    private Audio audio = new Audio();
    //jni实现的pitch分析器
    private int cAnalyzerId = 0;
 
    public AudioRecordHelper() {
    	
	}
    public static AudioRecordHelper getInstance(){
		return mAudioRecordHelper;
	}
    public void initAudioRecorder(int Simple){

    	this.Simple = Simple;
        Log.i("AudioRecordHelper", "initAudioRecorder Simple"+this.Simple);
        audio.input = AudioSource.DEFAULT;
        audio.initAudioRecorder();
    }

    public void initAudioRecorder(){
        this.Simple = Simple;
        Log.i("AudioRecordHelper", "initAudioRecorder Simple"+this.Simple);
        audio.input = AudioSource.DEFAULT;
        audio.initAudioRecorder();
        RecordFileWirter.getInstance().setCurrSongSaveName("wuyantest.pcm",1);

    }

    public synchronized void start(){
        if(!isRun){
            Log.i("AudioRecordHelper", "start()");
            isRun = true;

            audio.start();
        }else{
            throw new RuntimeException("AudioRecordHelper has started");
        }

    }

    public synchronized void stop(){
        if(isRun){
            try {
                LogUtil.i("call stop() time"+System.currentTimeMillis());
                audio.stop();
                stopSemaphore.acquire();
                LogUtil.i("real  stop() time 02  "+System.currentTimeMillis());
                stopSemaphore.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            LogUtil.i("AudioRecordHelper is not run");
        }

    }

    protected class Audio implements Runnable
    {
        // Preferences
    	private int inputSample;
    	private int outPutSize ;
    	private long aacEncoderId ;
    	
        protected int input;
        protected boolean filter;
        protected boolean downsample;

        protected double reference;

        private int bufferSizeInShort = -1;
        // Data
        protected Thread thread;
        private  volatile  boolean isStop = false;
        protected double frequency;
        protected int count;
        protected int note;

        private AudioRecord audioRecord;

        private static final int MAXIMA = 8;
        private static final int OVERSAMPLE = 16;
        private static final int SAMPLES = 16384;
        private static final int RANGE = SAMPLES * 3 / 8;
        private static final int STEP = SAMPLES / OVERSAMPLE;
        private static final int SIZE = 4096;

        // Constructor

        protected Audio()
        {

        }

        // Start audio
        protected void start()
        {
            // Start the thread
            thread = new Thread(this, "Audio");
            isStop = false;
            thread.start();
        }

        // Run
        @Override
        public void run()
        {
         processAudio();

        }

        // Stop
        protected void stop()
        {
        	LogUtil.i("AudioRecordHelper.....stop");
            thread.interrupt();
        	thread = null;
            isStop = true;
        }
        
        public void initAudioRecorder(){
//        	aacEncoderId =	AacEncoder.getInstance().encodeInit(Simple, 2,16);
//        	inputSample = AacEncoder.getInputSample(aacEncoderId);
//    		outPutSize =  AacEncoder.getMaxOutputBytes(aacEncoderId);
            encoder = new AudioEncoder();
      		bufferSizeInShort = 	 AudioRecord.getMinBufferSize(Simple, RECORD_CHANNEL,  AudioFormat.ENCODING_PCM_16BIT);
            Log.i("bufferSizeInShort", "bufferSizeInShort"+bufferSizeInShort);
        	if(bufferSizeInShort==-1){
        		Log.i("", "");
        		return;
        	}
        	if(bufferSizeInShort<=SIZE){
        		bufferSizeInShort =SIZE;
        	}
    		audioRecord = new AudioRecord(input, Simple, RECORD_CHANNEL,
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSizeInShort * 5);
            // Check state
            int state = audioRecord.getState();
            if (state != AudioRecord.STATE_INITIALIZED) {
                audioRecord.release();
                thread = null;
                LogUtil.w("state != AudioRecord.STATE_INITIALIZED , set thread null");
                return;
            }
        }

        // Process Audio
        protected void processAudio()
        {
            try {
                stopSemaphore.acquire();

            long base = System.currentTimeMillis();
            LogUtil.i( "[processAudio]enter processAudio time=>" + base);
            

            byte[] byteBuffer = new byte[SIZE];
            // Continue until the thread is stopped
	    	int size = 0;

            if(audioRecord!=null){
                audioRecord.startRecording();
            }

	    	LogUtil.i("before while time=>" + System.currentTimeMillis());
            while (!isStop)
            {
           // 	Log.i("Read a buffer of data", "Read a buffer of data");
                // Read a buffer of data
            	if (audioRecord == null) {
					LogUtil.e("audiorecord is null break");
					break;
				}
            	
            	if (byteBuffer == null) {
            		LogUtil.e("byteBuffer is null");
				}

                size = audioRecord.read(byteBuffer, 0, SIZE);



                LogUtil.i("audioRecord. read  over size"+size);
                // Stop the thread if no data
                
                if (size == 0)
                {
                	thread = null;
                	LogUtil.w("size == 0, set thread null");
                	break;
                }

                byte[] temp = byteBuffer.clone();

                //分发pcm数据
                sendPcmData(byteBuffer);


				byte[] out = encoder.offerEncoder(temp);
//						//LogUtil.i("in"+in.length+"out"+out.length);
//				int leng= AacEncoder.getInstance().aacEncode(temp, inputSample*2,out, aacEncoderId);
//
				if(out!=null&&out.length!=0){
					//分发AAC数据
					sendAACData(out, out.length);
				}

            }
            } catch (InterruptedException e) {
                LogUtil.i("InterruptedException      InterruptedException");
            }finally {
                if (audioRecord != null)
                {

                    LogUtil.i("will call audioRecord.stop() and audioRecord.release()");
                    audioRecord.stop();
                    audioRecord.release();
                    audioRecord = null;
                }else {
                    LogUtil.w("try stop audioRecord, but it is null");
                }

                encoder.close();
                isRun = false;
                stopSemaphore.release();
                LogUtil.i("real  stop() time 01 "+System.currentTimeMillis());
            }

        }

}
    
    public  void addPcmRecevicer(recordDataRecevicer recevicer){

        synchronized (recevicerStack){
            if(pcmRecevicers.contains(recevicer)){
                return;
            }
            pcmRecevicers.add(recevicer);
        }
    }
    
    
    public  void removePcmRecevicer(recordDataRecevicer recevicer){
        synchronized (recevicerStack){
            if(pcmRecevicers.contains(recevicer)){
                pcmRecevicers.remove(recevicer);
            }
        }

    }
    
    public  void addAACRecevicer(recordDataRecevicer recevicer){
        synchronized (recevicerStack){
            if(aacRecevicers.contains(recevicer)){
                return;
            }
            aacRecevicers.add(recevicer);
        }

    }
    
    
    public  void removeAACmRecevicer(recordDataRecevicer recevicer){
        synchronized (recevicerStack){
            if(aacRecevicers.contains(recevicer)){
                aacRecevicers.remove(recevicer);
            }
        }

    }

    public  void sendPcmData(byte[] byteBuffer){
        synchronized (recevicerStack){
            for(recordDataRecevicer recevicer:pcmRecevicers){
                recevicer.recevice(new EventForRecord(TYPE_PCM, byteBuffer.length, byteBuffer));
            }
        }

    }
    
    public  void sendAACData(byte[] out,int leng){
        synchronized (recevicerStack){
            //分发AAC数据
            for(recordDataRecevicer recevicer:aacRecevicers){
                recevicer.recevice(new EventForRecord(TYPE_AAC, leng, out));
            }
        }
    }
}

	
