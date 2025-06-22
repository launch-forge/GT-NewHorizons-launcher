package zi.zircky.gtnhlauncher.loading;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MinecraftLauncher {
  public static void launch(File javaPath, int ramGb, String username, File gameDir, String version) throws IOException {
    File librariesDir = new File(gameDir, "libraries");
    File nativesDir = new File(gameDir, "versions/" + version + "/" + version + "-natives");
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

    StringBuilder classpath = new StringBuilder();
    appendLibraries(librariesDir, classpath);
    classpath.append(File.pathSeparator).append(minecraftJar.getAbsolutePath());
    command.add(command.toString());

    command.add("net.minecraft.client.main.Main");

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
    File[] files = librariesDir.listFiles();
    if (files == null) return;

    for (File lib : files) {
      if (lib.isFile() && lib.getName().endsWith(".jar")) {
        classpath.append(lib.getAbsolutePath()).append(File.pathSeparator);
      }
    }
  }
}
