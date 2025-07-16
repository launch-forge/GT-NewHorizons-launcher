package zi.zircky.gtnhlauncher.service.download;

import java.util.List;

public class MmcPack {
  public List<Component> components;

  public String getMinecraftVersion() {
    for (Component c : components) {
      if ("net.minecraft".equals(c.uid)) return c.version;
    }
    return null;
  }

  public class Component {
    public String uid;
    public String version;
  }
}


