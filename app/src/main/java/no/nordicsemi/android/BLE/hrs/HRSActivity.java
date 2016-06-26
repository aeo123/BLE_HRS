/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * 
 * The information contained herein is property of Nordic Semiconductor ASA.
 * Terms and conditions of usage are described in detail in NORDIC SEMICONDUCTOR STANDARD SOFTWARE LICENSE AGREEMENT.
 * Licensees are granted free, non-transferable use of the information. NO WARRANTY of ANY KIND is provided. 
 * This heading must NOT be removed from the file.
 ******************************************************************************/
package no.nordicsemi.android.BLE.hrs;

import java.util.Iterator;
import java.util.UUID;

import no.nordicsemi.android.BLE.R;
import no.nordicsemi.android.BLE.profile.BleManager;
import no.nordicsemi.android.BLE.profile.BleProfileActivity;

import org.achartengine.GraphicalView;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Queue;
import java.util.LinkedList;

/**
 * HRSActivity is the main Heart rate activity. It implements HRSManagerCallbacks to receive callbacks from HRSManager class. The activity supports portrait and landscape orientations. The activity
 * uses external library AChartEngine to show real time graph of HR values.
 */
public class HRSActivity extends BleProfileActivity implements HRSManagerCallbacks {
	@SuppressWarnings("unused")
	private final String TAG = "HRSActivity";

	private static final String GRAPH_STATUS = "graph_status";
	private static final String GRAPH_COUNTER = "graph_counter";
	private static final String HR_VALUE = "hr_value";

	private final int MAX_HR_VALUE = 1000;
	private final int MIN_POSITIVE_VALUE = 0;
	private final int MESSAGE_REC=0;

	private Handler mHandler = new Handler();
	private Handler dHandler;

	private boolean isGraphInProgress = false;

	private GraphicalView mGraphView;
	private LineGraphView mLineGraph;
	private TextView mHRSValue, mHRSPosition;

	private int mInterval = 1000; // 1s interval
	private int mHrmValue = 0;
	private int mCounter = 0;
	private double secend=0;
	private double volt=0;

	private int BPM;                   // used to hold the pulse rate
	private int IBI =100;             // holds the time between beats, must be seeded!
	private boolean Pulse = false;     // true when pulse wave is high, false when it's low
	private int MEASURED_COUNT=0;
	private int rate[]=new int[5];          // array to hold last ten IBI values
	private int sampleCounter = 0;          // used to determine pulse timing
	private int lastBeatTime = 0;           // used to find IBI
	private int PP =620;                      // used to find peak in pulse wave, seeded
	private int T = 620;                     // used to find trough in pulse wave, seeded
	private int thresh = 620;                // used to find instant moment of heart beat, seeded
	private int amp = 2048;                   // used to hold amplitude of pulse waveform, seeded
	private boolean firstBeat = true;        // used to seed rate array so we startup with reasonable BPM
	private boolean secondBeat = false;      // used to seed rate array so we startup with reasonable BPM
	private int gLength = 1500;				//一屏图显示的点数

	Queue<Double> XT = new LinkedList<Double>();
	Queue<Double> YT = new LinkedList<Double>();

//	private double[] XT_temp=new double[500];	//显示点缓存
//	private double[] YT_temp=new double[500];
//	private double[] XT=new double[1500];	//显示点缓存
//	private double[] YT=new double[1500];


	@Override
	protected void onCreateView(Bundle savedInstanceState) {
		setContentView(R.layout.activity_feature_hrs);
		setGUI();
		new HRSThread().start();
		restoreSavedState(savedInstanceState);
	}

	private void setGUI() {
		mLineGraph = LineGraphView.getLineGraphView();
		mHRSValue = (TextView) findViewById(R.id.text_hrs_value);
		//mHRSPosition = (TextView) findViewById(R.id.text_hrs_position);
		showGraph();
	}

	private void showGraph() {
		mGraphView = mLineGraph.getView(this);
		ViewGroup layout = (ViewGroup) findViewById(R.id.graph_hrs);
		layout.addView(mGraphView);
	}

	private void restoreSavedState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			isGraphInProgress = savedInstanceState.getBoolean(GRAPH_STATUS);
			mCounter = savedInstanceState.getInt(GRAPH_COUNTER);
			mHrmValue = savedInstanceState.getInt(HR_VALUE);
			if (isGraphInProgress) {
				startShowGraph();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(GRAPH_STATUS, isGraphInProgress);
		outState.putInt(GRAPH_COUNTER, mCounter);
		outState.putInt(HR_VALUE, mHrmValue);
		stopShowGraph();
	}

	@Override
	protected int getAboutTextId() {
		return R.string.hrs_about_text_my;
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.hrs_default_name;
	}

	@Override
	protected UUID getFilterUUID() {
		return HRSManager.HR_SERVICE_UUID;
	}


	private void updateGraph(final int hrmValue) {
				//更新缓存数据
				mLineGraph.clearGraph();
				int xcounter=0;
				Iterator YTit=YT.iterator();
				while(YTit.hasNext())
				{
					double XT=2*(xcounter++);
					double YT=(Double)YTit.next();
					mLineGraph.addValue(XT, YT);
				}
				mGraphView.repaint();

			}

	private Runnable mRepeatTask = new Runnable() {
		@Override
		public void run() {

			if (isGraphInProgress) {
				setHRSValueOnView(BPM);    //更新bmp标签值，1hz
				mHandler.postDelayed(mRepeatTask, mInterval);
			}
		}
	};

