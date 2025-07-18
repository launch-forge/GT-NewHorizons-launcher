package zi.zircky.gtnhlauncher.service.settings.versionJava;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

public class JavaDetector {
  public static List<JavaInstallation> findInstalledJava() {
    Set<String> seenPaths = new HashSet<>();
    List<JavaInstallation> result = new ArrayList<>();

    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome != null) {
      String javaPath = javaHome + "/bin/java.exe";
      File javaFile = new File(javaPath);
      if (javaFile.exists() && seenPaths.add(javaPath)) {
        result.add(new JavaInstallation(
            getJavaVersion(javaPath),
            javaPath
        ));
      }

      File[] standardDirs  = {
          new File("C:/Program Files/Java"),
      new File("C:/Program Files (x86)/Java")};

      for (File dir : standardDirs) {
        if (dir.exists()) {
          for (File sub : Objects.requireNonNull(dir.listFiles())) {
            File javaExe = new File(sub, "bin/java.exe");
            String path = javaExe.getAbsolutePath();
            if (javaExe.exists() && seenPaths.add(path)) {
              result.add(new JavaInstallation(getJavaVersion(path), path));
            }
          }
        }
      }

      scanRecursiveForJava(new File("C:/"), seenPaths, result);
    }

    return result;
  }

  private static void scanRecursiveForJava(File dir, Set<String> seenPaths, List<JavaInstallation> result) {
    if (dir == null || !dir.exists() || !dir.isDirectory()) return;

    File[] files = dir.listFiles();
    if (files == null) return;

    for (File file : files) {
      try {
        if (file.isDirectory()) {
          String name = file.getName().toLowerCase();
          if (name.equals("windows") || name.equals("programdata") || name.equals("$recycle.bin")) continue;

          scanRecursiveForJava(file, seenPaths, result);
        } else  if (file.getName().equalsIgnoreCase("java.exe")) {
          String path = file.getAbsolutePath();
          if (seenPaths.add(path)) {
            String version = getJavaVersion(path);
            if (!version.equals("Неизвестно")) {
              result.add(new JavaInstallation(version, path));
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static String getJavaVersion(String javaPath) {
    try {
      Process process = new ProcessBuilder(javaPath, "-version")
          .redirectErrorStream(true)
          .start();

      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String versionLine = reader.readLine();
      process.waitFor();

      if (versionLine != null && versionLine.contains("\"")) {
        return versionLine.split("\"")[1];
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "Неизвестно";
  }

}
