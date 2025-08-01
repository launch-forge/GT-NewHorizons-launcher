package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import zi.zircky.gtnhlauncher.utils.MinecraftUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;


public class MinecraftLauncher {
  private static final Logger logger = Logger.getLogger(MinecraftLauncher.class.getName());
  private static final String LIBRARIES = "libraries";
  private static final File mcDir = MinecraftUtils.getMinecraftDir();
  private static final File mmcPack = new File(mcDir, "mmc-pack.json");
  private static final File PATCHES_DIR = new File(mcDir, "patches");
  private static final File LIBRARIES_DIR = new File(mcDir, LIBRARIES);

  private MinecraftLauncher() {
    throw new IllegalStateException("Minecraft Launcher");
  }

  public static class Library {
    String name;
    String url;
    String sha1;
    long size;
    boolean hasArtifact;

    public Library(String name, String url, String sha1, long size) {
      this.name = name;
      this.url = url;
      this.sha1 = sha1;
      this.size = size;
      this.hasArtifact = (url != null && !url.isEmpty());
    }

    public boolean isNative() {
      return name.contains(":natives");
    }

    public File getPath() {
      String[] parts = name.split(":");
      if (parts.length < 3) return null;

      String path = String.join("/", parts[0].replace(".", "/"), parts[1], parts[2]);
      String fileName = parts[1] + "-" + parts[2] + ".jar";
      return new File(LIBRARIES_DIR, path + "/" + fileName);
    }

    public String getName() {
      return name;
    }
  }

  public static ProcessBuilder launch(File javaPath, int ramGb, File gameDir, String username, String uuid, String accessToken, boolean useJava17Plus) throws IOException {

    List<MmcPackParser.Component> components = MmcPackParser.loadComponents(mmcPack);
    List<File> patchFiles = MmcPackParser.resolveComponentJsonFiles(PATCHES_DIR, components);

    List<JsonObject> allJsons = new ArrayList<>();
    for (File file : patchFiles) {
      try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        allJsons.add(json);
        logger.info("Parsed patch file: " + file.getAbsolutePath());
      } catch (IOException e) {
        logger.warning("Failed to parse patch file: " + file.getAbsolutePath() + " - " + e.getMessage());
      }
    }

    List<String> jvmArgs = collectJvmArgs(allJsons);

    if (useJava17Plus) {
      jvmArgs.add("-Dfile.encoding=UTF-8");
      jvmArgs.add("-Djava.system.class.loader=com.gtnewhorizons.retrofuturabootstrap.RfbSystemClassLoader");
    }

    String mainClass = resolveMainClass(allJsons);
    List<Library> libraries = collectLiberies(allJsons);

    if (useJava17Plus) {
      String lwjgl3ifyVersion = getlwjgl3ifyVersion(useJava17Plus);

      File lwjgl3ifyJar = new File(LIBRARIES_DIR, "lwjgl3ify-" + lwjgl3ifyVersion + "-forgePatches.jar");

      if (lwjgl3ifyJar.exists()) {
        libraries.add(new Library("me.eigenraven:lwjgl3ify:"+lwjgl3ifyVersion+":forgePatches", null, null, 0));
        logger.info("✅ Added lwjgl3ify-"+lwjgl3ifyVersion+"-forgePatches.jar: " + lwjgl3ifyJar.getAbsolutePath());
      } else {
        logger.warning("❌ lwjgl3ify-"+lwjgl3ifyVersion+"-forgePatches.jar not found at: " + lwjgl3ifyJar.getAbsolutePath());
      }
    }

    List<String> classpath = downloadAndBuildClasspath(libraries, useJava17Plus);

    String minecraftVersion = components.stream()
        .filter(c -> c.getUid().equals("net.minecraft") || c.getUid().equals("Minecraft with LWJGL3"))
        .findFirst()
        .map(useJava17Plus ? MmcPackParser.Component::getCachedVersion : MmcPackParser.Component::getVersion)
        .orElse("1.7.10");

    File nativesDir = new File(gameDir, "natives");
    NativesExtractor.extractNatives(nativesDir, libraries);

    jvmArgs.add("-Djava.library.path=" + nativesDir.getAbsolutePath());


    String mcArgs = resolverMinecraftArgs(allJsons);
    mcArgs = mcArgs
        .replace("${auth_player_name}", username)
        .replace("${auth_uuid}", uuid)
        .replace("${auth_access_token}", accessToken)
        .replace("${version_name}", minecraftVersion)
        .replace("${game_directory}", gameDir.getAbsolutePath() + "/.minecraft")
        .replace("${assets_root}", new File(gameDir, "assets").getAbsolutePath())
        .replace("${assets_index_name}", minecraftVersion)
        .replace("${user_properties}", "{}")
        .replace("${user_type}", "legacy");

