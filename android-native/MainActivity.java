package vn.banhang.app;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private static final String MSG = "Đang tải dữ liệu vui lòng đợi...";
    private static final int GREEN = Color.parseColor("#14690A");
    private static final int CREAM = Color.parseColor("#FCF8E8");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout overlay;
    private TextView typing, percent;
    private ProgressBar bar;
    private int tick = 0;
    private long startTime;
    private boolean closing = false;

    private final Runnable updater = new Runnable() {
        @Override
        public void run() {
            if (overlay == null) return;
            tick++;
            // Chữ chạy: "Đang tải" -> "Đang tải dữ" -> ... -> "Đang tải dữ liệu vui lòng đợi..."
            int n = Math.min(tick % (MSG.length() + 8), MSG.length());
            SpannableString sp = new SpannableString(MSG);
            sp.setSpan(new ForegroundColorSpan(Color.TRANSPARENT), n, MSG.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            typing.setText(sp);

            // % thật của WebView đang tải trang
            int p = 0;
            try {
                WebView wv = getBridge().getWebView();
                if (wv != null) p = wv.getProgress();
            } catch (Exception ignored) { }
            long elapsed = System.currentTimeMillis() - startTime;
            bar.setProgress(p);
            percent.setText("Đang tải ứng dụng... " + p + "%");

            // Trang web đã tải xong -> gỡ màn hình chờ (trang web có màn hình chờ riêng tiếp nối ngay phía sau)
            if ((p >= 100 && elapsed > 800) || elapsed > 30000) {
                closeOverlay();
                return;
            }
            handler.postDelayed(this, 110);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(AppBridgePlugin.class);
        super.onCreate(savedInstanceState);
        // Cho phép web tự phát âm thanh lúc vừa mở app (không cần chạm màn hình trước)
        try { getBridge().getWebView().getSettings().setMediaPlaybackRequiresUserGesture(false); } catch (Exception ignored) { }
        showOverlay();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void showOverlay() {
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(CREAM);
        overlay.setClickable(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);

        typing = new TextView(this);
        typing.setTextColor(GREEN);
        typing.setTextSize(18);
        typing.setTypeface(Typeface.DEFAULT_BOLD);
        typing.setGravity(Gravity.CENTER);
        typing.setText(MSG);
        box.addView(typing, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        percent = new TextView(this);
        percent.setTextColor(GREEN);
        percent.setTextSize(13);
        percent.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
        percent.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpPct = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpPct.topMargin = dp(10);
        box.addView(percent, lpPct);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.getProgressDrawable().setColorFilter(GREEN, android.graphics.PorterDuff.Mode.SRC_IN);
        LinearLayout.LayoutParams lpBar = new LinearLayout.LayoutParams(dp(220), dp(6));
        lpBar.topMargin = dp(14);
        box.addView(bar, lpBar);

        overlay.addView(box, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        addContentView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        startTime = System.currentTimeMillis();
        handler.post(updater);
    }

    private void closeOverlay() {
        if (closing || overlay == null) return;
        closing = true;
        handler.removeCallbacks(updater);
        final FrameLayout o = overlay;
        // đợi chút cho trang web kịp vẽ khung hình đầu tiên rồi mới gỡ
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ViewGroup parent = (ViewGroup) o.getParent();
                if (parent != null) parent.removeView(o);
                overlay = null;
            }
        }, 300);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
