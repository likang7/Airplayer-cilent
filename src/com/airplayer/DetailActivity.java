package com.airplayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


public class DetailActivity extends Activity{

	private TextView Name;
	private TextView Resolution;
	private TextView Duration;
	private TextView Size;
	private Button directPlayBtn;
	private Button playBtn;
	private ImageView preview;
	
	private int pid;
	private String fileName;
	private String fileLoad;
	private String command;
	private String returnData;
	private String setQuality;
	private String rtspServer;
	private static final int SERVERPROT = 8188;
	private Socket mSocket = null;
	private String[] quality = new String[]{"高清", "普通", "快速"};
	private String fileHome;
	private String resolution = "1280x720";
	private String airplayerPath = null;
	
	//更新视频预览图
	private final int MSG_SUCCESS = 0;//获取图片成功的标识  
	private final int MSG_FAILURE = -1;//获取图片失败的标识
	private Handler previewHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what){
			case MSG_SUCCESS:
				preview.setImageBitmap((Bitmap)msg.obj);
				break;
			case MSG_FAILURE:
				break;
			}
		}
	};
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.detail);
		
		Name = (TextView) this.findViewById(R.id.fname);
		Resolution = (TextView) this.findViewById(R.id.resolution);
		Duration = (TextView) this.findViewById(R.id.duration);
		Size = (TextView) this.findViewById(R.id.size);
		directPlayBtn = (Button) this.findViewById(R.id.direct);
		playBtn = (Button) this.findViewById(R.id.play);
		preview = (ImageView) this.findViewById(R.id.img);
		
		fileHome = this.getIntent().getStringExtra("homeDir");
		fileLoad = this.getIntent().getStringExtra("load");
		fileName = this.getIntent().getStringExtra("file");
		Name.setText(fileName);
		rtspServer = this.getIntent().getStringExtra("server");
		
		
		//获取媒体字幕
		new Thread(){
			@Override
			public void run() {
				getSubtitle(fileHome + fileLoad + fileName);
			}
		}.start();
		
		//获取媒体信息
		getDetail(fileHome +fileLoad+fileName);
		
		//获取视频预览图，并更新
		new Thread() {
			@Override
			public void run() {
				final String resolution = "256x144";
				Bitmap bm = getPreview("preview " + fileHome + fileLoad + fileName + ",resolution:" + resolution);
				
				if(bm == null)
					previewHandler.obtainMessage(MSG_FAILURE, bm).sendToTarget();
				else
					previewHandler.obtainMessage(MSG_SUCCESS, bm).sendToTarget();
			}
		}.start();

		
		directPlayBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				playMedia(fileHome +fileLoad+fileName+",quality:DIRECT");
			}
        	
        });
		playBtn.setOnClickListener(new OnClickListener(){

			public void onClick(View arg0) {
				setQuality = "high";
			    AlertDialog.Builder builder = 
					new AlertDialog.Builder(DetailActivity.this);
			    builder.setTitle("设置播放参数");
			    //builder.setMessage("设置视频质量");
			    builder.setSingleChoiceItems(quality, 0,  
					new DialogInterface.OnClickListener(){

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							switch(which){
							case 0:
								setQuality = "high";
								resolution = "1280x720";
								break;
							case 1:
								setQuality = "medium";
								resolution = "800x480";
								break;
							case 2:
								setQuality = "low";
								resolution = "640x360";
								break;
							}
						}
				
			    });
			    builder.setPositiveButton("确定", new DialogInterface.OnClickListener(){

				    @Override
				    public void onClick(DialogInterface dialog, int which) {
					    // TODO Auto-generated method stub
				    	playMedia(fileHome +fileLoad+fileName+",quality:"+setQuality + ",resolution:" + resolution);
				    }
				
			    });
			    builder.setNegativeButton("取消", null);
			    builder.show();
			}
        	
        });
	}
	
	//判断是否有SD卡
	public boolean avaiableMedia(){
	    String status=Environment.getExternalStorageState();
	         
	    if(status.equals(Environment.MEDIA_MOUNTED)){
	        return true;
	    }
	    else {
	        return false;
	    }
	}
	
	 public String getAirplayerPath() {
		 if (airplayerPath == null) {
			 airplayerPath = Environment.getExternalStorageDirectory() + "/airplayer";
			 File destDir = new File(airplayerPath);
			 if(!destDir.exists()){
				 destDir.mkdirs();
			 }
			 
			 airplayerPath = airplayerPath + "/subtitle/";
			 destDir = new File(airplayerPath);
			 if(!destDir.exists()){
				 destDir.mkdirs();
			 }
		 }
		 return airplayerPath;
	}

	
	private void download(String fp, String filename){
		if(avaiableMedia() == false)
			return;

	    try {
	    	Socket mSocket = new Socket(rtspServer, SERVERPROT);
			PrintWriter out = new PrintWriter(mSocket.getOutputStream());
			final String downloads = "download";
			final String request = downloads + " " + fp;
    		out.print(request);
    		out.flush();
    		
    		InputStream is = mSocket.getInputStream();
    		
	        FileOutputStream fileOutputStream = null;
	        if (is != null) {
	            File file = new File(getAirplayerPath(), filename);   
	            
	            fileOutputStream = new FileOutputStream(file);
	                             
	            byte[] buf = new byte[4096];
	            int ch = -1;
	            
	            while ((ch = is.read(buf)) != -1) {
	                fileOutputStream.write(buf, 0, ch);
	            }
	     
	        }
	        fileOutputStream.flush();
	        if (fileOutputStream != null) {
	            fileOutputStream.close();
	        }
	        if (is != null) {
	            is.close();
	        } 
	        
	        mSocket.close();
	    }  catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	private void getSubtitle(String path){
		final String getsubtitles = "getsubtitles";
		final String request = getsubtitles + " " + path;

		try{
    		Socket mSocket = new Socket(rtspServer, SERVERPROT);
    		BufferedReader in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			PrintWriter out = new PrintWriter(mSocket.getOutputStream());
    		out.print(request);
    		out.flush();
    		
    		ArrayList<String> subtitles = new ArrayList<String>();
    		boolean firstLine = true;
    		while((returnData = in.readLine()) != null){
    			if(firstLine){
    				firstLine = false;
    				if(returnData.startsWith("ERROR"))
    					break;
    				else
    					continue;
    			}
    			subtitles.add(new String(returnData));
    		}
    		mSocket.close();
    		
    		for(int i = 0; i < subtitles.size(); i++){
    			Log.i("subtitles", subtitles.get(i));
    			String fp = subtitles.get(i);
    			int start = fp.lastIndexOf('/');
    			if(start == -1)
    				start = 0;
    			String filename = fp.substring(start);
    			download(subtitles.get(i), filename);//subtitles.get(i).substring(subtitles.get(i).lastIndexOf('/')));
    		}
    		
		}catch (IOException e){
			e.printStackTrace();
		}
	}
	
	private Bitmap getPreview(String cmd){
		Bitmap bitmap = null;
		try{
			Socket mSocket = new Socket(rtspServer, SERVERPROT);
			PrintWriter out = new PrintWriter(mSocket.getOutputStream());
    		out.print(cmd);
    		out.flush();
    		bitmap = BitmapFactory.decodeStream(mSocket.getInputStream());
    		
    		mSocket.close();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return bitmap;
	}

	private void getDetail(String c) {
		command = "describe " + c;
		try {
    		mSocket = new Socket(rtspServer, SERVERPROT);
    		BufferedReader in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
    		PrintWriter out = new PrintWriter(mSocket.getOutputStream());
    		out.print(command);
    		out.flush();
    		int count = 0;
    		String[] temp;
			while((returnData = in.readLine()) != null){
				if(count == 1){
					temp = returnData.split(", ");
				    Resolution.setText(temp[0]);
				    Duration.setText(temp[1]);
				    Size.setText(temp[2]);
				}
				count++;
			}
			mSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void playMedia(String c) {
		// TODO Auto-generated method stub
    	command = "play " + c;
    	try {
    		mSocket = new Socket(rtspServer, SERVERPROT);
    		BufferedReader in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
    		PrintWriter out = new PrintWriter(mSocket.getOutputStream());
    		out.print(command);
    		out.flush();
    		int count = 0;
    		while((returnData = in.readLine()) != null){
				if(count == 0)
				{
					;
				}
				//if(count == 1)
					//pid = Integer.parseInt(returnData);
				else if(count == 1) {
		            mSocket.close();
					break;
				}
				count++;
			}
			System.out.println(returnData);
			Uri uri = Uri.parse(returnData);
            /*Intent intent;
            PackageManager manager = getPackageManager();

            intent = manager.getLaunchIntentForPackage("com.mxtech.videoplayer.ad");
            if (null != intent){
               intent.setPackage("com.mxtech.videoplayer.ad");
               intent.setClassName(  "com.mxtech.videoplayer.ad" , "com.mxtech.videoplayer.ad.ActivityScreen" );
            }
            else{
            	intent = new Intent();
            }*/
			Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(Intent.ACTION_VIEW);
            //intent.setDataAndType(uri, "video/*");
            intent.setData(uri);
            startActivity(intent);
            
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}