package zi.zircky.gtnhlauncher.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtils {
  public static String calculateSha1(File file) throws IOException {
    try (InputStream fis = new FileInputStream(file)) {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] buffer = new byte[8192];
      int len;
      while ((len = fis.read(buffer)) != -1) {
        md.update(buffer, 0, len);
      }
      byte[] hash = md.digest();
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-1 не поддерживается", e);
    }
  }
}
