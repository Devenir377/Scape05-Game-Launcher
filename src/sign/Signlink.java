package sign;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Signlink {
    
    public static ClassLoader loader = ClassLoader.getSystemClassLoader();
    
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 15000;
    
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    
    static {
        Path path = getCachePath();
        
        if (!Files.exists(path)) {
            try {
                Files.createDirectory(path);
                Files.createDirectory(path.resolve("lib/"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void unzip(byte[] data, Path path) throws IOException {
        unzip(new ByteArrayInputStream(data), path);
    }
    
    public static void unzip(InputStream in, Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        
        try (ZipInputStream zis = new ZipInputStream(in)) {
            byte[] buffer = new byte[4096];
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                Path dst = path.resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(dst);
                    zis.closeEntry();
                    continue;
                }
                
                Path parent = dst.getParent();
                if (parent != null && !Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                
                try (OutputStream out = Files.newOutputStream(
                        dst,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
                    
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
                
                zis.closeEntry();
            }
        }
    }
    
    public static byte[] download(String url) throws IOException {
        return download(url, null);
    }
    
    public static byte[] download(String url, DownloadListener listener) throws IOException {
        HttpURLConnection conn = null;
        
        try {
            conn = openHttpConnection(url);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException(buildHttpError(conn, url));
            }
            
            int size = conn.getContentLength();
            
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream baos = size > 0
                                                      ? new ByteArrayOutputStream(size)
                                                      : new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int total = 0;
                int read;
                int lastPercent = -1;
                
                while ((read = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                    total += read;
                    
                    if (listener != null && size > 0) {
                        int percent = (int) ((total * 100L) / size);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            listener.onRead(percent);
                        }
                    }
                }
                
                if (listener != null) {
                    listener.onRead(100);
                }
                
                return baos.toByteArray();
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    private static HttpURLConnection openHttpConnection(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setInstanceFollowRedirects(true);
        
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setRequestProperty("Cache-Control", "no-cache");
        conn.setRequestProperty("Pragma", "no-cache");
        conn.setRequestProperty("Connection", "keep-alive");
        
        return conn;
    }
    
    private static String buildHttpError(HttpURLConnection conn, String url) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("HTTP ")
                    .append(conn.getResponseCode())
                    .append(' ')
                    .append(conn.getResponseMessage())
                    .append(" for ")
                    .append(url);
            
            String errorBody = readStream(conn.getErrorStream());
            if (!errorBody.isEmpty()) {
                sb.append(" | ").append(errorBody);
            }
        } catch (IOException e) {
            sb.append("HTTP error for ").append(url);
        }
        return sb.toString();
    }
    
    private static String readStream(InputStream in) {
        if (in == null) {
            return "";
        }
        
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            String line;
            
            while ((line = br.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            
            return sb.toString();
        } catch (IOException e) {
            return "";
        }
    }
    
    public static Path getPath(String file, Object... args) {
        return getCachePath().resolve(
                String.format(file, args).toLowerCase().replace(" ", "_")
        );
    }
    
    public static Path getCachePath() {
        return Paths.get(System.getProperty("user.home"), ".scape1/");
    }
    
    public interface DownloadListener {
        void onRead(int percent);
    }
}