package zi.zircky.gtnhlauncher.service.download;

import zi.zircky.gtnhlauncher.service.settings.SettingsConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MinecraftLaunchCommandBuilder {
  public static ProcessBuilder buildLaunchCommand(
      MmcPackParser.MmcPackInfo info,
      File gameDir,
      File javaExecutable,
      File librariesDir,
      String username
  ) {
    List<String> command = new ArrayList<>();

    // java executable
    command.add(javaExecutable.getAbsolutePath());

    // JVM options
    if (info.java17Mode) {
      // for Java 17+
      command.add("--add-modules");
      command.add("ALL-MODULE-PATH");
      command.add("--add-exports");
      command.add("java.base/sun.security.util=ALL-UNNAMED");
      command.add("--add-opens");
      command.add("java.base/java.lang=ALL-UNNAMED");
    }

    // memory
    command.add("-Xmx" + SettingsConfig.load().getAllocatedRam() + "G");
    command.add("-Xms" + Math.min(SettingsConfig.load().getAllocatedRam(), 2) + "G");

    // classpath
    StringBuilder classpath = new StringBuilder();
    for (String uid : info.librariesToAdd) {
      File jar = findJarFile(librariesDir, uid);
      if (jar != null && jar.exists()) {
        classpath.append(jar.getAbsolutePath()).append(File.pathSeparator);
      }
    }

    // bootstrap/launch class
    String mainClass;
    if (info.java17Mode) {
      mainClass = "cpw.mods.bootstraplauncher.BootstrapLauncher";
    } else {
      mainClass = "net.minecraft.launchwrapper.Launch";
    }

    // add classpath and main class
    command.add("-cp");
    command.add(classpath.toString());
    command.add(mainClass);

    // Minecraft launch args
    command.add("--username");
    command.add(username);
    command.add("--version");
    command.add(info.minecraftVersion);
    command.add("--gameDir");
    command.add(gameDir.getAbsolutePath());
    command.add("--assetsDir");
    command.add(new File(gameDir, "assets").getAbsolutePath());

    // Optional tweak class (used in Forge)
    command.add("--tweakClass");
    command.add("net.minecraftforge.fml.common.launcher.FMLTweaker");

    // Return ProcessBuilder
    return new ProcessBuilder(command)
        .directory(gameDir)
        .inheritIO(); // передаёт stdout/stderr текущему процессу
  }

  private static File findJarFile(File librariesDir, String uid) {
    // Пример: net.minecraftforge → net/forge/forge/...
    String[] parts = uid.split("\\.");
    File current = librariesDir;
    for (String part : parts) {
      current = new File(current, part);
    }

    if (!current.exists() || !current.isDirectory()) return null;

    // Ищем самый свежий .jar
    File[] subdirs = current.listFiles(File::isDirectory);
    if (subdirs == null || subdirs.length == 0) return null;

    File latest = subdirs[0];
    for (File dir : subdirs) {
      if (dir.getName().compareTo(latest.getName()) > 0) {
        latest = dir;
      }
    }

    for (File f : latest.listFiles()) {
      if (f.getName().endsWith(".jar")) {
        return f;
      }
    }

    return null;
  }
}
