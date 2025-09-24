package zi.zircky.gtnhlauncher.service.download;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
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
    @Getter
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
      return name.contains(":natives-");
    }

    public File getPath() {
      String[] parts = name.split(":");
      if (parts.length < 3) return null;

      String group = parts[0].replace(".", "/");
      String artifact = parts[1];
      String version = parts[2];
      String classifier = parts.length >= 4 ? parts[3] : null;

      String path = group + "/" + artifact + "/" + version;
      String fileName = artifact + "-" + version + (classifier != null ? "-" + classifier : "") + ".jar";

      File fullPath = new File(LIBRARIES_DIR, path + "/" + fileName);
      System.out.println("Full path native: " + fullPath);
      return fullPath;
    }

  }

  public static ProcessBuilder launch(File javaPath, int ramGb, String username, String uuid, String accessToken, boolean useJava17Plus) throws IOException {
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
    List<Library> libraries = collectLiberies(allJsons, useJava17Plus);
    AssetsInstaller.installAssets(new File(MinecraftUtils.getMinecraftDir(), "assets"));

    List<String> classpath = downloadAndBuildClasspath(libraries, useJava17Plus);

    File nativesDir = new File(MinecraftUtils.getNativePath());
    logger.info("NativesDir: " + nativesDir);
    NativesExtractor.extractNatives(nativesDir, libraries);

    if (useJava17Plus) {
      String lwjgl3ifyVersion = getlwjgl3ifyVersion(useJava17Plus);

      File lwjgl3ifyJar = new File(LIBRARIES_DIR, "lwjgl3ify-" + lwjgl3ifyVersion + "-forgePatches.jar");

      if (lwjgl3ifyJar.exists()) {
        libraries.add(new Library("me.eigenraven:lwjgl3ify:" + lwjgl3ifyVersion + ":forgePatches", null, null, 0));
        logger.info("✅ Added lwjgl3ify-" + lwjgl3ifyVersion + "-forgePatches.jar: " + lwjgl3ifyJar.getAbsolutePath());
      } else {
        logger.warning("❌ lwjgl3ify-" + lwjgl3ifyVersion + "-forgePatches.jar not found at: " + lwjgl3ifyJar.getAbsolutePath());
      }
    }

    List<String> classpath = downloadAndBuildClasspath(libraries, useJava17Plus);

    String minecraftVersion = components.stream()
        .filter(c -> c.getUid().equals("net.minecraft") || c.getCachedName().equals("Minecraft with LWJGL3"))
        .findFirst()
        .map(useJava17Plus ? MmcPackParser.Component::getCachedVersion : MmcPackParser.Component::getVersion)
        .orElse("1.7.10");

    String forgeVersion = components.stream()
        .filter(c -> c.getUid().equals("net.minecraftforge") || c.getCachedName().equals("Forge-LWJGL3"))
        .findFirst()
        .map(useJava17Plus ? MmcPackParser.Component::getCachedVersion : MmcPackParser.Component::getVersion)
        .orElse("10.13.4.1614");

    String lwjglVersion = components.stream()
        .filter(c -> c.getUid().equals("org.lwjgl") || c.getCachedName().equals("LWJGL 3"))
        .findFirst()
        .map(useJava17Plus ? MmcPackParser.Component::getCachedVersion : MmcPackParser.Component::getVersion)
        .orElse("");

    String lwjglName = components.stream()
        .filter(c -> c.getUid().equals("org.lwjgl") || c.getCachedName().equals("LWJGL 3"))
        .findFirst()
        .map(MmcPackParser.Component::getUid)
        .orElse("");

    MojangInstaller.installVersion(minecraftVersion, mcDir, (p, msg) -> System.out.println(msg + " " + (int) (p * 100) + "%"));
    ForgeDownloader.ensureForgePresent(LIBRARIES_DIR, minecraftVersion, forgeVersion);

    jvmArgs.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
    jvmArgs.add("-Djna.debug_load=true");

    String mcArgs;
    if (useJava17Plus) {
      // 🔹 Аргументы для Java 17+
      mcArgs = resolverMinecraftArgs(allJsons);
      mcArgs = mcArgs
          .replace("${auth_player_name}", username)
          .replace("${auth_uuid}", uuid)
          .replace("${auth_access_token}", accessToken)
          .replace("${version_name}", minecraftVersion)
          .replace("${game_directory}", MinecraftUtils.gameRoot()) // .minecraft
          .replace("${assets_root}", new File(MinecraftUtils.getMinecraftDir(), "assets").getAbsolutePath())
          .replace("${assets_index_name}", minecraftVersion)
          .replace("${user_properties}", "{}")
          .replace("${user_type}", "legacy");
    } else {

      List<String> legacyArgs = new ArrayList<>();
      legacyArgs.add("--gameDir");
      legacyArgs.add(MinecraftUtils.gameRoot());
      legacyArgs.add("--username");
      legacyArgs.add(username);
      legacyArgs.add("--uuid");
      legacyArgs.add(uuid);
      legacyArgs.add("--accessToken");
      legacyArgs.add(accessToken);
      legacyArgs.add("--version");
      legacyArgs.add(minecraftVersion);
      legacyArgs.add("--assetsDir");
      legacyArgs.add(new File(MinecraftUtils.getMinecraftDir(), "assets").getAbsolutePath());
      legacyArgs.add("--assetIndex");
      legacyArgs.add("legacy");
      legacyArgs.add("--userProperties");
      legacyArgs.add("{}");
      legacyArgs.add("--userType");
      legacyArgs.add("legacy");

      mcArgs = String.join(" ", legacyArgs);
    }
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
    command.add("cpw.mods.fml.common.launcher.FMLTweaker");

    logger.info("Test commands: " + command);
    return new ProcessBuilder(command).directory(new File(MinecraftUtils.gameRoot()));
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
        .orElse("net.minecraft.launchwrapper.Launch");
  }

  private static String resolverMinecraftArgs(List<JsonObject> jsonObjects) {
    return jsonObjects.stream()
        .filter(jsonObject -> jsonObject.has("minecraftArguments"))
        .map(obj -> obj.get("minecraftArguments").getAsString())
        .findFirst()
        .orElse("");
  }

  private static List<Library> collectLiberies(List<JsonObject> jsonObjects, boolean useJava17Plus) {
    List<Library> result = new ArrayList<>();
    String librariesDir = MinecraftUtils.getLocalLibraryPath();

    if (useJava17Plus) {
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
    } else {
      String[] EXTRA_LIBRARIES = {
          // === LaunchWrapper & utils ===
          "net.minecraft:launchwrapper:1.12",
          "org.ow2.asm:asm-all:5.0.3",
          "com.typesafe.akka:akka-actor_2.11:2.3.3",
          "com.typesafe:config:1.2.1",
          "org.scala-lang:scala-library:2.11.1",
          "org.scala-lang:scala-compiler:2.11.1",
          "org.scala-lang:scala-reflect:2.11.1",
          "org.scala-lang.modules:scala-parser-combinators_2.11:1.0.1",

          // === LWJGL 2.x (основные) ===
          "org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209",
          "org.lwjgl.lwjgl:lwjgl_util:2.9.4-nightly-20150209",
          "org.lwjgl.lwjgl:lwjgl-platform:2.9.4-nightly-20150209:natives-windows",

          // === JInput (нужен для клавиатуры/мыши) ===
          "net.java.jinput:jinput:2.0.5",
          "net.java.jinput:jinput-platform:2.0.5:natives-windows",

          // === Apache Commons ===
          "org.apache.commons:commons-lang3:3.3.2",
          "commons-io:commons-io:2.4",
          "commons-codec:commons-codec:1.9",

          // === Guava / Gson ===
          "com.google.guava:guava:17.0",
          "com.google.code.gson:gson:2.2.4",

          // === Logback / Logging ===
          "org.apache.logging.log4j:log4j-api:2.0-beta9",
          "org.apache.logging.log4j:log4j-core:2.0-beta9",

          // === Other dependencies ===
          "net.sf.jopt-simple:jopt-simple:4.5",
          "oshi-project:oshi-core:1.1",
          "net.java.dev.jna:jna:3.4.0",
          "net.java.dev.jna:platform:3.4.0"
      };

      for (String lib : EXTRA_LIBRARIES) {
        String[] parts = lib.split(":");
        String groupId = parts[0];
        String artifactId = parts[1];
        String version = parts[2];
        String classifier = (parts.length > 3 ? parts[3] : null);

        try {
          LibraryDownloader.downloadLibrary(groupId, artifactId, version, new File(librariesDir), classifier);

          File jarPath = new File(librariesDir,
              groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" +
                  artifactId + "-" + version + (classifier != null ? "-" + classifier : "") + ".jar");

          result.add(new Library(lib, null, null, jarPath.length()));
          System.out.println("📦 Added extra lib: " + lib);
        } catch (IOException e) {
          System.err.println("❌ Не удалось скачать extra библиотеку: " + lib);
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
          logger.info("✓ Loaded: " + file.getPath());
          break;
        } catch (IOException e) {
          System.err.println("Loading error: " + lib.url);
        }
      }
      result.add(file.getAbsolutePath());
    }

    if (useJava17Plus) {
      String lwjgl3ifyVersion = getlwjgl3ifyVersion(useJava17Plus);

      File forgePatchesJar = new File(LIBRARIES_DIR, "lwjgl3ify-" + lwjgl3ifyVersion + "-forgePatches.jar");
      if (forgePatchesJar.exists()) {
        result.add(forgePatchesJar.getAbsolutePath());
      } else {
        // Логирование ошибки или попытка скачать файл
        System.err.println("Forge patches jar not found: " + forgePatchesJar.getAbsolutePath());
      }
    }

    File forgeJar = new File(LIBRARIES_DIR, "net/minecraftforge/forge/" + "1.7.10-10.13.4.1614-1.7.10" + "/forge-" + "1.7.10-10.13.4.1614-1.7.10" + "-universal.jar");
    System.out.println(forgeJar);
    if (forgeJar.exists()) {
      result.add(forgeJar.getAbsolutePath());
    }

    File mcJar = new File(MinecraftUtils.gameRoot(), "versions/1.7.10/1.7.10.jar");
    System.out.println(mcJar);
    if (mcJar.exists()) {
      result.add(mcJar.getAbsolutePath());
    } else {
      System.err.println("⚠ Minecraft jar not found: " + mcJar.getAbsolutePath());
    }

//    File launchwrapper = new File(LIBRARIES_DIR, "net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar");
//    if (!launchwrapper.exists()) {
//      logger.info("⬇ Downloading missing launchwrapper-1.12.jar");
//      LibraryDownloader.downloadLibrary(
//          "net.minecraft", "launchwrapper", "1.12",
//          LIBRARIES_DIR, null
//      );
//    }
//    result.add(launchwrapper.getAbsolutePath());
//
//    File joptsimpleJar = new File(LIBRARIES_DIR, "net/sf/jopt-simple/jopt-simple/4.5/jopt-simple-4.5.jar");
//    System.out.println(joptsimpleJar.getAbsolutePath());
//    if (!joptsimpleJar.exists()) {
//      result.add(joptsimpleJar.getAbsolutePath());
//    } else {
//      System.err.println("⚠ Jopt-Simple jar not found: " + joptsimpleJar.getAbsolutePath());
//    }
//
//
//    File mcJar = new File(mcDir, "versions/1.7.10/1.7.10.jar");
//    if (mcJar.exists()) {
//      result.add(mcJar.getAbsolutePath());
//    } else {
//      System.err.println("⚠ Minecraft jar not found: " + mcJar.getAbsolutePath());
//    }
//
//    // ✅ Добавляем forge universal jar
//    File forgeUniversal = new File(LIBRARIES_DIR,
//        "net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10-universal.jar");
//    if (forgeUniversal.exists()) {
//      result.add(forgeUniversal.getAbsolutePath());
//    } else {
//      System.err.println("⚠ Forge universal jar not found: " + forgeUniversal.getAbsolutePath());
//    }

    if (!useJava17Plus) {
      addAllJarsRecursive(LIBRARIES_DIR, result);

    }

    return result;
  }

  private static void addAllJarsRecursive(File dir, List<String> classpath) {
    if (dir.exists() && dir.isDirectory()) {
      for (File file : dir.listFiles()) {
        if (file.isDirectory()) {
          addAllJarsRecursive(file, classpath);
        } else if (file.isFile() && file.getName().endsWith(".jar")) {
          String path = file.getAbsolutePath();

          if (path.contains("guava-15.0.jar")) continue;
          if (path.contains("lwjgl-2.9.1.jar")) continue;
          if (path.contains("lwjgl_util-2.9.1.jar")) continue;
          if (path.contains("lwjgl-platform-2.9.1-natives-windows.jar")) continue;
          if (path.contains("lwjgl-platform-2.9.1-natives-linux.jar")) continue;
          if (path.contains("lwjgl-platform-2.9.1-natives-osx.jar")) continue;

          if (!classpath.contains(path)) {
            classpath.add(path);
          }
        }
      }
    }
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
