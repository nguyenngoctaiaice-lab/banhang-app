package vn.banhang.app;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Base64;
import android.webkit.WebView;

import androidx.core.content.FileProvider;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;

@CapacitorPlugin(name = "AppBridge")
public class AppBridgePlugin extends Plugin {

    /** In trang hiện tại bằng bảng in của Android (CSS @media print của hóa đơn vẫn được áp dụng). */
    @PluginMethod
    public void print(final PluginCall call) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    WebView wv = getBridge().getWebView();
                    PrintManager pm = (PrintManager) getActivity().getSystemService(Context.PRINT_SERVICE);
                    String job = "HoaDon";
                    PrintDocumentAdapter adapter = wv.createPrintDocumentAdapter(job);
                    pm.print(job, adapter, new PrintAttributes.Builder().build());
                    call.resolve();
                } catch (Exception e) {
                    call.reject(String.valueOf(e.getMessage()));
                }
            }
        });
    }

    /** Chia sẻ ảnh hóa đơn. Có pkg (vd com.zing.zalo) thì mở thẳng ứng dụng đó, không có/không mở được thì hiện bảng chọn. */
    @PluginMethod
    public void shareImage(PluginCall call) {
        try {
            String b64 = call.getString("base64", "");
            String name = call.getString("name", "hoa-don.png");
            String text = call.getString("text", "");
            String pkg = call.getString("pkg", "");
            byte[] data = Base64.decode(b64, Base64.DEFAULT);

            File dir = new File(getContext().getCacheDir(), "share");
            dir.mkdirs();
            File f = new File(dir, name.replaceAll("[^A-Za-z0-9._-]", "_"));
            FileOutputStream os = new FileOutputStream(f);
            os.write(data);
            os.close();

            Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", f);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            if (text != null && text.length() > 0) send.putExtra(Intent.EXTRA_TEXT, text);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            boolean direct = false;
            if (pkg != null && pkg.length() > 0) {
                Intent d = new Intent(send);
                d.setPackage(pkg);
                try {
                    getActivity().startActivity(d);
                    direct = true;
                } catch (ActivityNotFoundException e) {
                    direct = false;
                }
            }
            if (!direct) {
                Intent chooser = Intent.createChooser(send, "Chia s\u1ebb h\u00f3a \u0111\u01a1n");
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                getActivity().startActivity(chooser);
            }
            JSObject r = new JSObject();
            r.put("direct", direct);
            call.resolve(r);
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }
}
