package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MinecraftLauncher {
  public static void launch(File javaPath, int ramGb, String username, File gameDir, String version, boolean useJava17Plus) throws IOException, InterruptedException {
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
        JsonObject mmcPack = gson.fromJson(new FileReader(mmcPackFile), JsonObject.class);
        if (mmcPack != null && mmcPack.has("ccomponents")) {
          JsonArray components = mmcPack.getAsJsonArray("ccomponents");
          if (components != null) {
            for (int i = 0; i < components.size(); i++) {
              JsonObject component = components.get(i).getAsJsonObject();
              if (component != null && component.has("cachedName") && component.has(version)) {
                String name = component.get("cachedName").getAsString();
                if (name.equals("Minecraft")) {
                  minecraftVersion = component.get("version").getAsString();
                } else if (name.equals("Forge")) {
                  forgeVersion = component.get("version").getAsString();
                }
              }
            }
          } else {
            System.out.println("Ошибка: Поле 'components' в mmc-pack.json равно null.");
          }
        } else {
          System.out.println("Ошибка: Поле 'components' отсутствует в mmc-pack.json.");
        }
      } catch (IOException | com.google.gson.JsonSyntaxException e) {
        System.out.println("Ошибка при парсинге mmc-pack.json: " + e.getMessage());
      }
    } else {
      System.out.println("Предупреждение: mmc-pack.json не найден в " + mmcPackFile.getAbsolutePath() + ", используются значения по умолчанию.");
    }

    File librariesDir = new File(gameDir, "libraries");
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
        System.out.println("Предупреждение: lwjgl3ify-forgePatches.jar не найден в " + lwjglPatch.getAbsolutePath() + ", может потребоваться для Java 17+.");
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

    // Основной класс
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

    System.out.println("Запускаем Minecraft:");
    System.out.println(String.join(" ", command));

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.directory(gameDir);
    processBuilder.redirectErrorStream(true);

    File logFile = new File(gameDir, "latest.log");
    Process process = processBuilder.start();

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())); FileWriter logWriter = new FileWriter(logFile, true)) {
      String line;
      while ((line = reader.readLine()) != null) {
        System.out.println(line);
        logWriter.write(line + "\n");
      }
    }
    int exitCode = process.waitFor();
    System.out.println("Minecraft завершился с кодом: " + exitCode);
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
