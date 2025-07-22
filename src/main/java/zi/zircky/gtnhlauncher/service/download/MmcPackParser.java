package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MmcPackParser {
  public static class MmcPackInfo {
    public final String minecraftVersion;
    public final String forgeVersion;
    public boolean java17Mode;
    public final List<String> librariesToAdd;

    public MmcPackInfo(String minecraftVersion, String forgeVersion, boolean java17Mode, List<String> librariesToAdd) {
      this.minecraftVersion = minecraftVersion;
      this.forgeVersion = forgeVersion;
      this.java17Mode = java17Mode;
      this.librariesToAdd = librariesToAdd;
    }
  }

  public static MmcPackInfo parse(String filePath) throws IOException {
    JsonElement rootElement = JsonParser.parseReader(new FileReader(filePath));
    JsonObject root = rootElement.getAsJsonObject();
    JsonArray components = root.getAsJsonArray("components");

    String minecraftVersion = null;
    String forgeVersion = null;
    boolean java17Mode = false;
    List<String> librariesToAdd = new ArrayList<>();

    for (JsonElement element : components) {
      JsonObject component = element.getAsJsonObject();
      String uid = component.get("uid").getAsString();
      String version = component.has("version") ? component.get("version").getAsString() : null;

      librariesToAdd.add(uid);

      if ("net.minecraft".equals(uid)) {
        minecraftVersion = version;
      } else if ("net.minecraftforge".equals(uid)) {
        forgeVersion = version;
      } else if ("org.lwjgl3".equals(uid)) {
        java17Mode = true;
      }
    }

    return new MmcPackInfo(minecraftVersion, forgeVersion, java17Mode, librariesToAdd);
  }
}
