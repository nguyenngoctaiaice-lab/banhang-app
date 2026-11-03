package vn.banhang.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.capacitorjs.plugins.pushnotifications.MessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import me.leolin.shortcutbadger.ShortcutBadger;

/**
 * Nhận thông báo đẩy (data-only) từ máy chủ. Khi app KHÔNG mở: tự hiện thông báo và đặt số lên biểu tượng app
 * (cộng dồn mỗi đơn mới, xóa khi mở app). Khi app đang mở: chỉ chuyển cho trang web xử lý như cũ.
 */
public class BadgeMessagingService extends MessagingService {
    static final String CHANNEL = "don_moi_v2";   // đổi tên kênh vì âm thanh của kênh đã tạo thì không sửa được
    static final String CHANNEL_CMT = "binh_luan_v2";   // đổi tên kênh vì âm thanh của kênh đã tạo thì không sửa được
    static final String PREFS = "badge_prefs";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);   // vẫn chuyển cho plugin (trang web) nếu đang mở
        try {
            if (MainActivity.inFront) return;

            Map<String, String> data = message.getData();
            String title = data.get("title"), body = data.get("body");
            if ((title == null || title.isEmpty()) && message.getNotification() != null) {
                title = message.getNotification().getTitle();
                body = message.getNotification().getBody();
            }
            if (title == null || title.isEmpty()) title = "Quản lý bán hàng";
            if (body == null) body = "";
            boolean comment = "comment".equals(data.get("type"));   // khách bình luận / đánh giá
            String chId = comment ? CHANNEL_CMT : CHANNEL;

            SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            int n = sp.getInt("n", 0) + 1;
            sp.edit().putInt("n", n).apply();

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(chId) == null) {
                NotificationChannel ch = new NotificationChannel(chId, comment ? "Khách bình luận" : "Đơn khách đặt", NotificationManager.IMPORTANCE_HIGH);
                {   // âm thanh riêng: bình luận = res/raw/binh_luan.mp3, đơn mới = res/raw/don_moi.mp3 (phát cả khi tắt màn hình)
                    AudioAttributes aa = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
                    ch.setSound(sound(comment), aa);
                }
                ch.setShowBadge(true);
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
            }

            Intent open = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (open != null) open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder b = new NotificationCompat.Builder(this, chId)
                    .setSmallIcon(getApplicationInfo().icon)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS)
                    .setNumber(n)
                    .setContentIntent(pi);
            b.setSound(sound(comment));   // máy Android 7 trở xuống không có kênh
            Notification nt = b.build();

            // Xiaomi chỉ nhận số huy hiệu qua chính thông báo
            if ("xiaomi".equalsIgnoreCase(Build.MANUFACTURER)) {
                ShortcutBadger.applyNotification(getApplicationContext(), nt, n);
            }
            nm.notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), nt);
            ShortcutBadger.applyCount(getApplicationContext(), n);
        } catch (Exception ignored) { }
    }

    private Uri sound(boolean comment) {
        return Uri.parse("android.resource://" + getPackageName() + "/raw/" + (comment ? "binh_luan" : "don_moi"));
    }
}
