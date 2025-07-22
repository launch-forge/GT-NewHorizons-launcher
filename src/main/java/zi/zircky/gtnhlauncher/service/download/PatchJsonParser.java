package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PatchJsonParser {
  public static Set<MavenLibrary> extractLibraries(File patchesDir) {
    Set<MavenLibrary> libraries = new HashSet<>();
    Gson gson = new Gson();

    for (File jsonFile : Objects.requireNonNull(patchesDir.listFiles((d, n) -> n.endsWith(".json")))) {
      try (FileReader reader = new FileReader(jsonFile)) {
        JsonObject root = gson.fromJson(reader, JsonObject.class);
        JsonArray libs = root.getAsJsonArray("libraries");
        if (libs != null) {
          for (JsonElement elem : libs) {
            JsonObject libObj = elem.getAsJsonObject();
            if (libObj.has("name")) {
              String notation = libObj.get("name").getAsString();
              libraries.add(new MavenLibrary(notation));
            }
          }
        }
      } catch (Exception e) {
        System.err.println("❌ Ошибка в " + jsonFile.getName() + ": " + e.getMessage());
      }
    }

    return libraries;
  }
}
