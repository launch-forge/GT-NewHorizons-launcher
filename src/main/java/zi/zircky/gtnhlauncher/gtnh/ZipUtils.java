package zi.zircky.gtnhlauncher.gtnh;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtils {
  public static void unzip(File zipFile, File targetDir) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        File outFile = new File(targetDir, entry.getName());
        if (entry.isDirectory()) {
          outFile.mkdirs();
        } else {
          outFile.getParentFile().mkdirs();
          try (FileOutputStream fos = new FileOutputStream(outFile)) {
            zis.transferTo(fos);
          }
        }
      }
    }
  }
}
