package no.nordicsemi.android.BLE.profile;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

import no.nordicsemi.android.BLE.R;
import zhou.tools.fileselector.FileSelectorDialog;
import zhou.tools.fileselector.config.FileConfig;

/**
 * Created by aeo on 2016/11/28.
 */
public class ReadActivity extends Activity {
    // Debugging
    private static final String TAG = "READActivity";
    private static final boolean D = true;

    private FileConfig fileConfig;

    EditText EDT_time;
    EditText EDT_path;
    static String PATH;                                //完整路径
    String FILENAME = "SaveData";             //文件名
    String FILEEND = ".csv";                  //文件后缀
    String DIR = "BLE_DATA";                //文件夹名，也可以不要文件夹
    ArrayList<String> result;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.activity_read);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        fileConfig = new FileConfig();

        EDT_path = (EditText) findViewById(R.id.input_path);

        DIR=getSDPath() + File.separator +DIR;
        PATH =  DIR + File.separator + FILENAME + FILEEND;
        EDT_path.setText(PATH);

        Button Bt_read = (Button) findViewById(R.id.bt_read);
        Button Bt_choose = (Button) findViewById(R.id.bt_choose);
        Bt_read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PATH = EDT_path.getText().toString();
                result = read();
                if (result.size()>100){
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();

                    //bundle.putString("username",PATH);
                   // bundle.putSerializable("sdData", result);
                    intent.putStringArrayListExtra("sdData", result);
                    //intent.putExtra("sdData",result.toArray());
                    // Set result and finish this Activity
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }else
                    result=new ArrayList<String>();
            }
        });
        Bt_choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileSelectorDialog fileDialog = new FileSelectorDialog();
                fileDialog.setOnSelectFinish(new FileSelectorDialog.OnSelectFinish() {
                    @Override
                    public void onSelectFinish(ArrayList<String> paths) {
                        PATH =  paths.get(0).toString();
                        EDT_path.setText(PATH);
                        Toast.makeText(getApplicationContext(), paths.get(0).toString(), Toast.LENGTH_SHORT).show();
                    }
                });
                Bundle bundle = new Bundle();
                fileConfig.multiModel=false;
                fileConfig.startPath=DIR;
                bundle.putSerializable(FileConfig.FILE_CONFIG, fileConfig);
                fileDialog.setArguments(bundle);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                fileDialog.show(ft, "fileDialog");
            }
        });

    }




    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");


    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");



    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

    //返回键
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            this.finish();
            return true;
        }
        return false;
    }


    public String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState()
                .equals(android.os.Environment.MEDIA_MOUNTED);//判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();//获取跟目录
        } else { // SDCard不存在，使用Toast提示用户
            Toast.makeText(this, "打开路径失败，SD卡不存在！", Toast.LENGTH_LONG).show();
        }
        return sdDir.toString();
    }


    // 文件写操作函数
    public static void writeFile(String content, boolean append) {

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) { // 如果sdcard存在
            File file = new File(PATH); // 定义File类对象

            if (!file.getParentFile().exists()) { // 父文件夹不存在
                file.getParentFile().mkdirs(); // 创建文件夹
            }
            PrintStream out = null; // 打印流对象用于输出
            try {
                out = new PrintStream(new FileOutputStream(file, append)); // 追加文件
                if (!content.equals("")) //空的就不输出了
                    out.println(content);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    out.close(); // 关闭打印流
                }
            }
        }
    }


    // 文件读操作函数
    private ArrayList<String> read() {

//        if (Environment.getExternalStorageState().equals(
//                Environment.MEDIA_MOUNTED)) { // 如果sdcard存在
            File file = new File(PATH); // 定义File类对象
            if (!file.getParentFile().exists()) { // 父文件夹不存在
                file.getParentFile().mkdirs(); // 创建文件夹
            }
            Scanner scan = null; // 扫描输入
            //StringBuilder sb = new StringBuilder();
             ArrayList<String> sb=new ArrayList<String>();
            try {
                scan = new Scanner(new FileInputStream(file)); // 实例化Scanner
                while (scan.hasNext()) {            // 循环读取
                    sb.add(scan.next()); // 设置文本
                }
                return sb;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (scan != null) {
                    scan.close(); // 关闭打印流
                }
            }
//        } else { // SDCard不存在，使用Toast提示用户
//            Toast.makeText(this, "读取失败，SD卡不存在！", Toast.LENGTH_LONG).show();
//        }
        return null;
    }


}