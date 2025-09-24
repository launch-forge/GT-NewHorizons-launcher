package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static zi.zircky.gtnhlauncher.utils.Log.logs;

@Slf4j
public class MMPack {
  public static void mmcPack(File mmcPackFile, String minecraftVersion, String forgeVersion) {
    if (mmcPackFile.exists() && mmcPackFile.isFile()) {
      try (InputStreamReader reader = new InputStreamReader(new FileInputStream(mmcPackFile), StandardCharsets.UTF_8)) {
        Gson gson = new Gson();
        JsonObject mmcPack = gson.fromJson(reader, JsonObject.class);
        JsonArray components = mmcPack.getAsJsonArray("components");


        if (components != null) {
          for (int i = 0; i < components.size(); i++) {
            JsonObject component = components.get(i).getAsJsonObject();
            String name = component.has("cachedName") ? component.get("cachedName").getAsString() : "unknown";
            String uid = component.has("uid") ? component.get("uid").getAsString() : "unknown";
            String ver = component.has("cachedVersion") ? component.get("cachedVersion").getAsString() : "unknown";
            logs("Компонент: " + name + " | UID: " + uid + " | Версия: " + ver);

            if (uid.equalsIgnoreCase("Minecraft")) {
              minecraftVersion = ver;
            } else if (uid.equalsIgnoreCase("net.minecraftforge")) {
              forgeVersion = ver;
            }
          }
        } else {
          logs("❗ Поле 'components' отсутствует или null.");
        }

      } catch (Exception e) {
        logs("❌ Ошибка при чтении mmc-pack.json: " + e.getMessage());
      }
    } else {
      log.warn("⚠️ mmc-pack.json не найден, используются значения по умолчанию.");
    }
  }
}
