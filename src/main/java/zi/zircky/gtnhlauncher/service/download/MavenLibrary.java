package zi.zircky.gtnhlauncher.service.download;

import java.io.File;

public class MavenLibrary {
  private final String groupId;
  private final String artifactId;
  private final String version;

  public MavenLibrary(String notation) {
    String[] parts = notation.split(":");
    if (parts.length != 3)
      throw new IllegalArgumentException("Invalid notation: " + notation);
    this.groupId = parts[0];
    this.artifactId = parts[1];
    this.version = parts[2];
  }

  public String getPath() {
    return groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
  }

  public String getDownloadUrl() {
    return "https://maven.minecraftforge.net/" + getPath();  // или другой репозиторий
  }

  public File getLocalFile(File librariesDir) {
    return new File(librariesDir, getPath());
  }

  @Override
  public String toString() {
    return groupId + ":" + artifactId + ":" + version;
  }
}
