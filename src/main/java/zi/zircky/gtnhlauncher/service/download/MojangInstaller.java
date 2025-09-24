package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.*;
import zi.zircky.gtnhlauncher.utils.MinecraftUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

public class MojangInstaller {
  private static final String MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public static void installVersion(String versionId, File mcDir, ProgressCallback callback) throws IOException {
    // 1. Скачать манифест версий
    JsonObject manifest = readJsonFromUrl(MANIFEST_URL).getAsJsonObject();
    JsonArray versions = manifest.getAsJsonArray("versions");

    String versionMetaUrl = null;
    for (JsonElement el : versions) {
      JsonObject obj = el.getAsJsonObject();
      if (obj.get("id").getAsString().equals(versionId)) {
        versionMetaUrl = obj.get("url").getAsString();
        break;
      }
    }

    if (versionMetaUrl == null) throw new IOException("Версия не найдена в манифесте: " + versionId);

    // 2. Скачать версию
    JsonObject versionJson = readJsonFromUrl(versionMetaUrl).getAsJsonObject();


    // 3. Скачать клиент jar
    JsonObject downloads = versionJson.get("downloads").getAsJsonObject();
    JsonObject client = downloads.get("client").getAsJsonObject();
    String clientUrl = client.get("url").getAsString();

    File versionDir = new File(MinecraftUtils.gameRoot(), "versions/" + versionId);
    versionDir.mkdirs();
    File jarFile = new File(versionDir, versionId + ".jar");
    downloadFile(clientUrl, jarFile, callback);

    // 4. Скачать библиотеки
    JsonArray libraries = versionJson.getAsJsonArray("libraries");
    File librariesDir = new File(mcDir, "libraries");
    librariesDir.mkdirs();

    for (JsonElement libEl : libraries) {
      JsonObject lib = libEl.getAsJsonObject();
      if (!lib.has("downloads")) continue;

      JsonObject downloadsObj = lib.getAsJsonObject("downloads");

      // 📦 Скачиваем обычную библиотеку, если есть
      if (downloadsObj.has("artifact")) {
        JsonObject artifact = downloadsObj.getAsJsonObject("artifact");
        String url = artifact.get("url").getAsString();
        String path = artifact.get("path").getAsString();
        File libFile = new File(librariesDir, path);
        if (!libFile.getParentFile().exists()) libFile.getParentFile().mkdirs();
        downloadFile(url, libFile, callback);
      }

      // 🪟 Также поддержка natives (опционально)
      if (downloadsObj.has("classifiers")) {
        JsonObject classifiers = downloadsObj.getAsJsonObject("classifiers");
        // Пример: скачиваем natives-windows
        for (Map.Entry<String, JsonElement> entry : classifiers.entrySet()) {
          JsonObject classifier = entry.getValue().getAsJsonObject();
          if (!classifier.has("url") || !classifier.has("path")) continue;

          String url = classifier.get("url").getAsString();
          String path = classifier.get("path").getAsString();
          File libFile = new File(librariesDir, path);
          if (!libFile.getParentFile().exists()) libFile.getParentFile().mkdirs();
          downloadFile(url, libFile, callback);
        }
      }
    }

    // 5. Готово!
    System.out.println("Minecraft " + versionId + " установлен.");
  }

  private static JsonElement readJsonFromUrl(String url) throws IOException {
    try (InputStream is = new URL(url).openStream()) {
      return JsonParser.parseReader(new InputStreamReader(is));
    }
  }

  private static void downloadFile(String urlStr, File target, ProgressCallback callback) throws IOException {
    if (target.exists()) return; // не скачиваем повторно
    URL url = new URL(urlStr);
    URLConnection conn = url.openConnection();
    int fileSize = conn.getContentLength();

    try (InputStream in = conn.getInputStream();
         FileOutputStream out = new FileOutputStream(target)) {

      byte[] buffer = new byte[4096];
      int bytesRead;
      int totalRead = 0;

      while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
        totalRead += bytesRead;
        if (fileSize > 0) {
          double progress = (double) totalRead / fileSize;
          callback.update(progress, "Скачиваем: " + target.getName());
        }
      }
    }
  }
}
