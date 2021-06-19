package com.example.corto;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.example.corto.databinding.ActivityMainBinding;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "CORTO";
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("libcorto");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        TextView tv = binding.sampleText;

        if(!checkPermission())
            requestPermission();
        try {
            InputStream is = getResources().openRawResource(R.raw.mesh);
            int size = is.available();
            byte[] bytes = new byte[size];
            is.read(bytes, 0, bytes.length);
            Log.v(TAG, "BYTES ARE " + size);
            is.close();

            Log.v(TAG, "BYTE[0] IS " + bytes[0]);
            tv.setText(decode(bytes));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestPermission();
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

    // Call native decode function
    public native String decode(byte[] bytes);

}