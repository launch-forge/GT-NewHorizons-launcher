package zi.zircky.gtnhlauncher.utils;

public enum OperatingSystem {
  WINDOWS,
  LINUX,
  OSX,
  UNKNOWN;

  public static OperatingSystem getCurrent() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) return WINDOWS;
    if (os.contains("mac")) return OSX;
    if (os.contains("nix") || os.contains("nux") || os.contains("aix")) return LINUX;
    return UNKNOWN;
  }

  public static String getNativesClassifier() {
    OperatingSystem os = getCurrent();
    String arch = System.getProperty("os.arch").toLowerCase().contains("64") ? "64" : "32";

    return switch (os) {
      case WINDOWS -> "natives-windows" + arch;
      case LINUX -> "natives-linux";
      case OSX -> "native-osx";
      default -> "";
    };
  }
}
