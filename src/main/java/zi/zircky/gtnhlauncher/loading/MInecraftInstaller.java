package zi.zircky.gtnhlauncher.loading;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MInecraftInstaller {
  public static void installVersion(String versionId) throws IOException {
    List<MinecraftVersion> versions = MojangVersionList.fetchAvailableVersions();

    Optional<MinecraftVersion> selected = versions.stream()
            .filter(v -> v.id()
            .equals(versionId)).findFirst();

    if (selected.isEmpty()) {
      throw new IOException("Версия не найдена: " + versionId);
    }

    MinecraftVersionMeta meta = MojangVersionDownloader.downloadVersionMeta(selected.get().jsonUrl());
    System.out.println("✔ Версия: " + meta.id);
    System.out.println("✔ JAR URL: " + meta.downloads.client.url);
    System.out.println("✔ Assets index: " + meta.assetIndex.id);

    System.out.println("✔ Libraries: " + meta.libraries.size());
  }
}
