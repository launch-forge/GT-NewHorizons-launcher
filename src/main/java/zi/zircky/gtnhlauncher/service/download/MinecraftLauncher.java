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
  private static final String ADDOPENS = "--add-opens";
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

  public static ProcessBuilder launch(File javaPath, int ramGb, File gameDir, String username, String uuid, String accessToken) throws IOException {

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
    jvmArgs.add("-Dfile.encoding=UTF-8");
    jvmArgs.add("-Djava.system.class.loader=com.gtnewhorizons.retrofuturabootstrap.RfbSystemClassLoader");
    jvmArgs.addAll(List.of(
        ADDOPENS, "java.base/java.io=ALL-UNNAMED",
        ADDOPENS, "java.base/java.lang=ALL-UNNAMED",
        ADDOPENS, "java.base/java.lang.invoke=ALL-UNNAMED",
        ADDOPENS, "java.base/java.lang.ref=ALL-UNNAMED",
        ADDOPENS, "java.base/java.nio=ALL-UNNAMED",
        ADDOPENS, "java.base/java.util=ALL-UNNAMED",
        ADDOPENS, "java.base/java.util.zip=ALL-UNNAMED",
        ADDOPENS, "java.base/jdk.internal.loader=ALL-UNNAMED"
    ));
    String mainClass = resolveMainClass(allJsons);
    List<Library> libraries = collectLiberies(allJsons);

    File lwjgl3ifyJar = new File(LIBRARIES_DIR, "lwjgl3ify-2.1.14-forgePatches.jar");
    if (lwjgl3ifyJar.exists()) {
      libraries.add(new Library("me.eigenraven:lwjgl3ify:2.1.14:forgePatches", null, null, 0));
      logger.info("✅ Added lwjgl3ify-2.1.14-forgePatches.jar: " + lwjgl3ifyJar.getAbsolutePath());
    } else {
      logger.warning("❌ lwjgl3ify-2.1.14-forgePatches.jar not found at: " + lwjgl3ifyJar.getAbsolutePath());
    }

    List<String> classpath = downloadAndBuildClasspath(libraries);

    components.stream()
        .filter(c -> c.getUid().equals("me.eigenraven.lwjgl3ify.forgepatches"))
        .findFirst()
        .map(comp -> MmcPackParser.resolveComponentJarFile(PATCHES_DIR, comp))
        .ifPresentOrElse(
            forgepatchJar -> {
              if (forgepatchJar.exists() && !classpath.contains(forgepatchJar.getAbsolutePath())) {
                classpath.add(0, forgepatchJar.getAbsolutePath());
                logger.info("✅ Forgepatches added: " + forgepatchJar.getAbsolutePath());
              } else if (!forgepatchJar.exists()) {
                logger.warning("❌ Jar for Forgepatches was not found: " + forgepatchJar.getAbsolutePath());
              }
            },
            () -> logger.warning("❌ Component forgepatches not found in mmc-pack.json")
        );


    String minecraftVersion = components.stream()
        .filter(c -> c.getUid().equals("net.minecraft") || c.getUid().equals("Minecraft with LWJGL3"))
        .findFirst()
        .map(MmcPackParser.Component::getVersion)
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
      if (jsonObject.has("libraries")) {
        for (JsonElement element : jsonObject.getAsJsonArray("libraries")) {
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

  private static List<String> downloadAndBuildClasspath(List<Library> libraries) throws IOException {
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

    return result;
  }

}
