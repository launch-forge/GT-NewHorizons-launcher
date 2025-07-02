package zi.zircky.gtnhlauncher.service.download;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MinecraftLauncher {
  public static void launch(File javaPath, int ramGb, String username, File gameDir, String version) throws IOException {
    File librariesDir = new File(gameDir, "libraries");
    File nativesDir = new File(gameDir, "versions/" + version + "/" + version + "-natives"); // создается позже
    File minecraftJar = new File(gameDir, "versions/" + version + "/" + version + ".jar");

    if (!minecraftJar.exists()) {
      throw new IOException("Minecraft JAR не найден: " + minecraftJar.getAbsolutePath());
    }

    List<String> command = new ArrayList<>();
    command.add(javaPath.getAbsolutePath());
    command.add("-Xmx" + ramGb + "G");
    command.add("-Xms" + Math.min(ramGb, 2) + "G");
    command.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
    command.add("-cp");

    // classpath: libraries + jar
    Set<String> classpathEntries = new HashSet<>();
    appendLibraries(librariesDir, classpathEntries);
    classpathEntries.add(minecraftJar.getAbsolutePath());

    String classpath = String.join(File.pathSeparator, classpathEntries);
    command.add("-cp");
    command.add(classpath);

    command.add(classpath.toString());

    command.add("net.minecraft.client.main.Main");

    // Аргументы Minecraft
    command.add("--username"); command.add(username);
    command.add("--version"); command.add(version);
    command.add("--gameDir"); command.add(gameDir.getAbsolutePath());
    command.add("--assetsDir"); command.add(new File(gameDir, "assets").getAbsolutePath());

    System.out.println("Запускаем Minecraft:");
    System.out.println(String.join(" ", command));

    new ProcessBuilder(command)
        .directory(gameDir)
        .inheritIO()
        .start();
  }

  private static void appendLibraries(File librariesDir, Set<String> classpathEntries) {
    if (librariesDir == null || !librariesDir.exists()) return;
    for (File file : Objects.requireNonNull(librariesDir.listFiles())) {
      if (file.isDirectory()) {
        appendLibraries(file, classpathEntries);
      } else if (file.getName().endsWith(".jar")) {
        classpathEntries.add(file.getAbsolutePath());
      }
    }
  }
}
