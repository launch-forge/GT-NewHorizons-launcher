package zi.zircky.gtnhlauncher.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import zi.zircky.gtnhlauncher.utils.MinecraftUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AuthStorage {
  private static final File AUTH_FILE = new File(MinecraftUtils.getMinecraftDir(), "account.json");
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public static class AuthInfo {
    public String username;
    public String uuid;
    public String accessToken;
  }

  public static void save(AuthInfo authInfo) {
    try (FileWriter writer = new FileWriter(AUTH_FILE)) {
      GSON.toJson(authInfo, writer);
      System.out.println("[AUTH] Auth info saved to auth.json");
    } catch (IOException e) {
      System.err.println("[AUTH] Failed to save auth info: " + e.getMessage());
    }
  }

  public static AuthInfo load() {
    if (!AUTH_FILE.exists()) return null;
    try (FileReader reader = new FileReader(AUTH_FILE)) {
      return GSON.fromJson(reader, AuthInfo.class);
    } catch (IOException e) {
      System.err.println("[AUTH] Failed to load auth info: " + e.getMessage());
      return null;
    }
  }
}
