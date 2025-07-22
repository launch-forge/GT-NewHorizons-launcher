package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MinecraftLauncher {

  private static final File logFile = new File("launcher.log");

  public static void launch(File javaPath, int ramGb, String username, File gameDir, boolean useJava17Plus) throws IOException, InterruptedException {
    try (FileWriter fw = new FileWriter(logFile, false)) {
      fw.write(""); // очистить лог
    }
    log("=== Запуск Minecraft ===");

    File minecraftDir = new File(gameDir, ".minecraft");
    if (!minecraftDir.exists()) {
      minecraftDir = gameDir;
    }

    File mmcPackFile = new File(gameDir, "mmc-pack.json");
    String minecraftVersion = "1.7.10";
    String forgeVersion = "10.13.4.1614";

    if (mmcPackFile.exists() && mmcPackFile.isFile()) {
      try (InputStreamReader reader = new InputStreamReader(new FileInputStream(mmcPackFile), StandardCharsets.UTF_8)) {
        Gson gson = new Gson();
        JsonObject mmcPack = gson.fromJson(reader, JsonObject.class);
        JsonArray components = mmcPack.getAsJsonArray("components");

        if (components != null) {
          for (int i = 0; i < components.size(); i++) {
            JsonObject component = components.get(i).getAsJsonObject();
            String name = component.has("cachedName") ? component.get("cachedName").getAsString() : "unknown";
            String uid = component.has("uid") ? component.get("uid").getAsString() : "unknown";
            String ver = component.has("cachedVersion") ? component.get("cachedVersion").getAsString() : "unknown";
            log("Компонент: " + name + " | UID: " + uid + " | Версия: " + ver);

            if (uid.equalsIgnoreCase("Minecraft")) {
              minecraftVersion = ver;
            } else if (uid.equalsIgnoreCase("net.minecraftforge")) {
              forgeVersion = ver;
            }
          }
        } else {
          log("❗ Поле 'components' отсутствует или null.");
        }

      } catch (Exception e) {
        log("❌ Ошибка при чтении mmc-pack.json: " + e.getMessage());
      }
    } else {
      log("⚠️ mmc-pack.json не найден, используются значения по умолчанию.");
    }

    LauncherLibraryResolver launcherLibraryResolver = new LauncherLibraryResolver();

    File librariesDir = new File(gameDir, "libraries");
    try {
      launcherLibraryResolver.runDownload(librariesDir, mmcPackFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    File nativesDir = new File(minecraftDir, "versions/" + minecraftVersion + "/" + minecraftVersion + "-natives");
    File minecraftJar = new File(minecraftDir, "versions/" + minecraftVersion + "/" + minecraftVersion + ".jar");

    if (!minecraftJar.exists()) {
      throw new IOException("Minecraft JAR не найден: " + minecraftJar.getAbsolutePath());
    }
    if (!librariesDir.exists()) {
      throw new IOException("Папка libraries не найдена: " + librariesDir.getAbsolutePath());
    }
    if (!nativesDir.exists()) {
      throw new IOException("Папка natives не найдена: " + nativesDir.getAbsolutePath());
    }


    Set<String> classpathEntries = new HashSet<>();
    appendLibraries(librariesDir, classpathEntries);
    classpathEntries.add(minecraftJar.getAbsolutePath());

    if (useJava17Plus) {
      File lwjglPatch = new File(librariesDir, "lwjgl3ify-forgePatches.jar");
      if (lwjglPatch.exists()) {
        classpathEntries.add(lwjglPatch.getAbsolutePath());
      } else {
        log("⚠️ lwjgl3ify-forgePatches.jar не найден.");
      }
    }


    String classpath = String.join(File.pathSeparator, classpathEntries);

    List<String> command = new ArrayList<>();
    command.add(javaPath.getAbsolutePath());
    command.add("-Xmx" + ramGb + "G");
    command.add("-Xms" + Math.min(ramGb, 2) + "G");

    // JVM-аргументы
    if (!useJava17Plus) {
      // Оптимизации для Java 8
      command.add("-XX:+UseG1GC");
      command.add("-XX:+UnlockExperimentalVMOptions");
      command.add("-XX:G1NewSizePercent=20");
      command.add("-XX:G1ReservePercent=20");
      command.add("-XX:MaxGCPauseMillis=50");
      command.add("-XX:G1HeapRegionSize=32M");
    } else {
      // Для Java 17+ с кастомным Forge
      command.add("-Dfml.readTimeout=180");
    }

    command.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
    command.add("-cp");
    command.add(classpath);

    command.add(useJava17Plus ? "cpw.mods.fml.common.launcher.FMLTweaker" : "net.minecraft.launchwrapper.Launch");

    // Аргументы Minecraft
    command.add("--username");
    command.add(username);
    command.add("--version");
    command.add(minecraftVersion);
    command.add("--gameDir");
    command.add(gameDir.getAbsolutePath());
    command.add("--assetsDir");
    command.add(new File(gameDir, "assets").getAbsolutePath());
    command.add("--tweakClass");
    command.add(useJava17Plus ? "org.spongepowered.asm.launch.MixinTweaker" : "cpw.mods.fml.common.launcher.FMLTweaker");

    log("Команда запуска:");
    for (String arg : command) log("  " + arg);

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.directory(gameDir);
    processBuilder.redirectErrorStream(true);

    File mcLogFile = new File(gameDir, "latest.log");
    Process process = processBuilder.start();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())); FileWriter logWriter = new FileWriter(mcLogFile, true)) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
        logWriter.write(line + "\n");
      }
    }
    int exitCode = process.waitFor();
    log("=== Завершено с кодом: " + exitCode + " ===");
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

  private static void log(String message) {
    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    String line = "[" + timestamp + "] " + message;
    System.out.println(line);
    try (FileWriter fw = new FileWriter(logFile, true)) {
      fw.write(line + "\n");
    } catch (IOException e) {
      System.err.println("Ошибка записи в лог: " + e.getMessage());
    }
  }
}
