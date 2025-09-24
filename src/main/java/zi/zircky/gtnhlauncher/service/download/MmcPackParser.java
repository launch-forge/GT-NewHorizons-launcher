package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MmcPackParser {
  public static class Component {
    private String uid;
    private String version;
    private String cachedVersion;
    private String cachedName;
    private boolean important;
    private boolean dependencyOnly;

    @Override
    public String toString() {
      return getUid() + ":" + getVersion();
    }

    public String getUid() {
      return uid;
    }

    public void setUid(String uid) {
      this.uid = uid;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getCachedVersion() {
      return cachedVersion;
    }

    public void setCachedVersion(String cachedVersion) {
      this.cachedVersion = cachedVersion;
    }

    public String getCachedName() {
      return cachedName;
    }

    public void setCachedName(String cachedName) {
      this.cachedName = cachedName;
    }
  }

  public static List<Component> loadComponents(File mmcPackJson) throws IOException {
    try (Reader reader = new InputStreamReader(new FileInputStream(mmcPackJson), StandardCharsets.UTF_8)) {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
      JsonArray comps = root.getAsJsonArray("components");

      List<Component> result = new ArrayList<>();
      for (JsonElement el : comps) {
        JsonObject obj = el.getAsJsonObject();
        Component comp = new Component();
        comp.setUid(obj.get("uid").getAsString());
        comp.setVersion(obj.has("version") ? obj.get("version").getAsString() : "");
        comp.setCachedVersion(obj.has("cachedVersion") ? obj.get("cachedVersion").getAsString() : "");
        comp.setCachedName(obj.has("cachedName") ? obj.get("cachedName").getAsString() : comp.getUid());
        result.add(comp);
      }

      return result;

    }
  }

  public static File resolveComponentJarFile(File patchesDir, Component component) {
    if (component.version != null && !component.version.isEmpty()) {
      return new File(patchesDir, component.uid + "-" + component.version + ".jar");
    } else {
      // Если версия отсутствует — не может быть JAR
      return null; // или верни null
    }
  }

  public static List<File> resolveComponentJsonFiles(File patchesDir, List<Component> components) {
    List<File> result = new ArrayList<>();
    for (Component comp : components) {
      File jsonFile = new File(patchesDir, comp.getUid() + ".json");
      if (jsonFile.exists()) {
        result.add(jsonFile);
      } else {
        System.err.println("[WARN] Component not found in patches: " + comp.getUid());
      }
    }
    return result;
  }
}
