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
    private static String programUri = "https://unimozer.fisch.lu/webstart/"+program;
    private static String md5Uri = "https://unimozer.fisch.lu/webstart/md5.php";
    private static String iconName = "unimozer.png";
    private static String[] args;
    
    public static void main(String[] args) throws IOException
    {
        String buffer = "";
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            buffer+=arg+" ";
        }
        if(buffer.trim().equals("/modify=1"))
        {
            JOptionPane.showMessageDialog(null, "You are now in modification mode ...", "Modification mode", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        
        // save to class variable
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
        programUri = Ini.getInstance().getProperty("programUri", "https://unimozer.fisch.lu/webstart/"+program);
        program = path+program;
        md5Uri = Ini.getInstance().getProperty("md5Uri", "https://unimozer.fisch.lu/webstart/md5.php");
        iconName = Ini.getInstance().getProperty("iconName", "unimozer.png");
        //Ini.getInstance().save();

        Launcher launcher = new Launcher();
        launcher.setIcon(new javax.swing.ImageIcon(launcher.getClass().getResource("/lu/fisch/upla/icons/"+iconName)));
        launcher.setName(name);
        launcher.setVisible(true);
        launcher.setLocationRelativeTo(null);
        launcher.setStatus("Loading ...");

        try 
        {
            File jar = new File(program);
            launcher.setStatus("Testing local cache ...");
            if(!jar.exists())
            {
                launcher.setStatus("Testing network ...");
                if(isOnline())
                {
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
            else
            {
                if(isOnline())
                {
                    if(!getLocalMD5().equals(getRemoteMD5()))
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
        try (InputStream is = Files.newInputStream(Paths.get(program))) {
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
        }
        out.close();
    }
    
    private static void start() throws IOException, InterruptedException
    {
        // find javaw
        String bin = System.getProperty("file.separator")+"bin";
        boolean found=false;

        // get boot folder
        String bootFolder = System.getProperty("sun.boot.library.path");
        // go back two directories
        bootFolder=bootFolder.substring(0,bootFolder.lastIndexOf(System.getProperty("file.separator")));
        bootFolder=bootFolder.substring(0,bootFolder.lastIndexOf(System.getProperty("file.separator")));

        // get all files from the boot folder
        File bootFolderfile = new File(bootFolder);
        File[] files = bootFolderfile.listFiles();
        TreeSet<String> directories = new TreeSet<String>();
        for(int i=0;i<files.length;i++)
        {
            if(files[i].isDirectory()) directories.add(files[i].getAbsolutePath());
        }

        File javaw = null;
        while(directories.size()>0 && found==false)
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

        if(javaw!=null)
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
