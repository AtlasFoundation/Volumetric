package com.example.corto;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.corto.databinding.ActivityMainBinding;

import java.util.Timer;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ActivityMainBinding binding;
    MeshView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        view = new MeshView(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TextView tv = binding.sampleText;
        tv.setText("Static text");

        String videoTexturePath = "android.resource://" + getPackageName() + "/" + R.raw.liamt;
        String volumetricPath = "liamu.uvol";
        String manifestPath = "liam.manifest";

        setContentView(view);
        LoadActor(manifestPath, volumetricPath, videoTexturePath);

    }

    public void PlayActor(){
        if(view.actor != null)
            view.actor.Play();
    }

    public void StopActor(){

    }

    public void LoadActor(String manifestUrl, String uvolUrl, String videoUrl){
        if(view.actor != null){
            view.actor.Destroy();
        }

        view.actor = new Actor(this, view, manifestUrl, uvolUrl, videoUrl);
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