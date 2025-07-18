package zi.zircky.gtnhlauncher.service.settings.versionJava;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RecursiveTask;
import java.util.function.Consumer;

public class JavaScannerTask extends RecursiveTask<List<JavaInstallation>> {
  private final File directory;
  private final int depth;
  private final int maxDepth;
  private final Set<String> seenParhs;
  private final Consumer<String> onUpdate;
  private final Consumer<JavaInstallation> onFound;
  private final File baseDir;

  public JavaScannerTask(File directory, int depth, int maxDepth, Set<String> seenParhs, Consumer<String> onUpdate, Consumer<JavaInstallation> onFound, File baseDir) {
    this.directory = directory;
    this.depth = depth;
    this.maxDepth = maxDepth;
    this.seenParhs = seenParhs;
    this.onUpdate = onUpdate;
    this.onFound = onFound;
    this.baseDir = baseDir;
  }

  @Override
  protected List<JavaInstallation> compute() {
    List<JavaInstallation> results = new ArrayList<>();
    List<JavaScannerTask> subtasks = new ArrayList<>();

    try {
      File absDir = directory.getAbsoluteFile();
      String basePath = baseDir.getCanonicalPath();
      String dirPath = absDir.getCanonicalPath();

      if (!dirPath.startsWith(basePath)) {
        return results;
      }

    } catch (IOException e) {
      e.printStackTrace();
      return results;
    }

    System.out.println("[DEBUG] Скан: " + directory.getAbsolutePath() + " на глубине " + depth);

    if (depth > maxDepth) return results;
    if (directory == null || !directory.isDirectory() || !directory.exists()) {
      return results;
    }

    File[] files = directory.listFiles();
    if (files == null) return results;

    for (File file : files) {
      try {
        if (file.isDirectory()) {
          String absolutePath = file.getAbsolutePath().toLowerCase();
          if (isSystemDirectory(absolutePath)) continue;

          if (depth < maxDepth) {
            JavaScannerTask subtask = new JavaScannerTask(file, depth + 1, maxDepth, seenParhs, onUpdate, onFound, baseDir);
            subtask.fork();
            subtasks.add(subtask);
          }

        } else if (file.getName().equalsIgnoreCase("java.exe")) {
          String path = file.getAbsolutePath();

          if (!seenParhs.add(path)) continue;

          String version = getJavaVersion(path);
          if (!version.equals("Неизвестно")) {
            JavaInstallation installation = new JavaInstallation(version, path);
            results.add(installation);

            if (onFound != null) onFound.accept(installation);
            if (onUpdate != null) onUpdate.accept("✔ Найдена Java " + version + " — " + path);

            String color;
            if (version.startsWith("1.8")) {
              color = "\u001B[32m"; // зелёный — Java 8
            } else if (isJava17OrAbove(version)) {
              color = "\u001B[33m"; // жёлтый — Java 17+
            } else {
              color = "\u001B[0m";  // сброс цвета
            }

            System.out.println(color + "[FOUND] Java: " + path + " (версия: " + version + ", глубина: " + depth + ")" + "\u001B[0m");
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    for (JavaScannerTask subtask : subtasks) {
      results.addAll(subtask.join());
    }

    return results;

  }

  private String getJavaVersion(String javaPath) {
    try {
      Process process = new ProcessBuilder(javaPath, "-version")
          .redirectErrorStream(true).start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String versionList = reader.readLine();
      process.waitFor();

      if (versionList != null && versionList.contains("\"")) {
        return versionList.split("\"")[1];
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "Неизвестно";
  }

  private boolean isJava17OrAbove(String version) {
    try {
      if (version.startsWith("1.")) return false; // Java 8, 7 и т.д.
      int major = Integer.parseInt(version.split("\\.")[0]);
      return major >= 17;
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isSystemDirectory(String path) {
    return path.contains("\\windows")
        || path.contains("\\programdata")
        || path.contains("\\$recycle.bin")
        || path.contains("\\system volume information")
        || path.contains("\\recovery")
        || path.contains("\\pagefile.sys")
        || path.contains("\\python312")
        || path.contains("\\common files")
        || path.contains("\\apple software update")
        || path.contains("\\dolby")
        || path.contains("\\bonjour")
        || path.contains("\\crypto pro")
        || path.contains("\\finalwire")
        || path.contains("\\internet explorer")
        || path.contains("\\microsoft")
        || path.contains("\\microsoft SDKs")
        || path.contains("\\microsoft visual studio")
        || path.contains("\\microsoft.net")
        || path.contains("\\msbuild")
        || path.contains("\\mysql")
        || path.contains("\\nvidia corporation")
        || path.contains("\\razer")
        || path.contains("\\nodejs")
        || path.contains("\\git")
        || path.contains("\\gimp 2")
        || path.contains("\\all users")
        || path.contains("\\public")
        || path.contains("\\default")
        || path.contains("\\.android")
        || path.contains("\\.cache")
        || path.contains("\\.codeium")
        || path.contains("\\.config")
        || path.contains("\\.cursor")
        || path.contains("\\.ftb")
        || path.contains("\\.gradle")
        || path.contains("\\.gnupg")
        || path.contains("\\.tabnine")
        || path.contains("\\.ssh")
        || path.contains("\\.sonarlint")
        || path.contains("\\.skiko")
        || path.contains("\\sdk")
        || path.contains("\\saved games")
        || path.contains("\\packagemanagement")
        || path.contains("\\iPod")
        || path.contains("\\conf")
        || path.contains("\\legal")
        || path.contains("\\include")
        || path.contains("\\jmods")
        || path.contains("\\lib")
        || path.contains("\\local")
        || path.contains("\\")
        || path.contains("\\")

        || path.contains("\\hiberfil.sys");


  }

}
