package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import zi.zircky.gtnhlauncher.utils.OperatingSystem;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativesExtractor {
  public static boolean isAllowedByRules(JsonArray rules) {
    OperatingSystem current = OperatingSystem.getCurrent();
    boolean allowed = true;
    for (JsonElement element : rules) {
      JsonObject rule = element.getAsJsonObject();
      String action = rule.get("action").getAsString();
      if (rule.has("os")) {
        JsonObject os = rule.getAsJsonObject("os");
        if (os.has("name")) {
          String name = os.get("name").getAsString();
          boolean match = switch (name) {
            case "windows" -> current == OperatingSystem.WINDOWS;
            case "osx" -> current == OperatingSystem.OSX;
            case "linux" -> current == OperatingSystem.LINUX;
            default -> false;
          };
          if (action.equals("allow") && match) allowed = true;
          if (action.equals("disallow") && match) allowed = false;
        }
      } else {
        allowed = action.equals("allow");
      }
    }
    return allowed;
  }


  /**
   * Распаковывает native-библиотеки (.dll, .so, .dylib) из jar-файлов в указанный каталог.
   *
   * @param nativesDir директория, куда распаковать natives (например, ./natives/)
   * @param libraries  список библиотек (в формате "group:artifact:version[:classifier]"), содержащих путь до jar
   * @throws IOException если произошла ошибка ввода-вывода
   */
  public static void extractNatives(File nativesDir, List<MinecraftLauncher.Library> libraries) throws IOException {
    if (!nativesDir.exists() && !nativesDir.mkdirs()) {
      throw new IOException("It was not possible to create a folder for natives: " + nativesDir.getAbsolutePath());
    }

    for (MinecraftLauncher.Library lib : libraries) {
      if (!lib.isNative()) continue;

      File jarFile = lib.getPath();
      System.out.println("Native: "+jarFile);
      if (!jarFile.exists()) {
        System.err.println("[WARN] Not Naiden Natives Jar: " + jarFile);
        continue;
      }


      try (JarFile jar = new JarFile(jarFile)) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          String name = entry.getName();

          // Только .dll / .so / .dylib и без META-INF
          if (entry.isDirectory() || name.startsWith("META-INF/")) continue;
          if (!(name.endsWith(".dll") || name.endsWith(".so") || name.endsWith(".dylib"))) continue;

          File outFile = new File(nativesDir, new File(name).getName());

          if (outFile.exists()) continue;

          try (InputStream in = jar.getInputStream(entry);
               OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
              out.write(buffer, 0, len);
            }
          }
          System.out.println("✅ Extracted: " + outFile.getName());
        }
      }
    }
  }

}
