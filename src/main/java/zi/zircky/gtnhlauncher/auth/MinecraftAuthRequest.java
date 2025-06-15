package zi.zircky.gtnhlauncher.auth;

import com.google.gson.JsonElement;

public class MinecraftAuthRequest {
  public String identityToken;

  public MinecraftAuthRequest(String identityToken) {
    this.identityToken = identityToken;
  }
}
