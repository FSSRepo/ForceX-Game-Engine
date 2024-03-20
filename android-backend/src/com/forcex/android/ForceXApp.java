package com.forcex.android;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.*;
import android.provider.*;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.forcex.*;
import com.forcex.app.*;
import com.forcex.core.*;
import java.io.*;
import com.forcex.utils.*;
import java.util.*;
import android.database.*;

public class ForceXApp extends Activity implements SystemDevice
{
	SystemDevice.OnAndroidFileStream listen;
	
	private GLRenderer renderer;
	private Game game;
	ArrayList<InputListener> inputs;
	AndroidInput input_processor;
	ArrayList<String> default_folders = new ArrayList<>();

	private String getFileName(Uri uri) {
		String result = null;
		if (uri.getScheme().equals("content")) {
			Cursor cursor = getContentResolver().query(uri, null, null, null, null);
			try {
				if (cursor != null && cursor.moveToFirst()) {
					result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
				}
			} finally {
				cursor.close();
			}
		}
		if (result == null) {
			result = uri.getPath();
			int cut = result.lastIndexOf('/');
			if (cut != -1) {
				result = result.substring(cut + 1);
			}
		}
		return result;
	}

	public void addFolder(String folder) {
		default_folders.add(folder);
	}
	
	public void initialize(Game game, boolean fullScreen){
		if(fullScreen){
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		default_folders.add("gui");
		default_folders.add("fonts");
		default_folders.add("shaders");
		this.game = game;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
			if(permissionsAllowed()) {
				start();
			} else {
				requestPermissions(new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },100);
			}
			Log.w("ForceXApp", "need permissions");
		} else {
			start();
			Log.w("ForceXApp", "ignore permissions request");
		}
	}
	
	@Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if(grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
				Log.w("ForceXApp", "permissions granted");
				start();
            } else {
				Log.e("ForceXApp", "failed to get permissions from android");
				Intent intent = new Intent();
				intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
				Uri uri = Uri.fromParts("package",getPackageName(),null);
				intent.setData(uri);
				startActivity(intent);
            }
        }
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode != Activity.RESULT_OK){
			return;
		}
		switch(requestCode) {
			case 430:
				try
				{
					listen.open(getContentResolver().openInputStream(data.getData()), getFileName(data.getData()));
				}
				catch (FileNotFoundException e)
				{}
				break;
			case 431:
				try
				{
					listen.save(getContentResolver().openOutputStream(data.getData()));
				}
				catch (FileNotFoundException e)
				{}
				break;
		}
	}
	
	
	@Override
	protected void onPause(){
		if(!isFinishing()){
			if(renderer != null){
				renderer.pause();
			}
		}
		super.onPause();
	}

	@Override
	public void invokeFileChooser(final boolean open,final String label,final String def_name,final SystemDevice.OnAndroidFileStream listener)
	{
		runOnUiThread(() -> {
			ForceXApp.this.listen = listener;
			if(open) {
				Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
				intent.setType("*/*");
				startActivityForResult(Intent.createChooser(intent,label), 430);
			} else {
				Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
				intent.setType("*/*");
				intent.putExtra(Intent.EXTRA_TITLE, def_name);
				startActivityForResult(Intent.createChooser(intent,label), 431);
			}
		});
	}
	@Override
	protected void onDestroy() {
		if(isFinishing() && renderer != null){
			renderer.destroy();
		}
		super.onDestroy();
	}
	

	@Override
	public void destroy() {
		runOnUiThread(() -> {
			finish();
			android.os.Process.killProcess(android.os.Process.myPid());
		});
	}

	@Override
	public void showInfo(final String info,final boolean isError)
	{
		runOnUiThread(() -> dialog(info,isError));
	}

	@Override
	public void stopRender(){
		runOnUiThread(() -> stop());
	}

	@Override
	public int getAndroidVersion() {
		return Build.VERSION.SDK_INT;
	}

	@Override
	public void addInputListener(InputListener input) {
		inputs.add(input);
	}
	
	private void start() {
		FX.homeDirectory = getExternalFilesDir(null).getAbsolutePath() + "/";
		createEnvironmentFiles();
		inputs = new ArrayList<>();
		mkNomedia();
		input_processor = new AndroidInput(this);
		renderer = new GLRenderer(game,input_processor);
		GLSurfaceView glView = new GLSurfaceView(this);
		glView.setEGLContextClientVersion(2);
		glView.setOnTouchListener(input_processor);
		glView.setRenderer(renderer);
		setContentView(glView);
		FX.device = this;
	}
	
	private void stop() {
		if(renderer != null){
			renderer.destroy();
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			TextView tv = new TextView(this);
			tv.setText("Error!");
			setContentView(tv);
			Toast.makeText(getApplicationContext(),"The Open GL Renderer is Stopped!",Toast.LENGTH_LONG).show();
			renderer = null;
		}
	}
	
	private void dialog(String info,final boolean error){
		AlertDialog dialog = new AlertDialog.Builder(this).create();
		dialog.setMessage(info);
		dialog.setCancelable(!error);
		dialog.setButton(DialogInterface.BUTTON_POSITIVE,"Close", (p1, p2) -> {
			if(error) {
				finish();
			}
		});
		dialog.show();
	}
	
	@Override
	protected void onResume() {
		if(renderer != null){
			renderer.resume();
		}
		super.onResume();
	}
	
	@Override
    public void onBackPressed() {
        switch (renderer.onBackPressed()) {
            case EventType.REQUEST_EXIT:
                destroy();
                break;
        }
    }
	
	public boolean permissionsAllowed() {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
			return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
		}
		return true;
	}
	
	public void mkDirs(){
		File dir = new File(FX.homeDirectory);
		if(!dir.exists()) {
			dir.mkdir();
		}
		for(String folder : default_folders){
			File mk = new File(dir.getAbsolutePath()+"/"+folder);
			if(!mk.exists()){
				mk.mkdir();
			}
		}
	}
	
	public void mkNomedia() {
		File dir = new File(FX.homeDirectory + ".nomedia");
		if(!dir.exists()){
			try {
				if(dir.createNewFile()){
					System.out.println("No media created");
				}
			}
			catch (Exception e)
			{}
		}
	}
	
	private void createEnvironmentFiles() {
		mkDirs();
		extractAssets();
	}

	@Override
	public boolean isJDKDesktop() {
		return false;
	}
	
	public void extractAssets() {
		try{
			for(String folder : default_folders){
				String[] files = getAssets().list(folder);
				for(String file : files){
					if(new File(FX.homeDirectory + folder + "/" + file).exists()){
						String sha1 = SHA1Checksum.getSHA1(getAssets().open(folder + "/" + file));
						if(sha1.equals(SHA1Checksum.getSHA1(new FileInputStream(FX.homeDirectory + folder + "/" + file)))){
							continue;
						}
					}
					InputStream is = getAssets().open(folder + "/" + file);
					FileOutputStream os = new FileOutputStream(FX.homeDirectory + folder + "/" + file);
					byte[] data = new byte[is.available()];
					is.read(data);
					os.write(data);
					is.close();
					os.close();
					
				}
			}
		} catch(Exception e) {
			Logger.log(e);
		}
	}
}