	void Process_RecData(int[] value){
		//一个数据包内10个数据，一次添加10个到缓存
		for(int i=0;i<value.length;i++){
			//secend=xcounter*2;			//转换成时间,
			volt =value[i]*3.3f/2048.0f;	//转换成电压
			if(YT.size()<gLength) {	//等待数据满
				//XT.offer(secend);		//加入新的数据
				YT.offer(volt);
			}else{
				//XT.poll();				//弹出队列头
				YT.poll();
				//XT.offer(secend);
				YT.offer(volt);
				mCounter++;
			}
			HeartComputer(value[i]);		//处理数据计算BPM
		}
		//刷新频率=n*2ms
		if(mCounter==300) {
			updateGraph(mHrmValue);
			mCounter=0;
		}
	}
	//单独的线程处理数据
	class HRSThread extends Thread {
		@Override
		public void run() {
			//建立消息循环
			Looper.prepare();								 //初始化Loop
			dHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {//定义处理消息的方法
					switch (msg.what) {
						case MESSAGE_REC:
							Process_RecData((int[])msg.obj);
						default: break;
					}
				}

			};

			Looper.loop();//启动消息循环
		}
	}


   private  void HeartComputer(int Signal){
	sampleCounter += 2;                            // keep track of the time in mS with this variable
	long N = sampleCounter - lastBeatTime;       // monitor the time since the last beat to avoid noise
	//  find the peak and trough of the pulse wave
	if(Signal < thresh && N > (IBI*0.5)){       // avoid dichrotic noise by waiting 3/5 of last IBI
		if (Signal < T){                        // T is the trough
			T = Signal;                         // keep track of lowest point in pulse wave
		}
	}
	if(Signal > thresh && Signal > PP){          // thresh condition helps avoid noise
		PP = Signal;                             // P is the peak
	}                                        // keep track of highest point in pulse wave
	//  NOW IT'S TIME TO LOOK FOR THE HEART BEAT
	// signal surges up in value every time there is a pulse
	if (N > 50){                                   // avoid high frequency noise
		if ( (Signal >= thresh) && (Pulse == false) && (N > (IBI*0.5)) ){
			Pulse = true;                               // set the Pulse flag when we think there is a pulse
			IBI = sampleCounter - lastBeatTime;         // measure time between beats in mS
			lastBeatTime = sampleCounter;               // keep track of time for next pulse
			if(secondBeat){                        	// if this is the second beat, if secondBeat == TRUE
				secondBeat = false;                  // clear secondBeat flag
				rate[MEASURED_COUNT] = IBI;
				MEASURED_COUNT++;

			}
			if(firstBeat){                         // if it's the first time we found a beat, if firstBeat == TRUE
				firstBeat = false;                   // clear firstBeat flag
				secondBeat = true;                   // set the second beat flag
				                                   // IBI value is unreliable so discard it
			}
			if(MEASURED_COUNT==5) {
				int runningTotal = 0;                  // clear the runningTotal variable
				int i;
				for (i = 0; i < MEASURED_COUNT; i++) {                // shift data in the rate array
					runningTotal += rate[i];                				// add up the 9 oldest IBI values
				}
				runningTotal /= MEASURED_COUNT;                     // average the last 10 IBI values
				BPM = 60000 / runningTotal;               // how many beats can fit into a minute? that's BPM!
				MEASURED_COUNT=0;
			}
		}
	}
	if (Signal <= thresh && Pulse == true){   // when the values are going down, the beat is over
		   Pulse = false;                         // reset the Pulse flag so we can do it again
		   amp = PP - T;                           // get amplitude of the pulse wave
		   thresh = amp/2 + T;                    // set thresh at 50% of the amplitude
		   PP = thresh;                            // reset these for next time
		   T = thresh;
		   firstBeat = true;
	   }
	if (N > 2500){                              // if 2.5 seconds go by without a beat
		thresh = 620;                          // set thresh default
		PP = 620;                               // set P default
		T = 620;                               // set T default
		lastBeatTime = sampleCounter;          // bring the lastBeatTime up to date
		firstBeat = true;                      // set these to avoid noise
		secondBeat = false;                    // when we get the heartbeat back
		}

	}



	void startShowGraph() {
		isGraphInProgress = true;
		mRepeatTask.run();
	}

	void stopShowGraph() {
		isGraphInProgress = false;
		mHandler.removeCallbacks(mRepeatTask);
	}

	@Override
	protected BleManager<HRSManagerCallbacks> initializeManager() {
		HRSManager manager = HRSManager.getHRSManager();
		manager.setGattCallbacks(this);
		return manager;
	}

	private void setHRSValueOnView(final int value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (value >= MIN_POSITIVE_VALUE && value <= MAX_HR_VALUE) {
					mHRSValue.setText(Integer.toString(value));

				} else {
					mHRSValue.setText(R.string.not_available_value);
				}
			}
		});
	}


	@Override
	public void onServicesDiscovered(boolean optionalServicesFound) {
		// this may notify user or show some views
	}

	//@Override
	public void onHRNotificationEnabled() {
		startShowGraph();
	}

	@Override
	public void onHRValueReceived(int[] value) {
		//收到数据发送到处理线程
		Message msg= new Message();
		msg.obj=value;
		msg.arg1=value.length;
		msg.what = MESSAGE_REC;
		dHandler.sendMessage(msg);
	}

	@Override
	public void onDeviceDisconnected() {
		super.onDeviceDisconnected();
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				stopShowGraph();
			}
		});
	}

	@Override
	protected void setDefaultUI() {
		mHRSValue.setText(R.string.not_available_value);
		clearGraph();
	}

	private void clearGraph() {
		mLineGraph.clearGraph();
		mGraphView.repaint();
		mCounter = 0;
		mHrmValue = 0;
	}
}
