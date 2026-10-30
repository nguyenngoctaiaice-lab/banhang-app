package vn.banhang.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AppBridgePlugin.class);
        super.onCreate(savedInstanceState);
        askPermissions();
    }

    /** Hỏi quyền camera, vị trí, ảnh/lưu trữ ngay khi mở app (chỉ hỏi những quyền chưa được cấp). */
    private void askPermissions() {
        List<String> need = new ArrayList<>();
        List<String> all = new ArrayList<>();
        all.add(Manifest.permission.CAMERA);
        all.add(Manifest.permission.ACCESS_FINE_LOCATION);
        all.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) {
            all.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            all.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT <= 28) all.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        for (String p : all) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) need.add(p);
        }
        if (!need.isEmpty()) ActivityCompat.requestPermissions(this, need.toArray(new String[0]), 4321);
    }
}
