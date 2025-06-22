package zi.zircky.gtnhlauncher.loading;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MojangVersionList {
  private static final String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

  public static List<MinecraftVersion> fetchAvailableVersions() throws IOException {
    URL url = new URL(VERSION_MANIFEST_URL);

    try (InputStream in = url.openStream(); Reader reader = new InputStreamReader(in)) {
      JsonObject manifest = JsonParser.parseReader(reader).getAsJsonObject();
      JsonArray versions = manifest.getAsJsonArray("versions");

      List<MinecraftVersion> list = new ArrayList<>();
      for (JsonElement versionEl : versions) {
        JsonObject obj = versionEl.getAsJsonObject();
        String id = obj.get("id").getAsString();
        String urlStr = obj.get("url").getAsString();
        list.add(new MinecraftVersion(id, urlStr));
      }
      return list;
    }
  }
}
