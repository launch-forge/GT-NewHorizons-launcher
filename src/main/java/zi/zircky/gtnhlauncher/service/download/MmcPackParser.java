package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MmcPackParser {
  public static class Component {
    public String uid;
    public String version;
    public String cachedName;

    @Override
    public String toString() {
      return uid + ":" + version;
    }
  }

  public static List<Component> loadComponents(File mmcPackJson) throws IOException {
    try (Reader reader = new InputStreamReader(new FileInputStream(mmcPackJson), "UTF-8")) {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
      JsonArray comps = root.getAsJsonArray("components");

      List<Component> result = new ArrayList<>();
      for (JsonElement el : comps) {
        JsonObject obj = el.getAsJsonObject();
        Component comp = new Component();
        comp.uid = obj.get("uid").getAsString();
        comp.version = obj.has("version") ? obj.get("version").getAsString() : "";
        comp.cachedName = obj.has("cachedName") ? obj.get("cachedName").getAsString() : comp.uid;
        result.add(comp);
      }

      return result;
    }
  }

  public static List<File> resolveComponentJsonFiles(File patchesDir, List<Component> components) {
    List<File> result = new ArrayList<>();
    for (Component comp : components) {
      File jsonFile = new File(patchesDir, comp.uid + ".json");
      if (jsonFile.exists()) {
        result.add(jsonFile);
      } else {
        System.err.println("[WARN] Component not found in patches: " + comp.uid);
      }
    }
    return result;
  }
}
