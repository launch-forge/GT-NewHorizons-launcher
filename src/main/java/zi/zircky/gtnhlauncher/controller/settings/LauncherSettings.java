package zi.zircky.gtnhlauncher.controller.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class LauncherSettings {
  private static final Path CONFIG_PATH = Path.of("launcher_config.json");
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private String javaPath;
  private int allocatedRam;
  private int versionJava;

  public static LauncherSettings load() {
    if (Files.exists(CONFIG_PATH)) {
      try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
        return gson.fromJson(reader, LauncherSettings.class);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    // значения по умолчанию
    LauncherSettings defaults = new LauncherSettings();
    defaults.javaPath = "";
    defaults.allocatedRam = 4;
    defaults.versionJava = 8;
    return defaults;
  }

  public void save() {
    try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
      gson.toJson(this, writer);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Геттеры и сеттеры
  public String getJavaPath() { return javaPath; }
  public void setJavaPath(String javaPath) { this.javaPath = javaPath; }

  public int getAllocatedRam() { return allocatedRam; }
  public void setAllocatedRam(int allocatedRam) { this.allocatedRam = allocatedRam; }

  public int getVersionJava() {
    return versionJava;
  }

  public void setVersionJava(int versionJava) {
    this.versionJava = versionJava;
  }
}
