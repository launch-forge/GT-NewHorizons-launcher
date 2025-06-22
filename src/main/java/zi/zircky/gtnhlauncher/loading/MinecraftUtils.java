package zi.zircky.gtnhlauncher.loading;

import java.io.File;

public class MinecraftUtils {
  public static File getMinecraftDir() {
    String os = System.getProperty("os.name").toLowerCase();
    if (os.contains("win")) {
      return new File(System.getenv("APPDATA"), ".gtnh_minecraft");
    } else if (os.contains("mac")) {
      return new File(System.getProperty("user.home"), "Library/Application Support/minecraft");
    } else {
      return new File(System.getProperty("user.home"), ".minecraft");
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
      File legacyJava = new File(runtimeDir, "jre-legacy/windows-x64/java.exe");
      if (legacyJava.exists()) {
        return legacyJava;
      }
    }
    return null;
  }
}
