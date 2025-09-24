package zi.zircky.gtnhlauncher.service.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ForgeDownloader {
  private static final String MAVEN_BASE_URL = "https://maven.minecraftforge.net";
  private static final String GROUP_PATH = "net/minecraftforge/forge";

  public static void ensureForgePresent(File librariesDir, String mcVersion, String forgeVersion) throws IOException {
    final String FULL_VERSION = mcVersion + "-" + forgeVersion + "-" + mcVersion;
    final String FILE_NAME = "forge-" + FULL_VERSION + "-universal.jar";
    File targetPath = new File(librariesDir, GROUP_PATH + "/" + FULL_VERSION + "/" + FILE_NAME);

    if (targetPath.exists()) {
      System.out.println("Forge is already loaded: " + targetPath.getPath());
      return;
    }

    System.out.println("Forge is not found, loading ...");

    targetPath.getParentFile().mkdirs();

    String url = MAVEN_BASE_URL + "/" + GROUP_PATH + "/" + FULL_VERSION + "/" + FILE_NAME;

    try (InputStream in = new URL(url).openStream()) {
      Files.copy(in, targetPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
      System.out.println("Forge is successfully loaded: " + targetPath.getPath());
    } catch (IOException e) {
      System.err.println("Forge loading error: " + url);
      e.printStackTrace();
      throw e;
    }
  }

  public static void ensureForgeAndLwjgl(File librariesDir, String mcVersion, String forgeVersion, String lwjglName, String lwjglVersion) throws IOException {
    ensureForgePresent(librariesDir, mcVersion, forgeVersion);

    LibraryDownloader.downloadLibrary("org.ow2.asm", "asm-all", "5.0.3", librariesDir, null );
    LibraryDownloader.downloadLibrary("lzma", "lzma", "0.0.1", librariesDir, null );

    LibraryDownloader.downloadLibrary("org.scala-lang", "scala-library", "2.11.1", librariesDir, null);
    LibraryDownloader.downloadLibrary("org.scala-lang", "scala-compiler", "2.11.1", librariesDir, null);
    LibraryDownloader.downloadLibrary("org.scala-lang", "scala-reflect", "2.11.1", librariesDir, null);

    LibraryDownloader.downloadLibrary("com.google.guava", "guava", "21.0", librariesDir, null);

    //LibraryDownloader.downloadLibrary("org.spongepowered", "mixin", "0.8.5", librariesDir, null);

    // LWJGL Core
    LibraryDownloader.downloadLibrary(lwjglName + "/lwjgl", "lwjgl", lwjglVersion, librariesDir, null);

    // LWJGL Utils
    LibraryDownloader.downloadLibrary(lwjglName + "/lwjgl", "lwjgl_util", lwjglVersion, librariesDir, null);

    // LWJGL Natives (для Windows)
    LibraryDownloader.downloadLibrary(lwjglName + "/lwjgl", "lwjgl-platform", lwjglVersion, librariesDir, "natives-windows");
  }
}
