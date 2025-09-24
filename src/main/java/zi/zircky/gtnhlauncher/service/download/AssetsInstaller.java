package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class AssetsInstaller {
  private static final String INDEX_URL = "https://piston-meta.mojang.com/mc/assets/legacy/c0fd82e8ce9fbc93119e40d96d5a4e62cfa3f729/legacy.json";
  private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";

  /**
   * Устанавливает ассеты (legacy.json + файлы)
   *
   * @param assetsDir папка .gtnh-launcher/assets
   */

  public static void installAssets(File assetsDir) throws IOException {
    File indexesDir = new File(assetsDir, "indexes");
    File objectsDir = new File(assetsDir, "objects");

    indexesDir.mkdirs();
    objectsDir.mkdirs();

    File indexFile = new File(indexesDir, "legacy.json");

    // 1. Скачиваем индекс, если его нет
    if (!indexFile.exists()) {
      System.out.println("⏬ Скачиваю legacy.json...");
      try (InputStream in = new URL(INDEX_URL).openStream()) {
        Files.copy(in, indexFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      }
    } else {
      System.out.println("✓ legacy.json уже есть");
    }

    // 2. Парсим JSON
    JsonObject index;
    try (Reader reader = new FileReader(indexFile)) {
      index = JsonParser.parseReader(reader).getAsJsonObject();
    }

    JsonObject objects = index.getAsJsonObject("objects");
    int total = objects.size();
    int count = 0;

    // 3. Скачиваем недостающие ассеты
    for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
      count++;
      JsonObject obj = entry.getValue().getAsJsonObject();
      String hash = obj.get("hash").getAsString();
      int size = obj.get("size").getAsInt();

      String subDir = hash.substring(0, 2);
      File target = new File(objectsDir, subDir + "/" + hash);

      if (target.exists() && target.length() == size) {
        continue; // файл уже есть
      }

      target.getParentFile().mkdirs();
      String url = RESOURCES_URL + subDir + "/" + hash;
      System.out.println("⏬ [" + count + "/" + total + "] " + entry.getKey());

      try (InputStream in = new URL(url).openStream()) {
        Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        System.err.println("⚠ Ошибка скачивания: " + url);
      }
    }

    System.out.println("✅ Assets установлены в " + assetsDir.getAbsolutePath());
  }
}
