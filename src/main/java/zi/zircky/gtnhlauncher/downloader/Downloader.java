package zi.zircky.gtnhlauncher.downloader;

import java.io.*;
import java.net.URL;

public class Downloader {
  public static void downloadFile(URL url, File destination, String expectedSha1) throws IOException {
    if (destination.exists()) {
      String currentSha1 = FileUtils.calculateSha1(destination);
      if (currentSha1.equalsIgnoreCase(expectedSha1)) {
        System.out.println("[✓] Уже скачан: " + destination.getName());
      }
    }

    destination.getParentFile().mkdirs();
    System.out.println("→ Скачиваем " + " в " + destination.getPath());

    try (InputStream in = url.openStream(); OutputStream out = new FileOutputStream(destination)) {
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
    }

    String finalSha1 = FileUtils.calculateSha1(destination);
    if (!finalSha1.equalsIgnoreCase(expectedSha1)) {
      throw new IOException("Файл повреждён: " + destination.getName());
    }
  }
}
