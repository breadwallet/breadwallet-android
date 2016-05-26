package com.breadwallet.tools.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class RootHelper {
    public static boolean isDeviceRooted() {
        return checkRootA() || checkRootB() || checkRootC();
    }

    private static boolean checkRootA() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private static boolean checkRootB() {
        String[] paths = {"/sbin/su", "/data/local/xbin/su", "/system/bin/su", "/system/xbin/su",
                "/data/local/bin/su", "/system/app/Superuser.apk", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkRootC() {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            return in.readLine() != null;
        } catch (Exception e) {
            return false;
        } finally {
            if (p != null) p.destroy();
        }
    }
}