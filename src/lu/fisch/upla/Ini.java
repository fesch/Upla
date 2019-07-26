package lu.fisch.upla;

import java.io.*;
import java.util.*;

public class Ini {

    private static final String ininame = "upla.ini";
    private static String filename = "";
    private static Properties p = new Properties();
    private static Ini ini = null;

    public static String getDirname() {
        String path = ".";
        try {
            path = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath().replace("Upla.jar", "").replace("upla.jar", "");
            System.setProperty("user.dir", path);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return path;
    }

    public static void set(String key, String value) {
        Ini ini = Ini.getInstance();
        try {
            ini.load();
            ini.setProperty(key, value);
            ini.save();
        } catch (Exception ex) {
            // ignore any exception
        }
    }

    public static String get(String key, String defaultValue) {
        Ini ini = Ini.getInstance();
        try {
            ini.load();
            return ini.getProperty(key, defaultValue);
        } catch (Exception ex) {
            // ignore any exception
        }
        return null;
    }

    public static Ini getInstance() {
        try {
            filename = getDirname() + System.getProperty("file.separator") + ininame;
        } catch (Error e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ini == null) {
            ini = new Ini();
        }
        return ini;
    }

    public void load() throws FileNotFoundException, IOException {
        File f = new File(filename);
        if (f.length() == 0) {
            //System.out.println("File is empty!");
        } else {
            //p.loadFromXML(new FileInputStream(filename));
            p.load(new FileInputStream(filename));
        }
    }

    public void save() throws FileNotFoundException, IOException {
        /*OutputStream os = new FileOutputStream(filename);
		p.storeToXML(os, "last updated " + new java.util.Date());
		os.close();
         */
        //p.storeToXML(new FileOutputStream(filename), "last updated " + new java.util.Date());
        p.store(new FileOutputStream(filename), "last updated " + new java.util.Date());
    }

    public String getProperty(String _name, String _default) {
        if (p.getProperty(_name) == null) {
            return _default;
        } else {
            return p.getProperty(_name);
        }
    }

    public void setProperty(String _name, String _value) {
        p.setProperty(_name, _value);
    }

    public Set keySet() {
        return p.keySet();
    }

    private Ini() {
        try {
            File f = new File(filename);

            if (!f.exists()) {
                try {
                    File predefined = new File(filename);
                    if (predefined.exists()) {
                        p.load(new FileInputStream(predefined.getAbsolutePath()));
                    }
                    //setProperty("dummy","dummy");
                    save();
                } catch (Exception e) {
                    //e.printStackTrace();
                    System.out.println(e.getMessage());
                }
            }
        } catch (Error e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

}
