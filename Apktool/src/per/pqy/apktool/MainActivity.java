package per.pqy.apktool;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class MainActivity extends Activity {
	static int count=0;
	MyHandler myHandler = new MyHandler();
	ProgressDialog myDialog;
	private TextView tvpath;
	private ListView lvFiles;
	private TextView tv;
	private static final int DECODE = 1;
	private static final int COMPILE = 2;
	private static final int DEODEX = 3;
	private static final int DECDEX = 4;
	private static final int LONGPRESS = 5;
	public String uri;
	File currentParent;
	File[] currentFiles;	
	class MyHandler extends Handler {	
		@SuppressWarnings("deprecation")
		public void doWork(String str,final Bundle b){
			if(b.getBoolean("isTemp")){
				myDialog.setMessage(b.getString("op"));
			}else{
			SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE);
			if(settings.getInt("Vib", 0 ) != 0){
				Vibrator v = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
				v.vibrate(new long[]{0,200,100,200},-1);
			}
			if(settings.getInt("Noti", 0 ) != 0){
				NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				Notification notification = new Notification(R.drawable.ic_launcher,"操作完成！",System.currentTimeMillis());
				Context context = getApplicationContext(); 
				CharSequence contentTitle = b.getString("filename"); 
				CharSequence contentText =  "操作完成!"; 
				Intent notificationIntent = MainActivity.this.getIntent();
				PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this,0,notificationIntent,0);
				notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);	
				notification.flags |= Notification.FLAG_AUTO_CANCEL;				
				mNotificationManager.notify(count++,notification);				
			} 	
			myDialog.dismiss();
			Toast.makeText(MainActivity.this, str,Toast.LENGTH_LONG).show();
			AlertDialog.Builder b1 = new AlertDialog.Builder(
					MainActivity.this);
			String tmp_str = b.getString("filename")+"\n"+ new String("本次操作共耗时：");
			
			long time = (System.currentTimeMillis() - b.getLong("time"))/1000;
			if(time > 3600){
				tmp_str += Integer.toString((int) (time/3600)) + "时" + Integer.toString((int) (time%3600)/60) +
						"分" + Integer.toString((int) (time%60)) + "秒";
			}
			else if(time > 60){
				tmp_str +=  Integer.toString((int) (time%3600)/60) +
						"分" + Integer.toString((int) (time%60)) + "秒";
			}
			else{
				tmp_str +=  Integer.toString((int) time) + "秒";
			}
			b1.setTitle(tmp_str)
			.setMessage(b.getString("output"))
			.setPositiveButton("确定", null)
			.setNeutralButton(("复制"),
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,
						int which) {
					// TODO Auto-generated method stub
					ClipboardManager cmb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					cmb.setText(b.getString("output"));
				}
			}).create().show();
			currentFiles = currentParent.listFiles();
			inflateListView(currentFiles);
		}
		}
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			final Bundle b = msg.getData();
			switch (b.getInt("what")) {
			case 0:
				doWork("反编译完成！",b);
				break;
			case 1:
				doWork("签名完成！",b);
				break;
			case 2:
				doWork("编译完成！",b);
				break;
			case 3:
				doWork("反编译dex完成！",b);
				break;
			case 4:
				doWork("反编译资源完成！",b);
				break;
			case 5:
				doWork("odex反编译完成！",b);
				break;
			case 6:
				doWork("操作完成！",b);
				break;
			case 7:
				doWork("安装完成!",b);
				break;
			case 8:
				doWork("优化完成！",b);
				break;
			case 9:
				doWork("添加完成！",b);
				break;
			case 10:
				doWork("删除完成！",b);
				break;
			}
		}
	}
	public void threadWork(Context context,String message,final String command,final int what){
		 Thread thread = new Thread(){		
			public void run(){
				java.lang.Process process = null;
				DataOutputStream os = null;
				InputStream proerr = null; 
				InputStream proin = null;
				try {					
					Bundle tb = new Bundle();
					tb.putString("filename", new File(uri).getName());
					tb.putInt("what", what);					
					tb.putLong("time",System.currentTimeMillis());					
					tb.putBoolean("isTemp", false);	
					process = Runtime.getRuntime().exec("su ");
					os = new DataOutputStream(process.getOutputStream());
					proerr = process.getErrorStream();
					proin = process.getInputStream();
					os.writeBytes(command + "\n");
					os.writeBytes("exit\n");
					os.flush();					
					BufferedReader br1 = new BufferedReader(new InputStreamReader(proerr));
					String s = "";
					String totals="";					
					while((s=br1.readLine())!=null){
						Message mess = new Message();
						Bundle b = new Bundle();
						totals+=s+"\n";						
						b.putString("op", s);
						b.putInt("what", what);
						b.putBoolean("isTemp", true);
						mess.setData(b);
						myHandler.sendMessage(mess);
					}					
					process.waitFor();
					Message tmess = new Message();					
					tb.putString("output",totals+RunExec.inputStream2String(proin, "utf-8"));												
					tmess.setData(tb);
					myHandler.sendMessage(tmess);
				} catch (Exception e) {
					Log.d("*** DEBUG ***",
							"ROOT REE" + e.getMessage());
				} finally {
					try {
						if (os != null) {
							os.close();
						}
						process.destroy();
					} catch (Exception e) {
					}
				}			
			}		
		};
		thread.start();
		myDialog = new ProgressDialog(context);
		myDialog.setMessage(message);
		myDialog.setIndeterminate(true);
		myDialog.setCancelable(false);
		myDialog.setButton(DialogInterface.BUTTON_POSITIVE, "放入后台", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		        myDialog.dismiss();
		    }
		});
		/*
		myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消操作", new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
		       dialog.dismiss();
		    	
		    }
		});
		*/
		myDialog.show();
	}
	protected Dialog onCreateDialog(int id) {

		switch (id) {
		case DECODE:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.dec_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {						
							switch (which) {							
							case 0:
								final	String command = new String(" /lix/bash /sdcard/apktool/apktool.sh d -f ") 
								+ uri + " " + uri.substring(0, uri.length()-4) + "_src";
								threadWork(MainActivity.this,"正在反编译。。。",command,0);
								break;									
							case 1:
								final	String command1 = new String(" /lix/bash /sdcard/apktool/apktool.sh d -f -r ") 
								+ uri + " " + uri.substring(0, uri.length()-4) + "_src";
								threadWork(MainActivity.this,"正在反编译dex。。。",command1,3);
								break;	
							case 2:
								final String command2 = new String(" /lix/bash /sdcard/apktool/apktool.sh d -f -s ") 
								+ uri + " " + uri.substring(0, uri.length()-4) + "_src";
								threadWork(MainActivity.this,"正在反编译资源。。。",command2,4);								
								break;							
							case 3:		
								final String command3 = new String(" /lix/bash /sdcard/apktool/signapk.sh ") 
								+ uri + " " + uri.substring(0, uri.length()-4) + "_sign.apk";
								threadWork(MainActivity.this,"正在签名。。。",command3,1);					
								break;
							case 4:								
									final String command4 = new String("/lix/dexopt-wrapper ") 
									+ uri + " " + uri.substring(0, uri.length()-3) + "odex";
									threadWork(MainActivity.this,"正在生成。。。",command4,6);	
									break;
								
							case 5:
								final String command5 = new String("/lix/zipalign -f -v 4 ") + uri + " " + uri.substring(0, uri.length()-4)+"_zipalign.apk";
								threadWork(MainActivity.this,"正在优化。。。",command5,8);
								break;
							case 6:
								Intent intent = new Intent(Intent.ACTION_VIEW);  
						        final Uri apkuri = Uri.fromFile(new File(uri));  
						        intent.setDataAndType(apkuri, "application/vnd.android.package-archive");  
						        startActivity(intent);
						        break;
							case 7:
								return;								
							}
						}
					}).create();
		case COMPILE:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.comp_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								if(uri.endsWith("_src")){
								final String command = new String(" /lix/bash /sdcard/apktool/apktool.sh b -f -a /lix/aapt ")
								+ uri + " " + uri + ".apk";								
								threadWork(MainActivity.this,"正在编译，请稍等...",command,2);			
								}else if(uri.endsWith("_odex")){
									final String command = new String(" /lix/bash /sdcard/apktool/smali.sh ")
									+ uri + " " + uri.substring(0, uri.length()-5) + ".dex";									
									threadWork(MainActivity.this,"正在编译，请稍等...",command,2);
								}else if(uri.endsWith("_dex")){
									final String command = new String(" /lix/bash /sdcard/apktool/smali.sh ")
									+ uri + " " + uri.substring(0, uri.length()-4) + ".dex";									
									threadWork(MainActivity.this,"正在编译，请稍等...",command,2);
								}
								break;
							case 1:
								currentFiles = new File(uri).listFiles();
								inflateListView(currentFiles);
							case 2:
								
								return;
							}
						}
					}).create();
		case DEODEX:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.deodex_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								/*
								String apkFile = uri.substring(0, uri.length()-5);
								if(new File(apkFile+".apk").exists()){
									RunExec.Cmd("mkdir /tmp/"+new File(apkFile).getName());
									final String command = new String(" sh /sdcard/apktool/combine.sh ") + uri + " " 
											+ uri + "_s" +  " /tmp/"+ new File(apkFile).getName()+"/classes.dex" + " " + apkFile+".apk";
									threadWork(MainActivity.this,"正在合并，请稍等...",command,5);													
								}else if(new File(apkFile+".jar").exists()){
									RunExec.Cmd("mkdir /tmp/"+new File(apkFile).getName());
									final String command = new String(" sh /sdcard/apktool/combine.sh ") + uri + " " 
											+ uri + "_s" + " /tmp/"+ new File(apkFile).getName()+"/classes.dex" + " " + apkFile+".jar";
									threadWork(MainActivity.this,"正在合并，请稍等...",command,5);													
								}else{
									Toast.makeText(MainActivity.this, "当前目录没有同名的apk文件或jar文件，无法合并！",Toast.LENGTH_LONG).show();
								} */
								int apicode = android.os.Build.VERSION.SDK_INT;
								final String command = new String(" /lix/bash /sdcard/apktool/baksmali.sh -x -a ")+String.valueOf(apicode)
								+" "+uri+" -o "+uri.substring(0, uri.length()-5)+"_odex";
							//	Toast.makeText(MainActivity.this, command,Toast.LENGTH_LONG).show();
								threadWork(MainActivity.this,"正在反编译。。。",command,5);
								break;												
							case 1:
								return;
							}
						}
					}).create();
		case DECDEX:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.decdex_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								final String command = new String(" /lix/bash /sdcard/apktool/baksmali.sh ")+
								uri+" -o "+uri.substring(0, uri.length()-4)+"_dex";
								threadWork(MainActivity.this,"正在反编译。。。",command,3);
								break;
							case 1:
								String apkFile = uri.substring(0,uri.length()-3)+"apk";
								if(new File(apkFile).exists()){
									RunExec.Cmd(new String(" mv ")+uri+" "+new File(uri).getParent()+"/classes.dex");
									final String command1 = new String(" /lix/7za a -tzip "+apkFile+" "+new File(uri).getParent()+"/classes.dex");
									threadWork(MainActivity.this,"正在添加。。。",command1,9);
								}
								else
									Toast.makeText(MainActivity.this, "不存在同名apk文件！", Toast.LENGTH_LONG).show();
								break;
							case 2:
								String jarFile = uri.substring(0,uri.length()-3)+"jar";
								if(new File(jarFile).exists()){
									RunExec.Cmd(new String(" mv ")+uri+" "+new File(uri).getParent()+"/classes.dex");
									final String command2 = new String(" /lix/7za a -tzip "+jarFile+" "+new File(uri).getParent()+"/classes.dex");
									threadWork(MainActivity.this,"正在添加。。。",command2,9);
								}
								else
									Toast.makeText(MainActivity.this, "不存在同名jar文件！", Toast.LENGTH_LONG).show();
							case 3:
								return;
							}
						}
					}).create();
		case LONGPRESS:
			return new AlertDialog.Builder(MainActivity.this).setItems(
					R.array.longpress_array, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							switch (which) {
							case 0:
								final EditText et = new EditText(MainActivity.this);
								et.setText(new File(uri).getName());
								new AlertDialog.Builder(MainActivity.this)
								.setTitle("新名称")
								.setView(et)
								.setPositiveButton("确定", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,int which) {
								// TODO Auto-generated method stub
									String newName = et.getText().toString();									
									newName = currentParent + "/" + newName;
									RunExec.Cmd(" chmod 777 "+currentParent);
									new File(uri).renameTo(new File(newName));
									currentFiles = currentParent.listFiles();
									inflateListView(currentFiles);
								}														
								})
								.setNegativeButton("取消", null).show();
								break;
							case 1:
								new AlertDialog.Builder(MainActivity.this)
								.setTitle("删除？(注意：文件名不能包含中文）")
								.setPositiveButton("确定", new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,int which) {
								// TODO Auto-generated method stub	
									File file = new File(uri);
									if(file.isDirectory()){
									final String command = new String(" rm -r ")+uri;
									threadWork(MainActivity.this,"正在删除。。。",command,10);
									}
									else{
										file.delete();
										currentFiles = currentParent.listFiles();
										inflateListView(currentFiles);
									}
								}														
								})
								.setNegativeButton("取消", null).show();
								break;
							case 2:
								RunExec.Cmd(new String("chmod 777 "+uri));
								break;
							case 3:
								return ;
		}
						}
					}).create();
		}
		return null;
	} 

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		myHandler = new MyHandler();
		if (!(new File("/data/data/per.pqy.apktool/tag").exists())) {
			AlertDialog.Builder b1 = new AlertDialog.Builder(MainActivity.this);
			InputStream ips1 = MainActivity.this.getResources()
					.openRawResource(R.raw.agreement);
			DataInputStream dis1 = new DataInputStream(ips1);
			try {
				byte[] bytes;
				bytes = new byte[dis1.available()];
				String str = "";
				while (ips1.read(bytes) != -1)
					str = str + new String(bytes, "UTF-8");
				b1.setTitle("作者声明").setMessage(str);
				b1.setPositiveButton("确定", null);
				b1.setNeutralButton(("不再提示"),
						new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int which) {
						// TODO Auto-generated method stub
						RunExec.Cmd(" mkdir  /data/data/per.pqy.apktool/tag");
					}
				});
				b1.create().show();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					dis1.close();
					ips1.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} 
		new Thread(){
			public void run(){		
		RunExec.Cmd(" mount -o remount,rw rootfs /");
		if (!(new File("/data/data/per.pqy.apktool/lix").exists())) {
			RunExec.Cmd("dd if=/sdcard/apktool/busybox of=/tar");
			RunExec.Cmd("chmod 777 /tar");
			RunExec.Cmd("/tar xf /sdcard/apktool/jvm.tar --directory=/data/data/per.pqy.apktool");
			RunExec.Cmd("chmod -R 755 /data/data/per.pqy.apktool/lix");
			RunExec.Cmd(" rm /tar /lix");
		}
		if (!(new File("/lix").exists())) {
			RunExec.Cmd(" ln -s /data/data/per.pqy.apktool/lix /lix");
		}
		if (!(new File("/tmp").exists())) {
			RunExec.Cmd(" mkdir /tmp");
		}
			}
		}.start();
		setContentView(R.layout.main);
		lvFiles = (ListView) this.findViewById(R.id.files);
		tvpath = (TextView) this.findViewById(R.id.tvpath);
	//	tv = (TextView) this.findViewById(R.id.file_name);
		SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE);
		if(settings.getInt("bg", 0 ) == 0){
			lvFiles.setBackgroundColor(Color.BLACK);
			tvpath.setBackgroundColor(Color.BLACK);
		//	tv.setTextColor(Color.GREEN);
		}
		File root = new File("/");
		currentParent = root;
		currentFiles = currentParent.listFiles();

		// 使用当前目录的所有文件（夹）填充ListView
		inflateListView(currentFiles);

		lvFiles.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view,
					int position, long id) {
				// 单击文件触发的操作
				uri = currentFiles[position].getPath();
				if(uri.contains("//"))
					uri = RunExec.removeRepeatedChar(uri);
				if (currentFiles[position].isFile()) {									
					if (uri.endsWith(".apk") || uri.endsWith(".zip")
							|| uri.endsWith("jar"))
						showDialog(DECODE);
					else if(uri.endsWith(".odex"))
						showDialog(DEODEX);
					else if(uri.endsWith(".dex"))
						showDialog(DECDEX);
					else{
						Intent intent = new Intent(Intent.ACTION_VIEW);  
				        final Uri apkuri = Uri.fromFile(new File(uri));  
				        intent.setDataAndType(apkuri, "*/*");  
				        startActivity(intent);
					}
					return;
				} else if (currentFiles[position].isDirectory()
						&& (currentFiles[position].getName().endsWith("_src")||
								currentFiles[position].getName().endsWith("_odex")||
								currentFiles[position].getName().endsWith("_dex"))) {					
					showDialog(COMPILE);
					return;
				} 
				// 获取用户点击的文件夹下的所有文件
				File[] tem = currentFiles[position].listFiles();
				if (tem == null || tem.length == 0) {
					Toast.makeText(MainActivity.this, "当前路径不可访问或为空",
							Toast.LENGTH_LONG).show();
				} else {
					// 获取用户单击的列表项对应的文件夹，设为当前的文件夹
					currentParent = currentFiles[position];
					// 保存当前的父文件夹内的全部文件和文件夹
					currentFiles = tem;
					// 再次更新listview
					inflateListView(currentFiles);
				}
			}
		});
		
		lvFiles.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int position, long id) {
				// TODO 自动生成的方法存根
				uri = currentFiles[position].getPath();
				if(uri.contains("//"))
					uri = RunExec.removeRepeatedChar(uri);
				showDialog(LONGPRESS);
				return false;
			}			
		}); 
	}
	
	/**
	 * 根据文件夹填充listview
	 * 
	 * @param files
	 */
	@SuppressLint("SimpleDateFormat")
	private void inflateListView(File[] files) {
		List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
		Arrays.sort(files,new FileComparator());
			
		for (int i = 0; i < files.length; i++) {
			Map<String, Object> listItem = new HashMap<String, Object>();
			if (files[i].isDirectory()) {
				listItem.put("icon", R.drawable.folder);			
			}else {
				listItem.put("icon", R.drawable.file);
			}
			listItem.put("filename", files[i].getName());
			File myFile = new File(files[i].getAbsolutePath());
			long modTime = myFile.lastModified();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			long size = myFile.length();
			double fileSize;
			String strSize = null;
			java.text.DecimalFormat df = new java.text.DecimalFormat("#0.00");
			if(size>=1073741824){
				fileSize = (double)size/1073741824.0;				
				strSize = df.format(fileSize)+"G";				
			}else if(size>=1048576){
				fileSize = (double)size/1048576.0;				
				strSize = df.format(fileSize)+"M";				
			}else if(size>=1024){
				fileSize = (double)size/1024;			
				strSize = df.format(fileSize)+"K";
			}else{
				strSize = Long.toString(size)+"B";
			}
			if(myFile.isFile()&&myFile.canRead())
				listItem.put("modify",dateFormat.format(new Date(modTime))+"   " + strSize);
			else
				listItem.put("modify",dateFormat.format(new Date(modTime)));
			
			listItems.add(listItem);
		}

		// 定义一个SimpleAdapter
		SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, listItems,
				R.layout.list_item,
				new String[] { "filename", "icon", "modify" }, new int[] {
				R.id.file_name, R.id.icon, R.id.file_modify });
		// 填充数据集
		lvFiles.setAdapter(adapter);
		
		try {
			tvpath.setText("当前路径:" + currentParent.getAbsolutePath());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent) {
		if (paramInt == KeyEvent.KEYCODE_BACK)
			try {
				if (!currentParent.getCanonicalPath().equals("/")) {
					currentParent = currentParent.getParentFile();
					currentFiles = currentParent.listFiles();
					inflateListView(currentFiles);
				} else {
					AlertDialog.Builder localBuilder = new AlertDialog.Builder(
							this);
					localBuilder.setTitle("退出?");
					localBuilder.setPositiveButton("是",
							new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface paramAnonymousDialogInterface,int paramAnonymousInt) {
							System.exit(0);
						}
					});
					localBuilder.setNegativeButton("否",null);
					localBuilder.create().show();
				}
			} catch (Exception localException) {
			}
		return false;
	}

	public boolean onCreateOptionsMenu(Menu paramMenu) {
		getMenuInflater().inflate(R.menu.menu, paramMenu);
		return true;
	}

	@SuppressWarnings("resource")
	public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
		switch (paramMenuItem.getItemId()) {
		default:
			return false;
		case R.id.about:
			AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
			InputStream localInputStream = getResources().openRawResource(
					R.raw.about);
			new BufferedReader(new InputStreamReader(localInputStream));
			DataInputStream localDataInputStream = new DataInputStream(
					localInputStream);
			try {
				byte[] arrayOfByte = new byte[localDataInputStream.available()];
				String str;
				for (Object localObject2 = "";; localObject2 = str) {
					if (localInputStream.read(arrayOfByte) == -1) {
						localBuilder.setTitle("关于软件").setMessage(
								(CharSequence) localObject2);
						localBuilder.setPositiveButton("确定", null);
						localBuilder.create().show();
						try {
							localDataInputStream.close();
							localInputStream.close();
							return false;
						} catch (IOException localIOException4) {
							localIOException4.printStackTrace();
							return false;
						}
					}
					str = localObject2 + new String(arrayOfByte, "UTF-8");
				}
			} catch (IOException localIOException2) {
				localIOException2.printStackTrace();
				try {
					localDataInputStream.close();
					localInputStream.close();
					return false;
				} catch (IOException localIOException3) {
					localIOException3.printStackTrace();
					return false;
				}
			} finally {
				try {
					localDataInputStream.close();
					localInputStream.close();
					// throw localObject1;
				} catch (IOException localIOException1) {
					while (true)
						localIOException1.printStackTrace();
				}
			}
		case R.id.exit:
			System.exit(0);
		case R.id.install_framework:
			AlertDialog.Builder localBuilder1 = new AlertDialog.Builder(this);
			InputStream localInputStream1 = getResources().openRawResource(
					R.raw.framework);
			new BufferedReader(new InputStreamReader(localInputStream1));
			DataInputStream localDataInputStream1 = new DataInputStream(
					localInputStream1);
			try {
				byte[] arrayOfByte = new byte[localDataInputStream1.available()];
				String str;
				for (Object localObject2 = "";; localObject2 = str) {
					if (localInputStream1.read(arrayOfByte) == -1) {
						localBuilder1.setTitle("关于软件").setMessage(
								(CharSequence) localObject2);
						localBuilder1.setPositiveButton("确定安装", new DialogInterface.OnClickListener() {
							public void onClick(
									DialogInterface paramAnonymousDialogInterface,
									int paramAnonymousInt) {
								//安装框架
								if(new File("/sdcard/apktool/framework.apk").exists()){
									final String command = new String(
											" /lix/bash /sdcard/apktool/apktool.sh if /sdcard/apktool/framework.apk");
									uri="安装framework";
									threadWork(MainActivity.this,"正在安装framework文件...",command,7);
									
								}
								else
									Toast.makeText(MainActivity.this, "未发现framework.apk！", Toast.LENGTH_LONG)
									.show();
							}
						});
						localBuilder1.setNegativeButton("返回", null);
						localBuilder1.create().show();
						try {
							localDataInputStream1.close();
							localInputStream1.close();
							return false;
						} catch (IOException localIOException4) {
							localIOException4.printStackTrace();
							return false;
						}
					}
					str = localObject2 + new String(arrayOfByte, "UTF-8");
				}
			} catch (IOException localIOException2) {
				localIOException2.printStackTrace();
				try {
					localDataInputStream1.close();
					localInputStream1.close();
					return false;
				} catch (IOException localIOException3) {
					localIOException3.printStackTrace();
					return false;
				}
			} finally {
				try {
					localDataInputStream1.close();
					localInputStream1.close();
					// throw localObject1;
				} catch (IOException localIOException1) {
					while (true)
						localIOException1.printStackTrace();
				}
			}
		case R.id.refresh:
			currentFiles = currentParent.listFiles();
			inflateListView(currentFiles);
			return false;	
		case R.id.setting:
			SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE); 
			new AlertDialog.Builder(this).setTitle("个性配置")
			.setMultiChoiceItems(new String[] { "操作完成振动提示", "操作完成通知栏提示" ,"白色背景"},
									new boolean[] {settings.getInt("Vib", 0)==1,settings.getInt("Noti", 0)==1,settings.getInt("bg", 0)==1},
									new DialogInterface.OnMultiChoiceClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					// TODO 自动生成的方法存根
					SharedPreferences settings = getSharedPreferences("Settings", MODE_PRIVATE); 
					SharedPreferences.Editor editor = settings.edit();  					 
					if(isChecked){
						switch (which){
							case 0:
								editor.putInt("Vib", 1);
								editor.commit();
								break;
							case 1:
								editor.putInt("Noti", 1);
								editor.commit();
								break;
							case 2:
								editor.putInt("bg", 1);
								lvFiles.setBackgroundColor(Color.WHITE);
								tvpath.setBackgroundColor(Color.WHITE);
								editor.commit();
						//		tv.setTextColor(Color.BLACK);
								break;
						 					
					}}
					else{
						switch (which){
						case 0:
							editor.putInt("Vib", 0);
							editor.commit(); 
							break;
						case 1:
							editor.putInt("Noti", 0);
							editor.commit(); 
							break;
						case 2:
							editor.putInt("bg", 0);
							lvFiles.setBackgroundColor(Color.BLACK);
							tvpath.setBackgroundColor(Color.BLACK);
							editor.commit(); 
						//	tv.setTextColor(Color.GREEN);
							break;
								
					}}
					
				}
			})
			.setPositiveButton("确定",null)
			.show();
			return false;
		}
		
	}
}
