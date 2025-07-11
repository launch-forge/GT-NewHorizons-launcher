package zi.zircky.gtnhlauncher.controller.versionJava;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JavaDetector {
  public static List<JavaInstallation> findInstalledJava() {
    List<JavaInstallation> result = new ArrayList<>();

    String javaHome = System.getenv("JAVA_HOME");
    if (javaHome != null) {
      result.add(new JavaInstallation(
          getJavaVersion(javaHome + "/bin/java"),
          javaHome + "/bin/java"
      ));

      File programfiles = new File("C:/Program Files/Java");
      if (programfiles.exists()) {
        for (File dir : Objects.requireNonNull(programfiles.listFiles())) {
          File javaExe = new File(dir, "bin/java.exe");
          if (javaExe.exists()) {
            result.add(new JavaInstallation(
                getJavaVersion(javaExe.getAbsolutePath()),
                javaExe.getAbsolutePath()
            ));
          }
        }
      }
    }
    return result;
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
