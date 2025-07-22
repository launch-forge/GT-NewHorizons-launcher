package zi.zircky.gtnhlauncher.service.download;

import java.util.List;

public class MmcPack {
  public List<Component> components;
  public int formatVersion;

  public String getMinecraftVersion() {
    for (Component c : components) {
      if ("net.minecraft".equals(c.uid)) return c.version;
    }
    return null;
  }

  public class Component {
    public String cachedName;
    public String cachedVersion;
    public String uid;
    public String version;
    public Boolean cachedVolatile;
    public Boolean dependencyOnly;
    public Boolean important;
    public List<Requirement> cachedRequires;
  }

  public class Requirement {
    public String uid;
    public String equals;
    public String suggests;
  }
}


