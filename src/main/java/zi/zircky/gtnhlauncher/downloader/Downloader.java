package zi.zircky.gtnhlauncher.downloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class Downloader {
  public static void downloadFile(URL url, File destination, String expectedSha1) throws IOException {
    if (destination.exists()) {
      String currentSha1 = FileUtils.
    }
  }
}
