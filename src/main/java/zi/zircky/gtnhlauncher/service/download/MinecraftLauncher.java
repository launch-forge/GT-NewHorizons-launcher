package zi.zircky.gtnhlauncher.service.download;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    StringBuilder classpath = new StringBuilder();
    appendLibraries(librariesDir, classpath);
    classpath.append(File.pathSeparator).append(minecraftJar.getAbsolutePath());
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

  private static void appendLibraries(File librariesDir, StringBuilder classpath) {
    if (!librariesDir.exists()) return;

    File[] files = librariesDir.listFiles();
    if (files == null) return;

    for (File file : files) {
      if (file.isDirectory()) {
        appendLibraries(file, classpath);
      } else if (file.getName().endsWith(".jar")) {
        classpath.append(file.getAbsolutePath()).append(File.pathSeparator);
      }
    }
  }
}
