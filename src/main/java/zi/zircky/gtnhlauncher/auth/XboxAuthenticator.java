package zi.zircky.gtnhlauncher.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import zi.zircky.gtnhlauncher.utils.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
    return response.get("Token").asText();
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
    JsonNode xui = response.get("DisplayClaims").get("xui").get(0);
    return new XSTS(response.get("Token").asText(), xui.get("uhs").asText());
  }

  public static String getMinecraftToken(String userHash, String xstsToken) throws IOException, InterruptedException {
    String json = """
            {
              "identityToken": "XBL3.0 x=%s;%s"
            }
            """.formatted(userHash, xstsToken);

    JsonNode response = HttpUtils.postJson("https://api.minecraftservices.com/authentication/login_with_xbox", json);
    return response.get("access_token").asText();
  }

  public static MinecraftSession getMinecraftProfile(String mcAccessToken, String refreshToken) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
        .header("Authorization", "Bearer " + mcAccessToken)
        .build();

    HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode json = new ObjectMapper().readTree(response.body());

    return new MinecraftSession(
        json.get("id").asText(),
        json.get("name").asText(),
        mcAccessToken,
        refreshToken
    );
  }
}
