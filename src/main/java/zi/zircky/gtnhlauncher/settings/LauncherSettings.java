package zi.zircky.gtnhlauncher.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class LauncherSettings {
  private static final Path COMGIH_PATH = Path.of("launcher_config.json");
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private String javaPath;
  private int allocatedRam;

  public static LauncherSettings load() {
    if (Files.exists(COMGIH_PATH)) {
      try (Reader reader = Files.newBufferedReader(COMGIH_PATH)) {
        return gson.fromJson(reader, LauncherSettings.class);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    LauncherSettings defaults = new LauncherSettings();
    defaults.javaPath = "";
    defaults.allocatedRam = 4;
    return defaults;
  }

  public void save() {
    try (Writer writer = Files.newBufferedWriter(COMGIH_PATH)) {
      gson.toJson(this, writer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getJavaPath() {
    return javaPath;
  }
  public void setJavaPath(String javaPath) {
    this.javaPath = javaPath;
  }

  public int getAllocatedRam() {
    return allocatedRam;
  }

  public void setAllocatedRam(int allocatedRam) {
    this.allocatedRam = allocatedRam;
  }

}
