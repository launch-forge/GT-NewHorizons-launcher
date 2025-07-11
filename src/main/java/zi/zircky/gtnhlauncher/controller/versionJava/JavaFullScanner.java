package zi.zircky.gtnhlauncher.controller.versionJava;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class JavaFullScanner {
  private final File rootDir;
  private final Set<String> seenPaths = ConcurrentHashMap.newKeySet();
  private final List<JavaInstallation> results = Collections.synchronizedList(new ArrayList<>());
  private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  public JavaFullScanner(File rootDir) {
    this.rootDir = rootDir;
  }

  public List<JavaInstallation> getResults() {
    return results;
  }

  public void scanAll(Consumer<String> onUpdate, Consumer<JavaInstallation> onFound) {
    onUpdate.accept("\uD83D\uDD0D Быстрый поиск Java...");
    executorService.submit(() -> {
      try {
        scanDir(rootDir, onUpdate, onFound);
      } catch (Exception e) {
        onUpdate.accept("❌ Ошибка: " + e.getMessage());
      } finally {
        executorService.shutdown();
        try {
          executorService.awaitTermination(2, TimeUnit.MINUTES);
        } catch (InterruptedException interruptedException) {
          interruptedException.printStackTrace();
        }
        onUpdate.accept("✅ Поиск завершён. Найдено: " + results.size());
      }
    });
  }

  private void scanDir(File rootDir, Consumer<String> onUpdate, Consumer<JavaInstallation> onFound) {
    if (rootDir == null || !rootDir.exists() || !rootDir.isDirectory()) return;

    File[] files = rootDir.listFiles();
    if (files == null) return;

    List<File> subDirs = new ArrayList<>();

    for (File file : files) {
      if (file.isDirectory()) {
        String name = file.getName().toLowerCase();
        if (Set.of("windows", "programdata", "$recycle.bin", "system volume information").contains(name)) continue;
        subDirs.add(file);
      } else if (file.getName().equalsIgnoreCase("java.exe")) {
        String path = file.getAbsolutePath();
        if (seenPaths.add(path)) {
          String version = getJavaversion(path);
          if (!version.equals("Неизвестно")) {
            JavaInstallation java = new JavaInstallation(version, path);
            results.add(java);
            onFound.accept(java);
            onUpdate.accept("✔ Найдена Java: " + version + " — " + path);
          }
        }
      }
    }
    for (File subDir : subDirs) {
      executorService.submit(() -> scanDir(subDir, onUpdate, onFound));
    }
  }

  private String getJavaversion(String javaPath) {
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


}
