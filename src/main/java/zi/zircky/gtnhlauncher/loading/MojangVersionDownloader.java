package zi.zircky.gtnhlauncher.loading;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

public class MojangVersionDownloader {
  public static MinecraftVersionMeta downloadVersionMeta(String jsonUrl) throws IOException {
    URL url = new URL(jsonUrl);
    try (InputStream in = url.openStream(); Reader reader = new InputStreamReader(in)) {
      return new Gson().fromJson(reader, MinecraftVersionMeta.class);
    }
  }
}
