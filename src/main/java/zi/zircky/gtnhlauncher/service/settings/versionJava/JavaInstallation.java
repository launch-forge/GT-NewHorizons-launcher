package zi.zircky.gtnhlauncher.service.settings.versionJava;

public class JavaInstallation {
  private final String version;
  private final String path;

  public JavaInstallation(String version, String path) {
    this.version = version;
    this.path = path;
  }

  public String getVersion() {
    return version;
  }

  public String getPath() {
    return path;
  }

  @Override
  public String toString() {
    return "JavaInstallation{" +
        "version='" + version + '\'' +
        ", path='" + path + '\'' +
        '}';
  }
}
