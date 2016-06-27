/*******************************************************************************
 * Copyright (c) 2013 Nordic Semiconductor. All Rights Reserved.
 * <p/>
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
    private final int MESSAGE_REC = 0;

    private Handler mHandler = new Handler();
    private Handler dHandler;

    private boolean isGraphInProgress = false;

    private GraphicalView mGraphView;
    private LineGraphView mLineGraph;
    private TextView mHRSValue, mHRSPosition;

    private int mInterval = 1000; // 1s interval
    private int mHrmValue = 0;
    private int mCounter = 0;
    private double secend = 0;
    private double volt = 0;

    int BPM = 60;                           //心率数
    long IBI = 100;                        //得到的一次心跳时间间隔
    boolean Pulse = false;                    //找到一个脉动的标志
    float rate[]=new float[5];                  //保存多次心跳数处理
    long sampleCounter = 0;        // 时间计数器
    long lastBeatTime = 0;         // 找到上次脉动时间，求差
    int Peak = 620;               // 峰值
    int Nadir = 620;             // 峰底
    int thresh = 620;            // 单次阈值
    int amp = 2048;               // 峰峰值
    boolean firstBeat = true;         // 第一次上升沿
    boolean secondBeat = false;       // 和第一次跳求差得到一次跳动时间


    private int gLength = 800;                //一屏图显示的点数
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
        int xcounter = 0;
        Iterator YTit = YT.iterator();
        while (YTit.hasNext()) {
            double XT = 2 * (xcounter++);
            double YT = (Double) YTit.next();
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

    void Process_RecData(int[] value) {
        //一个数据包内10个数据，一次添加10个到缓存
        for (int i = 0; i < value.length; i++) {
            mCounter++;
            volt = value[i] * 3.3f / 2048.0f;    //转换成电压
            if (i % 2 == 0) {                   //二分频
                if (YT.size() < gLength) {        //等待数据满
                    //XT.offer(secend);		     //加入新的数据
                    YT.offer(volt);
                } else {
                    //XT.poll();				//弹出队列头
                    YT.poll();
                    //XT.offer(secend);
                    YT.offer(volt);

                }
            }

            HeartComputer(value[i]);        //处理数据计算BPM
        }
        //刷新频率=n*2ms
        if (mCounter == 40) {
            updateGraph(mHrmValue);
            mCounter = 0;
        }
    }

    //单独的线程处理数据
    class HRSThread extends Thread {
        @Override
        public void run() {
            //建立消息循环
            Looper.prepare();                                 //初始化Loop
            dHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {//定义处理消息的方法
                    switch (msg.what) {
                        case MESSAGE_REC:
                            Process_RecData((int[]) msg.obj);
                        default:
                            break;
                    }
                }

            };

            Looper.loop();//启动消息循环
        }
    }

    static int lastSignal[]=new int[10];           //保存之前的5场信号
    void HeartComputer(int Signal) {
        long N;
        int i;

        sampleCounter += 2;                                                   // 500hz采样率，一次2ms
        N = sampleCounter - lastBeatTime;                                      // 此次信号距离上次找到的脉冲时间差
        if (Signal < thresh && Signal < Nadir && Pulse == false) {
            Nadir = Signal;                                                  //更新当前峰底
        }

        if (Signal > thresh && Signal > Peak && Pulse == true) {                                 // // 峰值
            Peak = Signal;
        }
        //  心率信号0.78-3.33hz  ->1.28s-0.3s

        if (N > 250) {                                                                                          // 时间间隔滤波,至少200ms
            if ((Signal >= thresh) && (Signal - lastSignal[6]) > 0 && (Pulse == false) && (N > (IBI * 0.7))) {          //在上升沿捕获脉冲
                Pulse = true;
                IBI = sampleCounter - lastBeatTime;                                   // 得到两次上升沿时间差，就是周期
                lastBeatTime = sampleCounter;                                         // 更新时间
                if (secondBeat) {                                                        //
                    for (i = 4; i > 0; i--)                                                //fifo
                        rate[i] = rate[i - 1];

                    rate[0] = (float) 60000 / (float) IBI;
                }
                if (firstBeat) {                                                        //第一次进入作为搜寻的起点。直到一次丢失脉冲后再次进入first，否则一直是scend
                    firstBeat = false;
                    secondBeat = true;
                }
                if (rate[4]!=0)
                    BPM = (int)(0.4 * rate[0] + 0.3 * rate[1] + 0.2 * rate[2] + 0.1 * rate[3]);       //滑动滤波
                else
                    BPM = (int)rate[0];                                                 //刚开机的时候可以快速跟踪结果
            }
        }
        if (Signal < thresh && (Signal - lastSignal[6]) < 0 && Pulse == true) {                                    //找到脉冲后在下降沿更新参数
            Pulse = false;                               // 清楚脉冲标志
            amp = Peak - Nadir;                          // 峰峰值
            thresh = amp / 2 + Nadir;                      // 阈值
            Peak = thresh;                               // 峰值
            Nadir = thresh;                              //逢底
        }
        if (N > 1500) {                                                            // 2.5s找不到脉冲就丢失了信号。可能阈值不在范围
            thresh = 620;                              // 根据滑块来手动设置阈值
            Peak = 620;                               // 根据滑块来手动设置阈值
            Nadir = 620;                               // 根据滑块来手动设置阈值
            firstBeat = true;                                                       // 丢线后进入找第一个脉冲
            secondBeat = false;                    //
            lastBeatTime = sampleCounter;
        }
        for (i = 9; i > 0; i--)                      //fifo
            lastSignal[i] = lastSignal[i - 1];
        lastSignal[0] = Signal;
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
        Message msg = new Message();
        msg.obj = value;
        msg.arg1 = value.length;
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
