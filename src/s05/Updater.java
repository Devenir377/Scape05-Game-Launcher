package s05;

import sign.Signlink;

import javax.swing.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.CRC32;

public class Updater implements Runnable {
  
  private static final String PROPERTIES_URL = "https://gcache.2005.rs/client.properties";
  private static final String CODE_JAR = "lib/code.jar";
  private static final String REVISION_FILE = "revision.txt";
  private static final String LIB_DIR = "lib/";
  private static final int VERIFY_PERCENT = 70;
  private static final int COMPLETE_PERCENT = 100;
  
  JFrame frame;
  Progress progress;
  int state;
  
  private Updater() {
    this.progress = new Progress();
    this.progress.bar.setUI(new BasicProgressBarUI()
    {
      public void paint(Graphics g, JComponent c) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, c.getWidth(), c.getHeight());
        
        g.setColor(new Color(140, 17, 17));
        g.drawRect(1, 1, c.getWidth() - 3, c.getHeight() - 3);
        
        int frac = (this.progressBar.getValue() << 8) / this.progressBar.getMaximum();
        int w = (c.getWidth() - 6) * frac >> 8;
        
        g.fillRect(3, 3, w, c.getHeight() - 6);
      }
    });
    
    this.frame = new JFrame("Scape05");
    this.frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.frame.setResizable(false);
    this.frame.add(this.progress.root);
    this.frame.pack();
    this.frame.setLocationRelativeTo(null);
    this.frame.setVisible(true);
    
    Thread thread = new Thread(this);
    thread.setName("Updater");
    thread.start();
  }
  
  public static void main(String[] args) {
    System.setProperty("https.protocols", "TLSv1.2");
    
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException|InstantiationException|IllegalAccessException|javax.swing.UnsupportedLookAndFeelException ex) {
      ex.printStackTrace();
    }
    
    new Updater();
  }
  
  private void setPercent(int percent) {
    this.progress.bar.setValue(percent);
  }
  
  private void setAction(String text) {
    this.progress.action.setText(text);
  }
  
  
  public void run() {
    Properties properties = new Properties();
    
    try {
      while (this.frame.isDisplayable()) {
        switch (this.state) {
          case -1:
            clearCachedClient();
            this.state = 1;
            continue;
          
          case 0:
            fetchProperties(properties);
            break;
          
          case 1:
            downloadAndVerifyClient(properties);
            break;
          
          case 2:
            launchClient(properties);
            break;
          
          default:
            this.frame.dispose();
            return;
        }
        
        this.state++;
      }
    } catch (Exception e) {
      this.frame.dispose();
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, e, "Error", 0);
    }
  }
  
  private void clearCachedClient() throws IOException {
    Files.deleteIfExists(Signlink.getPath(CODE_JAR));
    Files.deleteIfExists(Signlink.getPath(REVISION_FILE));
  }
  
  private void fetchProperties(Properties properties) throws IOException {
    setAction("Fetching properties...");
    setPercent(10);
    
    properties.clear();
    properties.load(new ByteArrayInputStream(Signlink.download(PROPERTIES_URL)));
  }
  
  private void downloadAndVerifyClient(Properties properties) throws Exception {
    Path codePath = Signlink.getPath(CODE_JAR);
    Path revisionPath = Signlink.getPath(REVISION_FILE);
    
    byte[] clientJar = hasCachedClient(codePath, revisionPath)
                               ? Files.readAllBytes(codePath)
                               : downloadClientFiles(properties, codePath, revisionPath);
    
    setAction("Verifying...");
    setPercent(VERIFY_PERCENT);
    Thread.sleep(200L);
    
    if (!hasExpectedCrc(clientJar, properties.getProperty("crc"))) {
      this.state = -1;
      return;
    }
    
    String expectedRevision = properties.getProperty("revision");
    String actualRevision = new String(Files.readAllBytes(revisionPath)).trim();
    
    if (!actualRevision.equals(expectedRevision)) {
      this.state = -1;
    }
  }
  
  private boolean hasCachedClient(Path codePath, Path revisionPath) {
    return Files.exists(codePath) && Files.exists(revisionPath);
  }
  
  private byte[] downloadClientFiles(Properties properties, Path codePath, Path revisionPath) throws IOException {
    String baseUrl = properties.getProperty("url");
    String revision = properties.getProperty("revision");
    
    byte[] clientJar = downloadWithProgress(
            baseUrl + revision + ".jar",
            "Downloading game client..."
    );
    Files.write(codePath, clientJar, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    
    byte[] libraries = downloadWithProgress(baseUrl + "lib.zip", "Downloading libraries...");
    Signlink.unzip(libraries, Signlink.getPath(LIB_DIR));
    
    if (Boolean.parseBoolean(properties.getProperty("natives"))) {
      byte[] natives = downloadWithProgress(baseUrl + getNativeArchiveName(), "Downloading natives...");
      Signlink.unzip(natives, Signlink.getPath(LIB_DIR));
    }
    
    try (BufferedWriter writer = Files.newBufferedWriter(
            revisionPath,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
    )) {
      writer.write(revision);
    }
    
    return clientJar;
  }
  
  private byte[] downloadWithProgress(String url, String action) throws IOException {
    setAction(action + "0%");
    setPercent(0);
    
    return Signlink.download(url, percent -> {
      setPercent(percent);
      setAction(action + percent + "%");
    });
  }
  
  private String getNativeArchiveName() {
    String osName = System.getProperty("os.name").toLowerCase();
    
    if (osName.contains("win")) {
      return "win.zip";
    }
    
    if (osName.contains("mac")) {
      return "mac.zip";
    }
    
    return "nux.zip";
  }
  
  private boolean hasExpectedCrc(byte[] clientJar, String expectedCrc) {
    CRC32 crc = new CRC32();
    crc.update(clientJar);
    
    long actualCrc = crc.getValue();
    long parsedExpectedCrc = Long.parseLong(expectedCrc);
    
    if (actualCrc != parsedExpectedCrc) {
      System.out.println(actualCrc + " != " + expectedCrc);
      return false;
    }
    
    return true;
  }
  
  private void launchClient(Properties properties) throws Exception {
    setAction("Starting up...");
    setPercent(COMPLETE_PERCENT);
    
    List<URL> libraryUrls = new ArrayList<>();
    
    try (DirectoryStream<Path> libraries = Files.newDirectoryStream(Signlink.getPath(LIB_DIR), "*.jar")) {
      for (Path library : libraries) {
        libraryUrls.add(library.toUri().toURL());
      }
    }
    
    URLClassLoader classLoader = new URLClassLoader(libraryUrls.toArray(new URL[0]));
    Signlink.loader = classLoader;
    
    Class<?> mainClass = Class.forName(properties.getProperty("main-class"), true, classLoader);
    Method mainMethod = mainClass.getMethod("main", String[].class);
    
    List<String> arguments = new ArrayList<>();
    
    for (String propertyName : properties.stringPropertyNames()) {
      arguments.add("-" + propertyName);
      arguments.add(properties.getProperty(propertyName));
    }
    
    mainMethod.invoke(null, new Object[] { arguments.toArray(new String[0]) });
  }
  
}