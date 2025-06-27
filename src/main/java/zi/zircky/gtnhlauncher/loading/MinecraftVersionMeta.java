package zi.zircky.gtnhlauncher.loading;

import java.util.List;

public class MinecraftVersionMeta {
  public static class Library {
    public String name;
    public Downloads downloads;

    public static class Downloads {
      public Artifact artifact;

      public static class Artifact {
        public String url;
        public String path;
      }
    }
  }

  public static class Downloads {
    public DowenloadEntry client;
    public static class DowenloadEntry {
      public String url;
      public String sha1;
      public long size;
    }
  }

  public String id;
  public Downloads downloads;
  public List<Library> libraries;
  public AssetIndex assetIndex;

  public static class AssetIndex {
    public String id;
    public String url;
    public String sha1;
  }
}
