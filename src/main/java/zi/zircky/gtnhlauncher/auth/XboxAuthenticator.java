package zi.zircky.gtnhlauncher.auth;

import com.fasterxml.jackson.databind.JsonNode;
import zi.zircky.gtnhlauncher.utils.HttpUtils;

import java.io.IOException;

public class XboxAuthenticator {
  public static String getXboxLiveToken(String accessToken) throws IOException, InterruptedException {
    String json = """
            {
              "Properties": {
                "AuthMethod": "RPS",
                "SiteName": "user.auth.xboxlive.com",
                "RpsTicket": "d=%s"
              },
              "RelyingParty": "http://auth.xboxlive.com",
              "TokenType": "JWT"
            }
            """.formatted(accessToken);

    JsonNode response = HttpUtils.postJson("https://user.auth.xboxlive.com/user/authenticate", json);
    return requireText(response, "Token", "Xbox Live");
  }

  public static class XSTS {
    public final String token, userHash;

    public XSTS(String token, String userHash) {
      this.token = token;
      this.userHash = userHash;
    }
  }

  public static XSTS getXstsToken(String xboxToken) throws IOException, InterruptedException {
    String json = """
            {
              "Properties": {
                "SandboxId": "RETAIL",
                "UserTokens": ["%s"]
              },
              "RelyingParty": "rp://api.minecraftservices.com/",
              "TokenType": "JWT"
            }
            """.formatted(xboxToken);

    JsonNode response = HttpUtils.postJson("https://xsts.auth.xboxlive.com/xsts/authorize", json);
    JsonNode displayClaims = response.path("DisplayClaims").path("xui");

    if (!displayClaims.isArray() || displayClaims.isEmpty()) {
      throw new IOException("Missing Xbox user hash in XSTS response");
    }

    JsonNode xui = displayClaims.get(0);

    return new XSTS(requireText(response, "Token", "XSTS"), requireText(xui, "uhs", "XSTS"));
  }

  public static String getMinecraftToken(String userHash, String xstsToken) throws IOException, InterruptedException {
    String json = """
            {
              "identityToken": "XBL3.0 x=%s;%s"
            }
            """.formatted(userHash, xstsToken);

    JsonNode response = HttpUtils.postJson("https://api.minecraftservices.com/authentication/login_with_xbox", json);
    return requireText(response, "access_token", "Minecraft token");
  }

  public static MinecraftSession getMinecraftProfile(String mcAccessToken, String refreshToken) throws IOException, InterruptedException {
    JsonNode json = HttpUtils.getJson("https://api.minecraftservices.com/minecraft/profile", mcAccessToken);

    return new MinecraftSession(
        requireText(json, "id", "Minecraft profile"),
        requireText(json, "name", "Minecraft profile"),
        mcAccessToken,
        refreshToken
    );
  }

  private static String requireText(JsonNode json, String field, String context) throws IOException {
    JsonNode value = json.get(field);

    if (value == null || value.isNull()) {
      throw new IOException("Missing '" + field + "' in " + context + " response");
    }

    String text = value.asText();

    if (text == null || text.isBlank()) {
      throw new IOException("Missing '" + field + "' in " + context + " response");
    }

    return text;
  }
}
