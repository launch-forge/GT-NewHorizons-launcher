package zi.zircky.gtnhlauncher.service.gtnh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class GTNHInstaller {
  public static void installGTNH(File mcDir, String gtnhZipUrl) throws IOException {
    File tempZip = new File(mcDir, "GTNH.zip");
    File tempExtract = new File(mcDir, "GTNH_temp");

    // 1. Скачиваем сборку
    System.out.println("→ Скачивание сборки GTNH...");
    try (InputStream in = new URL(gtnhZipUrl).openStream()) {
      Files.copy(in, tempZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    // 2. Распаковка
    System.out.println("→ Распаковка GTNH...");
    ZipUtils.unzip(tempZip, tempExtract);

    // 3. Поиск корневой папки (в архиве может быть GT_New_Horizons/)
    File root = findRootFolder(tempExtract);
    if (root == null) throw new IOException("Не найдена корневая папка сборки");

    // 4. Копирование папок в .minecraft
    copyFolder(new File(root, "mods"), new File(mcDir, "mods"));
    copyFolder(new File(root, "config"), new File(mcDir, "config"));
    copyFolder(new File(root, "scripts"), new File(mcDir, "scripts"));

    // 5. Очистка
    deleteRecursive(tempZip);
    deleteRecursive(tempExtract);

    System.out.println("✅ GTNH установлена успешно.");
  }

  private static File findRootFolder(File base) {
    File[] subDirs = base.listFiles(File::isDirectory);
    if (subDirs != null && subDirs.length == 1) {
      return subDirs[0]; // GT_New_Horizons/
    }
    return base;
  }

  private static void copyFolder(File source, File dest) throws IOException {
    if (!source.exists()) return;
    Files.walk(source.toPath()).forEach(path -> {
      try {
        Path rel = source.toPath().relativize(path);
        Path target = dest.toPath().resolve(rel);
        if (Files.isDirectory(path)) {
          Files.createDirectories(target);
        } else {
          Files.createDirectories(target.getParent());
          Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  private static void deleteRecursive(File file) throws IOException {
    if (!file.exists()) return;
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        deleteRecursive(child);
      }
    }
    Files.delete(file.toPath());
  }
}
