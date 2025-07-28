package zi.zircky.gtnhlauncher.utils;

import java.io.File;

public class MinecraftUtils {
  private static final String fileName = ".gtnh-launcher";
  public static File getMinecraftDir() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      return new File(System.getenv("APPDATA"), fileName);
    } else if (os.contains("mac")) {
      return new File(System.getProperty("user.home"), "Library/Application Support/" + fileName);
    } else {
      return new File(System.getProperty("user.home"), fileName);
    }
  }

  public static boolean isMinecraftInstalled() {
    File mcDir = getMinecraftDir();
    return mcDir.exists() && mcDir.isDirectory() && new File(mcDir, "versions").exists();
  }

  public static File findJavaFromLauncher() {
    File mcDir = getMinecraftDir();
    File runtimeDir = new File(mcDir, "runtime");

    if (runtimeDir.exists()) {
      // Пример для Microsoft Launcher (обычно: runtime/jre-legacy/windows-x64/java.exe)
      File legacyJava = new File(runtimeDir, "jre-legacy/windows-x64/java.exe");
      if (legacyJava.exists()) {
        return legacyJava;
      }
    }
    return null;
  }
}
