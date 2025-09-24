package zi.zircky.gtnhlauncher.service.download.mojang;

public class MojangAssetIndexInfo extends MojangDownloadInfo {
  private String id;
  private boolean known = true;
  private long totalSize;

  public MojangAssetIndexInfo(String id) {
    this.id = id;
    if ("legacy".equals(id)) {
      setUrl("https://piston-meta.mojang.com/mc/assets/legacy/c0fd82e8ce9fbc93119e40d96d5a4e62cfa3f729/legacy.json");
    } else {
      setUrl("https://s3.amazonaws.com/Minecraft.Download/indexes/" + id + ".json");
    }
    this.known = false;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public void setTotalSize(long totalSize) {
    this.totalSize = totalSize;
  }

  public boolean isKnown() {
    return known;
  }

  public void setKnown(boolean known) {
    this.known = known;
  }

  public String getId() {
    return id;
  }
}
