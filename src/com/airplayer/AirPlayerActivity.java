package com.airplayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.http.util.EncodingUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class AirPlayerActivity extends Activity {
    /** Called when the activity is first created. */
	private ImageButton returnBtn;
	private Button refleshBtn;
	private ListView playList;
	private EditText serverUrl;
	private EditText serverPort;
	private ArrayList<HashMap<String, Object>> listItem;
	private SimpleAdapter adapter;
	private static FormatFilter formatFilter = new FormatFilter(); 
	
	private String rtspServer = "172.18.40.62";
	private static int SERVERPORT = 8188;
	private String command;                                     // 发送的命令
	private String load = "";                                        // 文件路径
	private String returnData;                                  // 接收到的信息
	private Socket mSocket = null;                              // 连接服务器的Socket
	private BufferedReader in = null;                           // 读取服务器返回信息
	private String homeDir = null;
	
	private class MediaItem implements Comparable<MediaItem>{
		public String name = "";
		public int type = -1;
		
		public int compareTo(MediaItem other){
			if(type == 0 && other.type != 0){
				return -1;
			}
			else if(type != 0 && other.type == 0){
				return 1;
			}
			else
				return name.compareTo(other.name);
		}
	}
	
	private MediaItem[] playlist= null; //记录文件类型和文件名字
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //rtspUrl = (EditText) this.findViewById(R.id.url);
        //playBtn = (Button) this.findViewById(R.id.play);
        refleshBtn = (Button) this.findViewById(R.id.reflesh);
        returnBtn = (ImageButton) this.findViewById(R.id.rtn);
        playList = (ListView) this.findViewById(R.id.playlist);
        
        setServer();
        
        
        playList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> adapter, View v, int position,
					long id) {
				// TODO Auto-generated method stub
				if(playlist[position].type == 0) {
					String file = playlist[position].name;
					getPlayList(homeDir +load+"/"+file);
					load = load + "/" + file;
				}
				else if(playlist[position].type == -1)
					Toast.makeText(AirPlayerActivity.this, "Error! Not media file!", 0).show();
				else if(playlist[position].type == 1) {
					Intent intent = new Intent(AirPlayerActivity.this, DetailActivity.class);
					Bundle bundle = new Bundle();
					bundle.putString("load", load+"/");
					bundle.putString("server", rtspServer);
					bundle.putString("file", playlist[position].name);
					bundle.putString("homeDir", homeDir);
					intent.putExtras(bundle);
					startActivity(intent);
				}
			}
        	
        });
        refleshBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				if(homeDir == null)
					getHomeDirectory();
				getPlayList(homeDir +load);
			}
        	
        });
        returnBtn.setOnClickListener(new OnClickListener(){
			public void onClick(View arg0) {
				getUpperDirectoryList();
			}
        });
    }
    
    private boolean getUpperDirectoryList(){
		//System.out.println(load);
		if(!load.equals("")) {
		    int i = load.lastIndexOf("/");
		    load = load.substring(0, i);
		    if(homeDir == null)
				getHomeDirectory();
		    getPlayList(homeDir +load);
		    return true;
		}
		return false;
    }
    
    private void getHomeDirectory(){
    	try{
    		mSocket = new Socket();
    		SocketAddress socketAddress = new InetSocketAddress(rtspServer, SERVERPORT); //获取sockaddress对象
    		mSocket.connect(socketAddress,2500);
			PrintWriter out = new PrintWriter(mSocket.getOutputStream());
    		out.print("home ");
    		out.flush();
    		BufferedReader in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
    		boolean isOK = false;
    		while((returnData = in.readLine()) != null){
    			if(isOK == false){
    				if(returnData.equals("OK")){
    					isOK = true;
    					homeDir = "";
    				}
    			}
    			else
    				homeDir += returnData;
    		}
    		
    		mSocket.close();
    	}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private void setServer() {
    	LayoutInflater inflater = (LayoutInflater) AirPlayerActivity.this
		        .getSystemService(LAYOUT_INFLATER_SERVICE);
		final View view = inflater.inflate(R.layout.set, null);
		SharedPreferences myPreferences = getSharedPreferences("SERVERADD", MODE_PRIVATE);
		rtspServer = myPreferences.getString("SERVERADD", "");
		serverUrl = (EditText) view.findViewById(R.id.server_url);
		serverUrl.setText(rtspServer);
		serverPort = (EditText) view.findViewById(R.id.server_port);
		serverPort.setText(SERVERPORT+"");
		final AlertDialog.Builder builder = new AlertDialog.Builder(AirPlayerActivity.this);
		builder.setTitle("设置").setView(view)
		    .setPositiveButton("确定", new DialogInterface.OnClickListener() {					
				@Override
				public void onClick(DialogInterface dialog, int which) {
					builder.setTitle("");
					rtspServer = serverUrl.getText().toString();
					SERVERPORT = Integer.parseInt(serverPort.getText().toString());
					SharedPreferences mySharedPreferences = getSharedPreferences("SERVERADD", MODE_PRIVATE);
					SharedPreferences.Editor editor = mySharedPreferences.edit();
					editor.putString("SERVERADD", rtspServer);
					editor.commit();
					getHomeDirectory();
					getPlayList(homeDir);
					load = "";
				}
			})
			.setNegativeButton("取消", null);
		builder.show();
	}
    
    private void getPlayList(String c) {
		// TODO Auto-generated method stub
    	command = "ls " + c;
    	playlist = null;
    	try {
    		mSocket = new Socket();
    		SocketAddress socketAddress = new InetSocketAddress(rtspServer, SERVERPORT); //获取sockaddress对象
    		mSocket.connect(socketAddress,2500);
     		in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
    		PrintWriter out = new PrintWriter(mSocket.getOutputStream());
    		out.print(command);
    		out.flush();
    		int count = 0;
    		
			while((returnData = in.readLine()) != null){
				if(count == 1)
				{
					playlist = new MediaItem[Integer.parseInt(returnData)];
					for(int i = 0; i < playlist.length; i++)
						playlist[i] = new MediaItem();
				}
				if(count > 1)
				    getFileData(playlist, count-2, returnData);
				count++;
			}
			mSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	if(playlist == null){
    		playlist = new MediaItem[]{};
    		Toast.makeText(AirPlayerActivity.this, 
    				"Error! Cannot connect server!"+rtspServer, 0).show();
    	}
    	Arrays.sort(playlist);
    	listItem = new ArrayList<HashMap<String, Object>>();
        listItem = addData(playlist);
        
        adapter = new SimpleAdapter(AirPlayerActivity.this, listItem, R.layout.list, 
        		new String[] {"Icon", "ItemName"}, 
                new int[] {R.id.icon, R.id.list_name});
        
        playList.setAdapter(adapter);
    	
	}

	private ArrayList<HashMap<String, Object>> addData(MediaItem[] playlist) {
		ArrayList<HashMap<String, Object>> list = 
				new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> map;
		
		for(int i = 0; i < playlist.length; i++){
			map = new HashMap<String, Object>();
			//switch(type[i]){
			switch(playlist[i].type){
			case 0:
				map.put("Icon", R.drawable.folder);
				break;
			case -1:
				map.put("Icon", R.drawable.not_media);
				break;
			case 1:
				map.put("Icon", R.drawable.media);
				break;
			}
			//String temp = EncodingUtils.getString(data[i].getBytes(), "UTF-8");
			String temp = EncodingUtils.getString(playlist[i].name.getBytes(), "UTF-8");
			map.put("ItemName", temp);
			list.add(map);
		}
		return list;
	}

	protected void getFileData(MediaItem[] playlist, int index, String Data) {
		String[] temp = Data.split(",");
		playlist[index].name = temp[0];
		if(Integer.parseInt(temp[1]) == 0){
			playlist[index].type = 0;
		}
		else {
			//if(formatFilter.isSupportFormat(temp[0])){
				playlist[index].type = 1;
			//}
			//else{
			//	playlist[index].type = -1;
			//}
		}
		//if(temp[1])
	}
    
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "设置");
		menu.add(0, 1, 1, "退出");
		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case 0:
			setServer();
			break;
		case 1:
			AirPlayerActivity.this.finish();
			break;
		}
		return true;
	}
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	    // 如果是返回键,直接上一层
	    if(keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME){
	    	if(!getUpperDirectoryList())
	    		return super.onKeyDown(keyCode, event);
	    	return true;
	    }
	    else
	    	return super.onKeyDown(keyCode, event);
    }
    
    /**
     * onConfigurationChanged
     * the package:android.content.res.Configuration.
     * @param newConfig, The new device configuration.
     * 当设备配置信息有改动（比如屏幕方向的改变，实体键盘的推开或合上等）时，
     * 并且如果此时有activity正在运行，系统会调用这个函数。
     * 注意：onConfigurationChanged只会监测应用程序在AnroidMainifest.xml中通过
     * android:configChanges="xxxx"指定的配置类型的改动；
     * 而对于其他配置的更改，则系统会onDestroy()当前Activity，然后重启一个新的Activity实例。
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {        
            super.onConfigurationChanged(newConfig);
            // 检测屏幕的方向：纵向或横向
            if (this.getResources().getConfiguration().orientation
                            == Configuration.ORIENTATION_LANDSCAPE) {
                    //当前为横屏， 在此处添加额外的处理代码
            }
            else if (this.getResources().getConfiguration().orientation
                            == Configuration.ORIENTATION_PORTRAIT) {
                    //当前为竖屏， 在此处添加额外的处理代码
            }
            //检测实体键盘的状态：推出或者合上   
            if (newConfig.hardKeyboardHidden
                            == Configuration.HARDKEYBOARDHIDDEN_NO){
                    //实体键盘处于推出状态，在此处添加额外的处理代码
            }
            else if (newConfig.hardKeyboardHidden
                            == Configuration.HARDKEYBOARDHIDDEN_YES){
                    //实体键盘处于合上状态，在此处添加额外的处理代码
            }
    }
}