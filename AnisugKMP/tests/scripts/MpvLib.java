import com.sun.jna.*;
import javax.swing.*;
import java.awt.*;

public interface MpvLib extends Library {
    Pointer mpv_create();

    int mpv_initialize(Pointer ctx);

    int mpv_set_option_string(Pointer ctx, String name, String data);

    int mpv_command(Pointer ctx, String[] args);

    void mpv_terminate_destroy(Pointer ctx);

    Pointer mpv_wait_event(Pointer ctx, double timeout);

    String mpv_error_string(int error);
}

class TestMpvJna {
    public static void main(String[] args) throws Exception {
        System.out.println("=== LibMpv JNA Debug Test ===");

        // Step 1: Load library
        MpvLib mpv;
        try {
            mpv = (MpvLib) Native.load("mpv", MpvLib.class);
            System.out.println("[OK] libmpv loaded");
        } catch (Exception e) {
            System.out.println("[FAIL] Cannot load libmpv: " + e.getMessage());
            return;
        }

        // Step 2: Create frame with canvas
        JFrame frame = new JFrame("MPV Test");
        frame.setSize(640, 480);
        Canvas canvas = new Canvas();
        canvas.setBackground(Color.BLACK);
        frame.add(canvas);
        frame.setVisible(true);
        Thread.sleep(500);

        long wid = Native.getComponentID(canvas);
        System.out.println("[OK] Canvas WID = " + wid);

        // Step 3: Create mpv
        Pointer ctx = mpv.mpv_create();
        if (ctx == null) {
            System.out.println("[FAIL] mpv_create() returned null");
            return;
        }
        System.out.println("[OK] mpv_create()");

        // Step 4: Set options before init
        int r;
        r = mpv.mpv_set_option_string(ctx, "wid", String.valueOf(wid));
        System.out.println("[" + (r == 0 ? "OK" : "FAIL:" + r) + "] set wid=" + wid);

        r = mpv.mpv_set_option_string(ctx, "vo", "x11");
        System.out.println("[" + (r == 0 ? "OK" : "FAIL:" + r) + "] set vo=x11");

        r = mpv.mpv_set_option_string(ctx, "hwdec", "auto");
        System.out.println("[" + (r == 0 ? "OK" : "FAIL:" + r) + "] set hwdec=auto");

        r = mpv.mpv_set_option_string(ctx, "mute", "yes");
        System.out.println("[" + (r == 0 ? "OK" : "FAIL:" + r) + "] set mute=yes");

        r = mpv.mpv_set_option_string(ctx, "osc", "no");
        System.out.println("[" + (r == 0 ? "OK" : "FAIL:" + r) + "] set osc=no");

        r = mpv.mpv_set_option_string(ctx, "terminal", "yes");
        System.out.println("[" + (r == 0 ? "OK" : "FAIL:" + r) + "] set terminal=yes");

        r = mpv.mpv_set_option_string(ctx, "msg-level", "all=v");
        System.out.println("[" + (r == 0 ? "OK" : "FAIL:" + r) + "] set msg-level=all=v");

        // Step 5: Initialize
        r = mpv.mpv_initialize(ctx);
        System.out.println("[" + (r == 0 ? "OK" : "FAIL:" + r) + "] mpv_initialize()");
        if (r != 0) {
            System.out.println("  error: " + mpv.mpv_error_string(r));
            return;
        }

        // Step 6: Load a test video
        String testFile = args.length > 0 ? args[0] : "/tmp/mpv_test.mp4";
        // Copy resource if needed
        if (args.length == 0) {
            var stream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(
                            "composeResources/anisugkmp.composeapp.generated.resources/drawable/splash.mp4");
            if (stream != null) {
                var fos = new java.io.FileOutputStream(testFile);
                stream.transferTo(fos);
                fos.close();
                System.out.println("[OK] Extracted resource to " + testFile);
            } else {
                System.out.println("[WARN] No resource found, using path: " + testFile);
            }
        }

        r = mpv.mpv_command(ctx, new String[] { "loadfile", testFile, null });
        System.out.println("[" + (r == 0 ? "OK" : "FAIL:" + r) + "] loadfile " + testFile);

        // Step 7: Event loop
        System.out.println("Entering event loop (10 seconds)...");
        long end = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < end) {
            Pointer event = mpv.mpv_wait_event(ctx, 0.5);
            if (event != null) {
                int eventId = event.getInt(0);
                if (eventId != 0) {
                    System.out.println("  event_id=" + eventId);
                }
            }
        }

        mpv.mpv_terminate_destroy(ctx);
        frame.dispose();
        System.out.println("=== Done ===");
    }
}