    List<String> command = new ArrayList<>();
    command.add(javaPath.getAbsolutePath());
    command.add("-Xmx" + ramGb + "G");
    command.add("-Xms" + Math.min(ramGb, 2) + "G");
    command.addAll(jvmArgs);
    command.add("-cp");
    command.add(String.join(File.pathSeparator, classpath));
    command.add(mainClass);
    command.addAll(Arrays.asList(mcArgs.split(" ")));
    command.add("--tweakClass");
    command.add(useJava17Plus ? "cpw.mods.fml.common.launcher.FMLTweaker" : "net.minecraft.launchwrapper.Launch");

    logger.info("Test commands: " + command);
    return new ProcessBuilder(command).directory(gameDir);
  }

  private static List<JsonObject> loadAllJson() throws IOException {
    List<MmcPackParser.Component> components = MmcPackParser.loadComponents(mmcPack);
    List<File> patchFiles = MmcPackParser.resolveComponentJsonFiles(PATCHES_DIR, components);

    List<JsonObject> result = new ArrayList<>();
    for (File file : patchFiles) {
      try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
        result.add(JsonParser.parseReader(reader).getAsJsonObject());
      }
    }
    return result;
  }

  private static List<String> collectJvmArgs(List<JsonObject> jsonObjects) {
    List<String> result = new ArrayList<>();
    for (JsonObject jsonObject : jsonObjects) {
      if (jsonObject.has("+jvmArgs")) {
        JsonArray arr = jsonObject.getAsJsonArray("+jvmArgs");
        for (JsonElement jsonElement : arr) result.add(jsonElement.getAsString());
      }
    }
    return result;
  }

  private static String resolveMainClass(List<JsonObject> jsonObjects) {
    return jsonObjects.stream()
        .filter(jsonObject -> jsonObject.has("mainClass"))
        .max(Comparator.comparingInt(o -> o.has("order") ? o.get("order").getAsInt() : 0))
        .map(obj -> obj.get("mainClass").getAsString())
        .orElse("net.minecraft.client.main.Main");
  }

  private static String resolverMinecraftArgs(List<JsonObject> jsonObjects) {
    return jsonObjects.stream()
        .filter(jsonObject -> jsonObject.has("minecraftArguments"))
        .map(obj -> obj.get("minecraftArguments").getAsString())
        .findFirst()
        .orElse("");
  }

  private static List<Library> collectLiberies(List<JsonObject> jsonObjects) {
    List<Library> result = new ArrayList<>();
    for (JsonObject jsonObject : jsonObjects) {
      if (jsonObject.has(LIBRARIES)) {
        for (JsonElement element : jsonObject.getAsJsonArray(LIBRARIES)) {
          JsonObject libObj = element.getAsJsonObject();
          String name = libObj.get("name").getAsString();

          if (libObj.has("downloads")) {
            JsonObject downloads = libObj.getAsJsonObject("downloads");
            if (downloads.has("artifact")) {
              JsonObject art = downloads.getAsJsonObject("artifact");
              String url = art.get("url").getAsString();
              String sha1 = art.get("sha1").getAsString();
              long size = art.get("size").getAsLong();
              result.add(new Library(name, url, sha1, size));
            }
          } else if (libObj.has("MMC-hint") && libObj.get("MMC-hint").getAsString().equals("local")) {
            result.add(new Library(name, null, null, 0));
          }
        }
      }
    }
    return result;
  }

  private static List<String> downloadAndBuildClasspath(List<Library> libraries, boolean useJava17Plus) throws IOException {
    List<String> result = new ArrayList<>();

    for (Library lib : libraries) {
      File file = lib.getPath();
      if (!file.exists() && lib.hasArtifact) {
        file.getParentFile().mkdirs();
        logger.info("⬇ Downloading: " + file.getName());
        try (InputStream in = new URL(lib.url).openStream()) {
          Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
      }
      result.add(file.getAbsolutePath());
    }

    String lwjgl3ifyVersion = getlwjgl3ifyVersion(useJava17Plus);

    File forgePatchesJar = new File(LIBRARIES_DIR, "lwjgl3ify-" + lwjgl3ifyVersion + "-forgePatches.jar");
    if (forgePatchesJar.exists()) {
      result.add(forgePatchesJar.getAbsolutePath());
    } else {
      // Логирование ошибки или попытка скачать файл
      System.err.println("Forge patches jar not found: " + forgePatchesJar.getAbsolutePath());
    }

    return result;
  }

  private static String getlwjgl3ifyVersion(boolean useJava17Plus) throws IOException {
    List<MmcPackParser.Component> components = MmcPackParser.loadComponents(mmcPack);

    return components.stream()
        .filter(c -> c.getUid().equals("me.eigenraven.lwjgl3ify.forgepatches") || c.getUid().equals("LWJGL3ify Early Classpath"))
        .findFirst()
        .map(useJava17Plus ? MmcPackParser.Component::getCachedVersion : MmcPackParser.Component::getVersion)
        .orElse("2.1.14");
  }

}
