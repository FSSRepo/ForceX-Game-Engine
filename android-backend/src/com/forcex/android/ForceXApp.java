package com.forcex.android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.forcex.FX;
import com.forcex.app.EventType;
import com.forcex.app.Game;
import com.forcex.app.InputListener;
import com.forcex.core.SystemDevice;
import com.forcex.io.FileSystem;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

public class ForceXApp extends Activity implements SystemDevice {
    ArrayList<InputListener> inputs;
    AndroidInput input_processor;
    private SystemDevice.OnAndroidFileStream file_system_listener;
    private GLRenderer renderer;
    private Game game;

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int col_idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (col_idx >= 0) {
                        result = cursor.getString(col_idx);
                    }
                }
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


    public void initialize(Game game, boolean fullScreen) {
        if (fullScreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        this.game = game;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (permissionsAllowed()) {
                start();
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
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
        if (requestCode == 100) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.w("ForceXApp", "permissions granted");
                start();
            } else {
                Log.e("ForceXApp", "failed to get permissions from android");
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case 430:
                try {
                    file_system_listener.open(getContentResolver().openInputStream(data.getData()), getFileName(data.getData()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            case 431:
                try {
                    file_system_listener.save(getContentResolver().openOutputStream(data.getData()));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    @Override
    protected void onPause() {
        if (!isFinishing()) {
            if (renderer != null) {
                renderer.pause();
            }
        }
        super.onPause();
    }

    @Override
    public void invokeFileChooser(final boolean open, final String label, final String def_name, final SystemDevice.OnAndroidFileStream listener) {
        runOnUiThread(() -> {
            ForceXApp.this.file_system_listener = listener;
            if (open) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                startActivityForResult(Intent.createChooser(intent, label), 430);
            } else {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_TITLE, def_name);
                startActivityForResult(Intent.createChooser(intent, label), 431);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (isFinishing() && renderer != null) {
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
    public void setCursorState(boolean show) {

    }

    @Override
    public void showInfo(final String info, final boolean isError) {
        runOnUiThread(() -> dialog(info, isError));
    }

    @Override
    public void stopRender() {
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
        FX.fs = new FileSystem() {
            @Override
            protected InputStream getAndroidAsset(String name) {
                try {
                    return getAssets().open(name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        FileSystem.homeDirectory = getExternalFilesDir(null).getAbsolutePath() + "/";
        inputs = new ArrayList<>();
        input_processor = new AndroidInput(this);
        renderer = new GLRenderer(game, input_processor);
        GLSurfaceView glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setOnTouchListener(input_processor);
        glView.setRenderer(renderer);
        setContentView(glView);
        FX.device = this;
    }

    private void stop() {
        if (renderer != null) {
            renderer.destroy();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TextView tv = new TextView(this);
            tv.setText("Error!");
            setContentView(tv);
            Toast.makeText(getApplicationContext(), "The Open GL Renderer is Stopped!", Toast.LENGTH_LONG).show();
            renderer = null;
        }
    }

    private void dialog(String info, final boolean error) {
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setMessage(info);
        dialog.setCancelable(!error);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Close", (p1, p2) -> {
            if (error) {
                finish();
            }
        });
        dialog.show();
    }

    @Override
    protected void onResume() {
        if (renderer != null) {
            renderer.resume();
        }
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        if (renderer.onBackPressed() == EventType.REQUEST_EXIT) {
            destroy();
        }
    }

    public boolean permissionsAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
        }
        return true;
    }

    @Override
    public boolean isJDKDesktop() {
        return false;
    }
}
