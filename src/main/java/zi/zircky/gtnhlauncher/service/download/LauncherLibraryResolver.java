package zi.zircky.gtnhlauncher.service.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

public class LauncherLibraryResolver {
  public void runDownload(File librariesDir, File patchesDir) throws Exception {
    Set<MavenLibrary> neededLibraries = PatchJsonParser.extractLibraries(patchesDir);

    for (MavenLibrary lib : neededLibraries) {
      File target = lib.getLocalFile(librariesDir);
      if (!target.exists()) {
        System.out.println("Downloading: " + lib);
        downloadFile(lib.getDownloadUrl(), target);
      } else {
        System.out.println("Already present: " + lib);
      }
    }

    System.out.println("✅ All libraries are in place.");
  }

  private static void downloadFile(String url, File destination) throws IOException {
    destination.getParentFile().mkdirs();  // ensure directory exists

    try (InputStream in = new URL(url).openStream();
         FileOutputStream out = new FileOutputStream(destination)) {
      byte[] buffer = new byte[8192];
      int len;
      while ((len = in.read(buffer)) != -1) {
        out.write(buffer, 0, len);
      }
    }
  }
}
