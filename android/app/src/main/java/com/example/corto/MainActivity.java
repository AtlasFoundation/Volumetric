package com.example.corto;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.corto.databinding.ActivityMainBinding;

import java.util.Timer;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ActivityMainBinding binding;
    MeshView view;

    private MediaPlayer mediaPlayer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = new MeshView(this);
        mediaPlayer = new MediaPlayer();

        try {
            AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.liamt);
            mediaPlayer.setDataSource(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        } catch (Exception e) {
            Timber.e("open video fail "+e);
            e.printStackTrace();
        }

        String volumetricPath = "liam.uvol";
        String manifestPath = "liam.manifest";

        setContentView(view);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        LoadActor(manifestPath, volumetricPath);

    }

    public void PlayActor(){
        if(view.actor != null)
            view.actor.Play();
    }

    public void StopActor(){

    }

    public void LoadActor(String manifestUrl, String uvolUrl){
        if(view.actor != null){
            view.actor.Destroy();
        }

        view.actor = new Actor(this, view, manifestUrl, uvolUrl, mediaPlayer);
    }


    private boolean checkPermission() {
        int result = checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    private void requestPermission() {
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows us to read  files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            requestPermissions(new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

}