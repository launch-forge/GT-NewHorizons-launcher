package zi.zircky.gtnhlauncher.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
  public static final File logFile = new File("launcher.log");

  public static void logs(String message) {

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
