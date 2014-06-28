package stericson.busybox.Support;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Shell;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import stericson.busybox.App;
import stericson.busybox.interfaces.CommandCallback;

public class Common {

    static final int BB_VERSION = 65463;

    static ShellCommand command;
    static List<String> paths = new ArrayList<String>();

    public static boolean extractBusybox(Context context, String customBinaryLocation) {
        String realFile = "busybox-" + App.getInstance().getArch() + ".png";

        //sometimes this can cause a nullpointer
        //reference https://code.google.com/p/android/issues/detail?id=8886
        File storageLocation;

        try
        {
            storageLocation = new File(context.getFilesDir().toString() + "/bb");
        }
        catch (NullPointerException e)
        {
            storageLocation = new File(context.getFilesDir().toString() + "/bb");
        }

        if(!storageLocation.exists())
        {
            storageLocation.mkdirs();
        }

        new File(storageLocation.getPath() + "/busybox").delete();

        try {

            InputStream in;

            if(customBinaryLocation.length() > 0)
            {
                in = new FileInputStream(new File(customBinaryLocation));
            }
            else
            {
                in = context.getResources().getAssets().open(realFile);
            }

            OutputStream out = new FileOutputStream(
                    storageLocation.getPath() + "/busybox");
            byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            // we have to close these here
            out.flush();
            out.close();
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (RootTools.exists(storageLocation.getPath() + "/busybox")) {
            try {
                command = new ShellCommand(new CCB(), 0,

                        "dd if=" + storageLocation.getPath() + "/busybox of=/data/local/busybox",
                        "toolbox chmod 0755 /data/local/busybox",
                        "chmod 0755 /data/local/busybox");
                RootTools.getShell(true).add(command);
                command.pause();

                new File(storageLocation.getPath() + "/busybox").delete();

                return true;

            } catch (Exception e) {}
        }

        new File(storageLocation.getPath() + "/busybox").delete();

        return false;
    }

    public static String getSingleBusyBoxPath() {

        try {
            command = new ShellCommand(new CCB(), BB_VERSION, "busybox which busybox");
            Shell.startRootShell().add(command);
            command.pause();

            for (String path : paths) {
                if (path.startsWith("/")) {
                    return path.replace("busybox", "");
                } else {
                    break;
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String[] findBusyBoxLocations(boolean includeSymlinks, boolean single) {

        if (single) {
            String single_location = getSingleBusyBoxPath();

            if (single_location != null) {
                return new String[]{single_location};
            }
        }

        Set<String> tmpSet = new HashSet<String>();

        try {
            for (String paths : RootTools.getPath()) {
                if (RootTools.exists(paths + "/busybox")) {
                    String symlink = RootTools.getSymlink(paths + "/busybox");

                    if (includeSymlinks || symlink.equals("")) {
                        tmpSet.add(paths);

                        if (single)
                            break;
                    }
                }
            }
        } catch (Exception ignore) {
            // nothing found.
        }

        String locations[] = new String[tmpSet.size()];

        int i = 0;
        for (String paths : tmpSet) {
            locations[i] = paths + "/";
            i++;
        }

        return locations;
    }

    public static class CCB implements CommandCallback {
        @Override
        public void commandCallback(CommandResult result) {

        }

        @Override
        public void commandOutput(int id, String line) {
            if (id == BB_VERSION) {
                paths.add(line);
            }
        }
    }
}
