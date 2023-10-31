package lu.fisch.upla;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JOptionPane;

public class Main {

    private static String name = "Unimozer";
    private static String program = "Unimozer.jar";
    private static String programJAR = "Unimozer.jar";
    private static String programUri = "https://unimozer.fisch.lu/webstart/"+program;
    private static String md5Uri = "https://unimozer.fisch.lu/webstart/md5.php";
    private static String iconName = "unimozer.png";
    private static int updateMode = 0;
    // START KGU#1095 2023-10-30: Issue #10
    private static String minJavaVersion = "";
    // END KGU#1095 2023-10-30
    private static String[] args;
    
    private static Matcher VER_MATCHER = Pattern.compile("(.*?)([0-9]+\\.[0-9]+([._][0-9]+)*)(.*?)").matcher("");
    
    public static void main(String[] args) throws IOException
    {
        // save args to class variable
        Main.args=args;
        
        //JOptionPane.showMessageDialog(null, buffer, "BUFFER", JOptionPane.ERROR_MESSAGE);
        //JOptionPane.showMessageDialog(null, "Starting ...", "Error", JOptionPane.ERROR_MESSAGE);

        String path = "";
        try {
            path = Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString().replace("Upla.jar", "").replace("upla.jar", "").replace("file:/", "");
            //JOptionPane.showMessageDialog(null, path, "path", JOptionPane.ERROR_MESSAGE);
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error #main 1", JOptionPane.ERROR_MESSAGE);
        }
        
        if (path.startsWith("/"))
        {
            path = "/" + path;
        }
        
        if((System.getProperty("os.name").toLowerCase().startsWith("mac os x") ||
            System.getProperty("os.name").toLowerCase().startsWith("nix") ||
            System.getProperty("os.name").toLowerCase().startsWith("nux")) &&
            !path.startsWith("/"))
        {
            path = "/"+path;
        }
        
        //JOptionPane.showMessageDialog(null, path, "PATH", JOptionPane.ERROR_MESSAGE);

        //Ini.getInstance().save(); 
        Ini.getInstance().load();
        name = Ini.getInstance().getProperty("name", "Unimozer");
        program = Ini.getInstance().getProperty("program", "Unimozer.jar");
        programJAR = program;
        programUri = Ini.getInstance().getProperty("programUri", "https://unimozer.fisch.lu/webstart/"+program);
        program = path+program;
        md5Uri = Ini.getInstance().getProperty("md5Uri", "https://unimozer.fisch.lu/webstart/md5.php");
        iconName = Ini.getInstance().getProperty("iconName", "unimozer.png");
        updateMode = Integer.valueOf(Ini.getInstance().getProperty("updateMode", "0"));
        // START KGU#1095 2023-10-30: Issue #10
        minJavaVersion = Ini.getInstance().getProperty("minJavaVersion", "");
        // END KGU#1095 2023-10-30
        //Ini.getInstance().save();
        
        String buffer = "";
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            buffer+=arg+" ";
        }
        if(buffer.trim().equals("/modify=1"))
        //if(true)
        {
            try {
                //JOptionPane.showMessageDialog(null, "You are now in modification mode ...", "Modification mode", JOptionPane.ERROR_MESSAGE);
                MainFrame mainFrame = new MainFrame();
                mainFrame.setMode(updateMode);
                mainFrame.setVisible(true);
                Ini.getInstance().setProperty("updateMode",String.valueOf(mainFrame.getMode()));
                Ini.getInstance().save();
                System.exit(0);
            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #main 3", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
        

        Launcher launcher = new Launcher();
        launcher.setIcon(new javax.swing.ImageIcon(launcher.getClass().getResource("/lu/fisch/upla/icons/"+iconName)));
        launcher.setName(name);
        launcher.setVisible(true);
        launcher.setLocationRelativeTo(null);

        launcher.setStatus("Loading ...");

        try 
        {
            File jar = new File(URLDecoder.decode(program,StandardCharsets.UTF_8.name()));
            //JOptionPane.showMessageDialog(null, jar, "jar", JOptionPane.ERROR_MESSAGE);
            launcher.setStatus("Testing local cache ...");
            if(!jar.exists())
            {
                if(updateMode==2)
                {
                    JOptionPane.showMessageDialog(null, "The file <"+programJAR+"> can't be found!"
                        + "\n\n"
                        + "You chose never to look for updates online so "+name+" won't be able to start right now."
                        + "\n\n"
                        + "You may want to modify your installation and switch to another update mode ...", "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                else
                {
                    launcher.setStatus("Testing network ...");
                    if(isOnline())
                    {
                        if(updateMode==1)
                        {
                            int res = JOptionPane.showConfirmDialog(null, "The file <"+programJAR+"> can't be found!"
                                + "\n\n"
                                + "Do you want to download it now? ("+name+" will quit otherwise ...)", "Error", JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
                            if(res==JOptionPane.NO_OPTION)
                                System.exit(1);
                        }
                        
                        launcher.setStatus("Downloading ...");
                        //launcher.setStatus(program);
                        download();
                        launcher.setStatus("Starting application ...");
                        start();
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(null, "The server can't be reached.\nPlease make sure you have an active internet connection ...", "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                }
            }
            else
            {
                if(updateMode!=2)
                {
                    if(isOnline())
                    {
                        if(!getLocalMD5().equals(getRemoteMD5()))
                        {
                            boolean download = true;
                            if(updateMode==1)
                            {
                                int res = JOptionPane.showConfirmDialog(null, "An update is available!"
                                    + "\n\n"
                                    + "Do you want to download it now?", "Information", JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
                                if(res==JOptionPane.NO_OPTION)
                                    download=false;
                            }
                            if(download)
                            {
                                //launcher.setStatus(getLocalMD5()+" - "+getRemoteMD5());
                                launcher.setStatus("Downloading ... ");
                                try {
                                    download();
                                }
                                catch (Exception ex) {
                                    if (JOptionPane.showConfirmDialog(null, 
                                            "The download of the new version of " + program + " failed:\n" + ex
                                            + "\n\nDo you want to start the recent version instead?", "Error",
                                            JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.YES_OPTION) {
                                        JOptionPane.showMessageDialog(null, "Launching " + name + " aborted.", "Error #download", JOptionPane.ERROR_MESSAGE);
                                        System.exit(1);
                                    };
                                }
                            }
                        }
                    }
                }
                launcher.setStatus("Starting application ...");
                start();
            }
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error #main 2", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private static boolean isOnline()
    {
        try {
            final URL url = new URL(md5Uri);
            url.openStream();
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
    
    private static String getRemoteMD5() throws MalformedURLException, IOException
    {
        // create a new trust manager that trust all certificates
        TrustManager[] trustAllCerts;
        trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // activate the new trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } 
        catch (KeyManagementException | NoSuchAlgorithmException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #getRemoteMD5", JOptionPane.ERROR_MESSAGE);
        }

        // dwonload the program
        URL url = new URL(md5Uri);
        BufferedReader in;
        in = new BufferedReader(new InputStreamReader(url.openStream()));

        String inputLine;
        String md5 = "";
        while ((inputLine = in.readLine()) != null)
        {
            md5+=inputLine;
        }
        in.close();;
        return md5.trim();
    }
    
    private static String getLocalMD5() throws NoSuchAlgorithmException, IOException
    {
        // get md5 hash of local file
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(Paths.get(URLDecoder.decode(program,StandardCharsets.UTF_8.name())))) {
            DigestInputStream dis = new DigestInputStream(is, md);
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        StringBuilder checksumSb = new StringBuilder();
        for (byte digestByte : md.digest()) {
          checksumSb.append(String.format("%02x", digestByte));
        }
        return checksumSb.toString();
    }

    private static void download() throws MalformedURLException, IOException
    {
        // create a new trust manager that trust all certificates
        TrustManager[] trustAllCerts;
        trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // activate the new trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } 
        catch (KeyManagementException | NoSuchAlgorithmException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #download", JOptionPane.ERROR_MESSAGE);
        }

        // dwonload the program
        URL url = new URL(programUri);
        URLConnection connection = url.openConnection();
        OutputStream out;
        try (InputStream in = connection.getInputStream()) {
            //System.out.println(program);
            //JOptionPane.showMessageDialog(null, program, "program", JOptionPane.ERROR_MESSAGE);
            out = new FileOutputStream(new File(URLDecoder.decode(program,StandardCharsets.UTF_8.name())));
            byte[] buffer = new byte[2048];
            int length;
            int downloaded = 0;
            while ((length = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, length);
                downloaded+=length;
            }
            out.close();
        }
    }
    
    private static void start() throws IOException, InterruptedException
    {
        // find javaw
        String bin = System.getProperty("file.separator")+"bin";
        //boolean found=false;

        // get boot folder
        String bootFolderList = System.getProperty("sun.boot.library.path");
        String[] bootFolders = bootFolderList.split(File.pathSeparator);
        // START KGU#1095 2023-10-30: Issue #10 We sort by formatted version strings
        //TreeSet<String> directories = new TreeSet<String>();
        TreeMap<String, String> directories = new TreeMap<String, String>();
        // END KGU#1095 2023-10-30
        for (String bootFolder: bootFolders) {
            if (bootFolder.endsWith("bin")) {
                // go back two directories
                bootFolder=bootFolder.substring(0,bootFolder.lastIndexOf(System.getProperty("file.separator")));
                bootFolder=bootFolder.substring(0,bootFolder.lastIndexOf(System.getProperty("file.separator")));

                // get all files from the boot folder
                File bootFolderfile = new File(bootFolder);
                File[] files = bootFolderfile.listFiles();
                for (int i=0; i<files.length; i++)
                {
                    if (files[i].isDirectory()) {
                        // START KGU#1095 2023-10-30: Issue #10
                        //directories.add(files[i].getAbsolutePath());
                        String key = deriveFormattedVersionString(files[i].getName());
                        if (key.isEmpty()) {
                            // Last to be used, as potential fallback
                            key = "000." + files[i].getName();
                        }
                        directories.put(key, files[i].getAbsolutePath());
                        // END KGU#1095 2023-10-30
                    }
                }
            }
        }

        File javaw = null;
        String foundJavaVer = null;
        while (!directories.isEmpty())
        {
            // START KGU#1095 2023-10-30: Issue #10
            //String JDK_directory = directories.last();
            //directories.remove(JDK_directory);
            Entry<String, String> entry = directories.lastEntry();
            String JDK_directory = entry.getValue();
            directories.remove(foundJavaVer = entry.getKey());
            // END KGU#1095 2023-10-30
            
            JDK_directory += bin + System.getProperty("file.separator");
            javaw = new File(JDK_directory + "java");
            if(javaw.exists()) break;
            javaw = new File(JDK_directory + "javaw");
            if(javaw.exists()) break;
            javaw = new File(JDK_directory + "javaw.exe");
            if(javaw.exists()) break;
            foundJavaVer = null;
        }

        // START KGU#1095 2023-10-30: Issue #10
        // FIXME do this after the directory selection if we don't find a suited one...
        if (!minJavaVersion.isEmpty()) {
            javaw = checkJavaVersion(foundJavaVer, javaw);
        }
        // END KGU#1095 2023-10-30

        if (javaw != null)
        {
            // start it
            //System.out.println("Starting: "+javaw.getAbsolutePath()+" -jar "+program);
            if(System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
            {
                ArrayList<String> list = new ArrayList<>();
                list.add(javaw.getAbsolutePath());
                list.add("-jar");
                list.add("-Dapple.laf.useScreenMenuBar=true");
                list.add("-Dcom.apple.macos.use-file-dialog-packages=true");
                list.add("-Dcom.apple.macos.useScreenMenuBar=true");
                list.add("-Dcom.apple.smallTabs=true-Xmx1024M");
                list.add("-Dcom.apple.mrj.application.apple.menu.about.name="+name+"");
                list.add("-Dapple.awt.application.name="+name+"");
                list.add("-Xdock:name="+name);
                list.add(URLDecoder.decode(program,StandardCharsets.UTF_8.name()));
                for (int i = 0; i < args.length; i++) {
                   String arg = args[i];
                    list.add(arg);
                }
                ProcessBuilder processBuilder = new ProcessBuilder(list);
                Process process = processBuilder.start();
               //Process process = new ProcessBuilder(javaw.getAbsolutePath(),"-jar","-Dapple.laf.useScreenMenuBar=true",program).start();
            }
            else
            {
                ArrayList<String> list = new ArrayList<>();
                list.add(javaw.getAbsolutePath());
                list.add("-jar");
                list.add(URLDecoder.decode(program,StandardCharsets.UTF_8.name()));
                for (int i = 0; i < args.length; i++) {
                    String arg = args[i];
                    list.add(arg);
                }
                ProcessBuilder processBuilder = new ProcessBuilder(list);
                Process process = processBuilder.start();
            }
            try {
                // Wait a little before this process terminates
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #start", JOptionPane.ERROR_MESSAGE);
            }
            // terminate this process
            System.exit(0);
        }
    }

    // START KGU#1095 2023-10-30: Issue #10 provide safer version selection
    /**
     * Checks the current Java version in comparison to the required one, may
     * abort this process. Otherwise returns a {@link File} object for the javaw
     * executable, or {@code node}.<br/>
     * <b>Note:<b/> Don't invoke this method {@link IFileLocationResolver}
     * {@link #minJavaVersion} is not specified (i.e. empty).
     * 
     * @param foundVersion - a (formatted) version string derived from the
     *    retrieved Java boot directory, or {@code null} (rather unlikely)
     * @param javaw - either a {@link File} object representing the found
     *    executable, or {@code null} if non was found (rather unlikely
     *    since this process runs Java-based...)
     * 
     * @return a {@link File} object meant to specify executable for starting
     *    the Java subprocess (may be given {@code javaw}).
     */
    private static File checkJavaVersion(String foundVersion, File javaw) {
        String javaVersion = System.getProperty("java.version");
        int cmp1 = compareVersionStrings(javaVersion, minJavaVersion);
        int cmp2 = -2;
        if (foundVersion != null) {
            cmp2 =compareVersionStrings(foundVersion, minJavaVersion); 
        }
        // If the found version looks dubious or obsolete try the current version
        if (cmp2 < 0 && cmp1 > 0) {
            /* Rather desperate approach (java.home should have been among
             * sun.boot.library.path)
             */
            File javawFile = null;
            String JDK_directory = System.getProperty("java.home");
            javawFile = Path.of(JDK_directory, "bin", "java").toFile();
            if (!javawFile.exists()) {
                javawFile = Path.of(JDK_directory, "bin", "javaw").toFile();
            }
            if (!javawFile.exists()) {
                javawFile = Path.of(JDK_directory, "bin", "javaw.exe").toFile();
            }
            if (javawFile.exists()) {
                foundVersion = javaVersion;
                // Okay, indeed found something better suited
                return javawFile;
            }
        }
        if (javaw != null) {
            javaVersion = javaw.getParentFile().getParentFile().getName();
        }
        if (cmp2 == -2) {
            // Unclear version info - but should we actually pester the user?
            int answer = JOptionPane.showConfirmDialog(null,
                    "Having trouble to compare required Java version " + minJavaVersion
                    + " with current Java version " + javaVersion + "."
                    + "\n\n"
                    + "Try to start " + name + " with Java " + javaVersion + " nevertheless?",
                    "Java Version Trouble",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (answer == JOptionPane.CANCEL_OPTION) {
                System.exit(1);
            }
        }
        else if (cmp2 < 0) {
            // The found version is obsolete and unsuited for the product start
            JOptionPane.showMessageDialog(null,
                        name + " requires at least Java version " + minJavaVersion + ","
                        + "\n"
                        + "but is attempted to be run with Java " + javaVersion + " only!" 
                        + "\n\n"
                        + name + " cannot be started."
                        + "\n\n"
                        + "Ensure at least Java " + minJavaVersion + " is installed or disable Java " + javaVersion + "!",
                        "Java Version Error",
                        JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        return javaw;
    }

    /**
     * Compares the numerical parts of the specified version strings hierarchically,
     * after leading and trailing text has been cut off. Returns -1 if {@code avail}
     * is "smaller" (shorter or lower) than {@code required}, 0 if both are equivalent,
     * and 1 if {@code avail} represents a higher version than {@code required}. In
     * case of parsing problems, the result will be -2.
     * 
     * @param avail - a string supposed to contain the current Java version code like
     *     "jdk-17.0.2-open10" or "1.8.0".
     * @param required - a string assumed to specify a required Java version, e.g.
     *     "11" or "17.2.1" or "jre1.8.2".
     * @return -1, 0, or 1 if the contained dot-separated number sequences could be
     *     compared, -2 otherwise.
     */
    private static int compareVersionStrings(String avail, String required)
    {
        avail = deriveFormattedVersionString(avail);
        required = deriveFormattedVersionString(required);
        if (avail.isEmpty() || required.isEmpty()) {
            return -2;
        }
        int cmp = avail.compareTo(required);
        if (cmp != 0) {
            cmp /= Math.abs(cmp);
        }
        return cmp;
    }

    /**
     * Extracts version information of the kind d.dd.ddd (with varying
     * number of digits in every section) from the given {@code verString}
     * and returns a formatted version with exactly 3 digits per section
     * for a lexicographic comparison and ordering. A leading 1 (as in
     * "1.8.0_261") will be dropped.
     * 
     * @param verString - a string assumed to contain version information.
     * @return the formatted string - might be empty if there is no version
     *     info available - requires at least  single dot.
     */
    private static String deriveFormattedVersionString(String verString)
    {
        if (VER_MATCHER.reset(verString).matches()) {
            verString = VER_MATCHER.group(2);
            String[] parts = verString.split("[._]");
            int i = 0;
            if (parts.length > 1 && parts[0].equals("1")) {
                i++;
            }
            StringBuilder sb = new StringBuilder();
            for (; i < parts.length; i++) {
                if (sb.length() > 0) {
                    sb.append(".");
                }
                int nbr = 0;
                try {
                    nbr = Integer.parseInt(parts[i]);
                }
                catch (NumberFormatException ex) {}
                sb.append(String.format("%03d", nbr));
            }
            verString = sb.toString();
        }
        else {
            verString = "";
        }
        return verString;
    }
    // END KGU#1095 2023-10-30

}

/* OLD VERSON BELOW - MERGE CONFLICT 

package lu.fisch.upla;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.JOptionPane;

public class Main {

    private static String name = "Unimozer";
    private static String program = "Unimozer.jar";
    private static String programJAR = "Unimozer.jar";
    private static String programUri = "https://unimozer.fisch.lu/webstart/"+program;
    private static String md5Uri = "https://unimozer.fisch.lu/webstart/md5.php";
    private static String iconName = "unimozer.png";
    private static int updateMode = 0;
    private static String[] args;
    
    public static void main(String[] args) throws IOException
    {
        // save args to class variable
        Main.args=args;
        
        //JOptionPane.showMessageDialog(null, buffer, "BUFFER", JOptionPane.ERROR_MESSAGE);
        //JOptionPane.showMessageDialog(null, "Starting ...", "Error", JOptionPane.ERROR_MESSAGE);

        String path = "";
        try {
            path = Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString().replace("Upla.jar", "").replace("upla.jar", "").replace("file:/", "");
            //JOptionPane.showMessageDialog(null, path, "path", JOptionPane.ERROR_MESSAGE);
        } catch (URISyntaxException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error #main 1", JOptionPane.ERROR_MESSAGE);
        }
        
        if(path.startsWith("/"))
        {
            path = "/"+path;
        }
        
        if((System.getProperty("os.name").toLowerCase().startsWith("mac os x") ||
            System.getProperty("os.name").toLowerCase().endsWith("nix") ||
            System.getProperty("os.name").toLowerCase().endsWith("nux")) &&
            !path.startsWith("/"))
        {
            path = "/"+path;
        }
        
        //JOptionPane.showMessageDialog(null, path, "PATH", JOptionPane.ERROR_MESSAGE);

        //Ini.getInstance().save(); 
        Ini.getInstance().load();
        name = Ini.getInstance().getProperty("name", "Unimozer");
        program = Ini.getInstance().getProperty("program", "Unimozer.jar");
        programJAR = program;
        programUri = Ini.getInstance().getProperty("programUri", "https://unimozer.fisch.lu/webstart/"+program);
        program = path+program;
        md5Uri = Ini.getInstance().getProperty("md5Uri", "https://unimozer.fisch.lu/webstart/md5.php");
        iconName = Ini.getInstance().getProperty("iconName", "unimozer.png");
        updateMode = Integer.valueOf(Ini.getInstance().getProperty("updateMode", "0"));
        //Ini.getInstance().save();
        
        String buffer = "";
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            buffer+=arg+" ";
        }
        if(buffer.trim().equals("/modify=1"))
        //if(true)
        {
            try {
                //JOptionPane.showMessageDialog(null, "You are now in modification mode ...", "Modification mode", JOptionPane.ERROR_MESSAGE);
                MainFrame mainFrame = new MainFrame();
                mainFrame.setMode(updateMode);
                mainFrame.setVisible(true);
                Ini.getInstance().setProperty("updateMode",String.valueOf(mainFrame.getMode()));
                Ini.getInstance().save();
                System.exit(0);
            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #main 3", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
        

        Launcher launcher = new Launcher();
        launcher.setIcon(new javax.swing.ImageIcon(launcher.getClass().getResource("/lu/fisch/upla/icons/"+iconName)));
        launcher.setName(name);
        launcher.setVisible(true);
        launcher.setLocationRelativeTo(null);
        launcher.setStatus("Loading ...");

        try 
        {
            File jar = new File(URLDecoder.decode(program,StandardCharsets.UTF_8.name()));
            //JOptionPane.showMessageDialog(null, jar, "jar", JOptionPane.ERROR_MESSAGE);
            launcher.setStatus("Testing local cache ...");
            if(!jar.exists())
            {
                if(updateMode==2)
                {
                    JOptionPane.showMessageDialog(null, "The file <"+programJAR+"> can't be found!"
                        + "\n\n"
                        + "You chose never to look for updates online so "+name+" won't be able to start right now."
                        + "\n\n"
                        + "You may want to modify your installation and switch to another update mode ...", "Error", JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                }
                else
                {
                    launcher.setStatus("Testing network ...");
                    if(isOnline())
                    {
                        if(updateMode==1)
                        {
                            int res = JOptionPane.showConfirmDialog(null, "The file <"+programJAR+"> can't be found!"
                                + "\n\n"
                                + "Do you want to download it now? ("+name+" will quit otherwise ...)", "Error", JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
                            if(res==JOptionPane.NO_OPTION)
                                System.exit(1);
                        }
                        
                        launcher.setStatus("Downloading ...");
                        //launcher.setStatus(program);
                        download();
                        launcher.setStatus("Starting application ...");
                        start();
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(null, "The server can't be reached.\nPlease make sure you have an active internet connection ...", "Error", JOptionPane.ERROR_MESSAGE);
                        System.exit(1);
                    }
                }
            }
            else
            {
                if(updateMode!=2)
                {
                    if(isOnline())
                    {
                        if(!getLocalMD5().equals(getRemoteMD5()))
                        {
                            boolean download = true;
                            if(updateMode==1)
                            {
                                int res = JOptionPane.showConfirmDialog(null, "An update is available!"
                                    + "\n\n"
                                    + "Do you want to download it now?", "Information", JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
                                if(res==JOptionPane.NO_OPTION)
                                    download=false;
                            }
                            if(download)
                            {
                                //launcher.setStatus(getLocalMD5()+" - "+getRemoteMD5());
                                launcher.setStatus("Downloading ... ");
                                try {
                                    download();
                                }
                                catch (Exception ex) {
                                    if (JOptionPane.showConfirmDialog(null, 
                                            "The download of the new version of " + program + " failed:\n" + ex
                                            + "\n\nDo you want to start the recent version instead?", "Error",
                                            JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE) != JOptionPane.YES_OPTION) {
                                        JOptionPane.showMessageDialog(null, "Launching " + name + " aborted.", "Error #download", JOptionPane.ERROR_MESSAGE);
                                        System.exit(1);
                                    };
                                }
                            }
                        }
                    }
                }
                launcher.setStatus("Starting application ...");
                start();
            }
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error #main 2", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        // make sure we terminate the process
        System.exit(0);
    }
    
    private static boolean isOnline()
    {
        try {
            final URL url = new URL(md5Uri);
            url.openStream();
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
    
    private static String getRemoteMD5() throws MalformedURLException, IOException
    {
        // create a new trust manager that trust all certificates
        TrustManager[] trustAllCerts;
        trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // activate the new trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } 
        catch (KeyManagementException | NoSuchAlgorithmException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #getRemoteMD5", JOptionPane.ERROR_MESSAGE);
        }

        // dwonload the program
        URL url = new URL(md5Uri);
        BufferedReader in;
        in = new BufferedReader(new InputStreamReader(url.openStream()));

        String inputLine;
        String md5 = "";
        while ((inputLine = in.readLine()) != null)
        {
            md5+=inputLine;
        }
        in.close();;
        return md5.trim();
    }
    
    private static String getLocalMD5() throws NoSuchAlgorithmException, IOException
    {
        // get md5 hash of local file
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(Paths.get(URLDecoder.decode(program,StandardCharsets.UTF_8.name())))) {
            DigestInputStream dis = new DigestInputStream(is, md);
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        StringBuilder checksumSb = new StringBuilder();
        for (byte digestByte : md.digest()) {
          checksumSb.append(String.format("%02x", digestByte));
        }
        return checksumSb.toString();
    }

    private static void download() throws MalformedURLException, IOException
    {
        // create a new trust manager that trust all certificates
        TrustManager[] trustAllCerts;
        trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };

        // activate the new trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } 
        catch (KeyManagementException | NoSuchAlgorithmException ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #download", JOptionPane.ERROR_MESSAGE);
        }

        // dwonload the program
        URL url = new URL(programUri);
        URLConnection connection = url.openConnection();
        OutputStream out;
        try (InputStream in = connection.getInputStream()) {
            //System.out.println(program);
            //JOptionPane.showMessageDialog(null, program, "program", JOptionPane.ERROR_MESSAGE);
            out = new FileOutputStream(new File(URLDecoder.decode(program,StandardCharsets.UTF_8.name())));
            byte[] buffer = new byte[2048];
            int length;
            int downloaded = 0;
            while ((length = in.read(buffer)) != -1)
            {
                out.write(buffer, 0, length);
                downloaded+=length;
            }
            out.close();
        }
    }
    
    private static void start() throws IOException, InterruptedException
    {
        // find javaw
        String bin = System.getProperty("file.separator")+"bin";
        boolean found=false;

        // get boot folder
        String bootFolderList = System.getProperty("sun.boot.library.path");
        String[] bootFolders = bootFolderList.split(File.pathSeparator);
        TreeSet<String> directories = new TreeSet<String>();
        for (String bootFolder: bootFolders) {
            if (bootFolder.endsWith("bin")) {
                // go back two directories
                bootFolder=bootFolder.substring(0,bootFolder.lastIndexOf(System.getProperty("file.separator")));
                bootFolder=bootFolder.substring(0,bootFolder.lastIndexOf(System.getProperty("file.separator")));

                // get all files from the boot folder
                File bootFolderfile = new File(bootFolder);
                File[] files = bootFolderfile.listFiles();
                for (int i=0; i<files.length; i++)
                {
                    if (files[i].isDirectory())
                        directories.add(files[i].getAbsolutePath());
                }
            }
        }

        File javaw = null;
        while (directories.size()>0 && !found)
        {
            String JDK_directory = directories.last();
            directories.remove(JDK_directory);   

            javaw = new File(JDK_directory+bin+System.getProperty("file.separator")+"java");
            if(javaw.exists()) break;
            javaw = new File(JDK_directory+bin+System.getProperty("file.separator")+"javaw");
            if(javaw.exists()) break;
            javaw = new File(JDK_directory+bin+System.getProperty("file.separator")+"javaw.exe");
            if(javaw.exists()) break;
        }

        if (javaw != null)
        {
            // start it
            //System.out.println("Starting: "+javaw.getAbsolutePath()+" -jar "+program);
            if(System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
            {
                ArrayList<String> list = new ArrayList<>();
                list.add(javaw.getAbsolutePath());
                list.add("-jar");
                list.add("-Dapple.laf.useScreenMenuBar=true");
                list.add("-Dcom.apple.macos.use-file-dialog-packages=true");
                list.add("-Dcom.apple.macos.useScreenMenuBar=true");
                list.add("-Dcom.apple.smallTabs=true-Xmx1024M");
                list.add("-Dcom.apple.mrj.application.apple.menu.about.name="+name+"");
                list.add("-Dapple.awt.application.name="+name+"");
                list.add("-Xdock:name="+name);
                list.add(URLDecoder.decode(program,StandardCharsets.UTF_8.name()));
                for (int i = 0; i < args.length; i++) {
                   String arg = args[i];
                    list.add(arg);
                }
                ProcessBuilder processBuilder = new ProcessBuilder(list);
                Process process = processBuilder.start();
               //Process process = new ProcessBuilder(javaw.getAbsolutePath(),"-jar","-Dapple.laf.useScreenMenuBar=true",program).start();
            }
            else
            {
                ArrayList<String> list = new ArrayList<>();
                list.add(javaw.getAbsolutePath());
                list.add("-jar");
                list.add(URLDecoder.decode(program,StandardCharsets.UTF_8.name()));
                for (int i = 0; i < args.length; i++) {
                   String arg = args[i];
                    list.add(arg);
                }
                ProcessBuilder processBuilder = new ProcessBuilder(list);
                Process process = processBuilder.start();
            }
            try {
                // terminated this process
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error #start", JOptionPane.ERROR_MESSAGE);
            }
            // terminated this process but wait a bit
            //Thread.sleep(1*1000);
            System.exit(0);
        }
    }
}
*/