package zi.zircky.gtnhlauncher.service.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class LibraryDownloader {

  private static final String[] REPOS = {
      "https://libraries.minecraft.net/",
      "https://repo1.maven.org/maven2/"
  };

  public static void downloadLibrary(String groupId, String artifactId, String version, File librariesDir, String classifier) throws IOException {
    String path = groupId.replace('.', '/') + "/" + artifactId + "/" + version;
    String fileName = artifactId + "-" + version + (classifier != null ? "-" + classifier : "") + ".jar";

    File target = new File(librariesDir, path + "/" + fileName);
    if (target.exists()) {
      System.out.println("✓ Library found: " + target.getPath());
      return;
    }

    target.getParentFile().mkdirs();

    IOException lastEx = null;
    for (String repo : REPOS) {
      String url = repo + path + "/" + fileName;
      System.out.println("⏬ Trying: " + url);
      try (InputStream in = new URL(url).openStream()) {
        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✓ Loaded: " + target.getPath());
        return;
      } catch (IOException e) {
        System.err.println("Loading error: " + url);
        lastEx = e;
      }
    }
    throw new IOException("Failed to download " + fileName + " from all repos", lastEx);

  }
}
