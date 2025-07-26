package zi.zircky.gtnhlauncher.auth;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class MicrosoftLoginManager {
  private static final Logger log = Logger.getLogger(MicrosoftLoginManager.class.getName());

  private static final String CLIENT_ID;
  private static final String CLIENT_SECRET; // Опционально
  private static final String REDIRECT_URI = "http://localhost:8080/callback";
  private static final int CALLBACK_PORT = 8080;
  private static final String SCOPE = "XboxLive.signin XboxLive.offline_access";
  private static final Gson GSON = new Gson();
  private final OAuth20Service oauthService;
  private HttpServer callbackServer;
  private Consumer<String[]> onSuccess;
  private Consumer<String> onError;

  static {
    Properties props = new Properties();
    try (InputStream input = MicrosoftLoginManager.class.getClassLoader().getResourceAsStream("application.properties")) {
      if (input != null) {
        props.load(input);
      } else {
        log.warning("Warning: application.properties not found, using default values");
      }
    } catch (IOException e) {
      log.warning("Error loading application.properties: " + e.getMessage());
    }

    CLIENT_ID = props.getProperty("microsoft.client.id");
    CLIENT_SECRET = props.getProperty("microsoft.client.secret");
  }

  public MicrosoftLoginManager() {
    oauthService = new ServiceBuilder(CLIENT_ID)
        .apiSecret(CLIENT_SECRET)
        .defaultScope(SCOPE)
        .callback(REDIRECT_URI)
        .build(new MicrosoftOAuthApi());
  }

  public String getAuthorizationUrl() {
    String authUrl = oauthService.getAuthorizationUrl();
    log.info("Authorization URL: " + authUrl);
    return authUrl;
  }

  public void login(Consumer<String[]> onSuccess, Consumer<String> onError) {
    this.onSuccess = onSuccess;
    this.onError = onError;
    try {
      startCallbackServer();
    } catch (IOException e) {
      onError.accept("Ошибка при запуске сервера: " + e.getMessage());
    }
  }

  private void startCallbackServer() throws IOException {
    if (callbackServer != null) {
      try {
        callbackServer.stop(0);
        log.info("Callback server stopped");
      } catch (Exception e) {
        log.warning("Failed to stop existing server: " + e.getMessage());
      }
    }

    int[] portsToTry = {CALLBACK_PORT, 8081, 8082};
    IOException lastException = null;

    for (int port : portsToTry) {
      try {
        callbackServer = HttpServer.create(new InetSocketAddress(port), 0);
        callbackServer.createContext("/callback", this::handleCallback);
        callbackServer.setExecutor(null);
        callbackServer.start();
        log.info("Callback server started on port: " + port);
        return;
      } catch (BindException e) {
        lastException = e;
        log.warning("Failed to bind to port " + port + ": " + e.getMessage());
      }
    }

    throw new IOException("Unable to start callback server on any port", lastException);
  }

  private void handleCallback(HttpExchange exchange) throws IOException {
    String query = exchange.getRequestURI().getQuery();
    log.info("Callback query: " + query);
    String code = null;
    String error = null;

    if (query != null) {
      if (query.contains("code=")) {
        code = query.split("code=")[1].split("&")[0];
      } else if (query.contains("error=")) {
        error = query.split("error=")[1].split("&")[0];
        String errorDescription = query.contains("error_description=") ? query.split("error_description=")[1].split("&")[0] : "No description";
        log.info("OAuth error: " + error + ", description: " + errorDescription);
      }
    }

    if (code != null) {
      try {
        log.info("Received OAuth code: " + code);
        OAuth2AccessToken accessToken = oauthService.getAccessToken(code);
        log.info("OAuth token request sent to: " + oauthService.getApi().getAccessTokenEndpoint());
        log.info("Microsoft access token: " + (accessToken.getAccessToken().length() > 10 ? accessToken.getAccessToken().substring(0, 10) + "..." : accessToken.getAccessToken()));
        log.info("Token scope: " + accessToken.getScope());
        log.info("Refresh token: " + (accessToken.getRefreshToken() != null ? accessToken.getRefreshToken().substring(0, 10) + "..." : "<none>"));
        String msAccessToken = accessToken.getAccessToken();

        if (accessToken.getRefreshToken() != null) {
          try {
            OAuth2AccessToken refreshedToken = oauthService.refreshAccessToken(accessToken.getRefreshToken());
            log.info("Refreshed access token: " + (refreshedToken.getAccessToken().length() > 10 ? refreshedToken.getAccessToken().substring(0, 10) + "..." : refreshedToken.getAccessToken()));
            msAccessToken = refreshedToken.getAccessToken();
          } catch (Exception e) {
            log.info("Failed to refresh token: " + e.getMessage());
          }
        }

        String xboxLiveToken = authenticateXboxLive(msAccessToken);
        log.info("Xbox Live token: " + (xboxLiveToken.length() > 10 ? xboxLiveToken.substring(0, 10) + "..." : xboxLiveToken));
        String[] xstsData = getXSTSToken(xboxLiveToken);
        log.info("XSTS UHS: " + (xstsData[0].length() > 10 ? xstsData[0].substring(0, 10) + "..." : xstsData[0]));
        log.info("XSTS Token: " + (xstsData[1].length() > 10 ? xstsData[1].substring(0, 10) + "..." : xstsData[1]));
        String minecraftToken = authenticateMinecraft(xstsData[0], xstsData[1]);
        boolean ownsMinecraft = checkGameOwnership(minecraftToken);
        String[] profile = getMinecraftProfile(minecraftToken);

        if (ownsMinecraft) {
          AuthStorage.AuthInfo authInfo = new AuthStorage.AuthInfo();
          authInfo.username = profile[0];
          authInfo.uuid = profile[1];
          authInfo.accessToken = minecraftToken;

          AuthStorage.save(authInfo);

          onSuccess.accept(new String[]{profile[0], profile[1], minecraftToken});
        } else {
          onError.accept("Ошибка: Minecraft не куплен.");
        }

        String response = "Авторизация успешна. Можете закрыть это окно.";
        exchange.sendResponseHeaders(200, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
      } catch (Exception e) {
        onError.accept("Ошибка авторизации: " + e.getMessage());
        String response = "Ошибка авторизации: " + e.getMessage();
        exchange.sendResponseHeaders(500, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
      } finally {
        exchange.close();
        if (callbackServer != null) {
          try {
            callbackServer.stop(0);
            log.info("Callback server stopped");
          } catch (Exception e) {
            log.warning("Failed to stop callback server: " + e.getMessage());
          }
        }
      }
    } else {
      String errorMsg = error != null ? "OAuth error: " + error + (query.contains("error_description=") ? ", description: " + query.split("error_description=")[1].split("&")[0] : "") : "Ошибка: код авторизации не получен.";
      onError.accept(errorMsg);
      String response = errorMsg;
      exchange.sendResponseHeaders(400, response.getBytes().length);
      exchange.getResponseBody().write(response.getBytes());
      exchange.close();
    }
  }

  private String authenticateXboxLive(String msAccessToken) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    String json = "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=" + msAccessToken + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}";
    log.info("XboxLiveAuthRequest JSON: " + json);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("x-xbl-contract-version", "2")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    log.info("Xbox Live request URI: " + request.uri());
    log.info("Xbox Live request headers: " + request.headers());

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("Xbox Live response status: " + response.statusCode());
    log.info("Xbox Live response headers: " + response.headers());
    log.info("Xbox Live response body: " + (response.body().isEmpty() ? "<empty>" : response.body()));

    if (response.statusCode() != 200) {
      throw new IOException("Xbox Live authentication failed with status: " + response.statusCode() + ", body: " + (response.body().isEmpty() ? "<empty>" : response.body()));
    }

    JsonObject jsonResponse = GSON.fromJson(response.body(), JsonObject.class);
    if (jsonResponse == null) {
      throw new IOException("Failed to parse Xbox Live response as JSON: " + response.body());
    }

    JsonElement tokenElement = jsonResponse.get("Token");
    if (tokenElement == null || tokenElement.isJsonNull()) {
      throw new IOException("No 'Token' field in Xbox Live response: " + response.body());
    }

    return tokenElement.getAsString();
  }

  private String[] getXSTSToken(String xboxLiveToken) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    String json = "{\"Properties\":{\"UserTokens\":[\"" + xboxLiveToken + "\"],\"SandboxId\":\"RETAIL\"},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}";
    log.info("XSTSAuthRequest JSON: " + json);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("x-xbl-contract-version", "1")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    log.info("XSTS request URI: " + request.uri());
    log.info("XSTS request headers: " + request.headers());

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("XSTS response status: " + response.statusCode());
    log.info("XSTS response headers: " + response.headers());
    log.info("XSTS response body: " + (response.body().isEmpty() ? "<empty>" : response.body()));

    if (response.statusCode() != 200) {
      throw new IOException("XSTS authentication failed with status: " + response.statusCode() + ", body: " + (response.body().isEmpty() ? "<empty>" : response.body()));
    }

    JsonObject jsonResponse = GSON.fromJson(response.body(), JsonObject.class);
    if (jsonResponse == null) {
      throw new IOException("Failed to parse XSTS response as JSON: " + response.body());
    }

    JsonElement tokenElement = jsonResponse.get("Token");
    JsonElement displayClaimsElement = jsonResponse.get("DisplayClaims");
    if (tokenElement == null || tokenElement.isJsonNull() || displayClaimsElement == null || displayClaimsElement.isJsonNull()) {
      throw new IOException("Missing 'Token' or 'DisplayClaims' in XSTS response: " + response.body());
    }

    String token = tokenElement.getAsString();
    String uhs = displayClaimsElement.getAsJsonObject().getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();
    return new String[]{uhs, token};
  }

  private String authenticateMinecraft(String uhs, String xstsToken) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    String json = "{\"identityToken\":\"XBL3.0 x=" + uhs + ";" + xstsToken + "\"}";
    log.info("MinecraftAuthRequest JSON: " + json);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    log.info("Minecraft request URI: " + request.uri());
    log.info("Minecraft request headers: " + request.headers());

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("Minecraft auth response status: " + response.statusCode());
    log.info("Minecraft auth response headers: " + response.headers());
    log.info("Minecraft auth response body: " + (response.body().isEmpty() ? "<empty>" : response.body()));

    if (response.statusCode() != 200) {
      throw new IOException("Minecraft authentication failed with status: " + response.statusCode() + ", body: " + (response.body().isEmpty() ? "<empty>" : response.body()));
    }

    JsonObject jsonResponse = GSON.fromJson(response.body(), JsonObject.class);
    if (jsonResponse == null) {
      throw new IOException("Failed to parse Minecraft response as JSON: " + response.body());
    }

    JsonElement accessTokenElement = jsonResponse.get("access_token");
    if (accessTokenElement == null || accessTokenElement.isJsonNull()) {
      throw new IOException("No 'access_token' field in Minecraft response: " + response.body());
    }

    return accessTokenElement.getAsString();
  }

  private boolean checkGameOwnership(String minecraftToken) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.minecraftservices.com/entitlements/mcstore"))
        .header("Authorization", "Bearer " + minecraftToken)
        .header("Accept", "application/json")
        .GET()
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("Ownership check response status: " + response.statusCode());
    log.info("Ownership check response body: " + (response.body().isEmpty() ? "<empty>" : response.body()));

    if (response.statusCode() != 200) {
      throw new IOException("Ownership check failed with status: " + response.statusCode() + ", body: " + response.body());
    }

    JsonObject jsonResponse = GSON.fromJson(response.body(), JsonObject.class);
    if (jsonResponse == null) {
      throw new IOException("Failed to parse ownership response as JSON: " + response.body());
    }

    JsonElement itemsElement = jsonResponse.get("items");
    if (itemsElement == null || itemsElement.isJsonNull()) {
      throw new IOException("No 'items' field in ownership response: " + response.body());
    }

    return itemsElement.getAsJsonArray().size() > 0;
  }

  private String[] getMinecraftProfile(String minecraftToken) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.minecraftservices.com/minecraft/profile"))
        .header("Authorization", "Bearer " + minecraftToken)
        .header("Accept", "application/json")
        .GET()
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    log.info("Profile response status: " + response.statusCode());
    log.info("Profile response body: " + (response.body().isEmpty() ? "<empty>" : response.body()));

    if (response.statusCode() != 200) {
      throw new IOException("Profile fetch failed with status: " + response.statusCode() + ", body: " + response.body());
    }

    JsonObject jsonResponse = GSON.fromJson(response.body(), JsonObject.class);
    if (jsonResponse == null) {
      throw new IOException("Failed to parse profile response as JSON: " + response.body());
    }

    JsonElement nameElement = jsonResponse.get("name");
    JsonElement idElement = jsonResponse.get("id");
    if (nameElement == null || nameElement.isJsonNull() || idElement == null || idElement.isJsonNull()) {
      throw new IOException("Missing 'name' or 'id' in profile response: " + response.body());
    }

    return new String[]{nameElement.getAsString(), idElement.getAsString()};
  }

  static class MicrosoftOAuthApi extends DefaultApi20 {
    @Override
    public String getAccessTokenEndpoint() {
      return "https://login.live.com/oauth20_token.srf";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
      return "https://login.live.com/oauth20_authorize.srf";
    }
  }

}
