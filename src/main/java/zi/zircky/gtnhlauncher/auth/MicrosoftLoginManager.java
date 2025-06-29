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
  Logger log = Logger.getLogger(getClass().getName());

  private static final String CLIENT_ID;
  private static final String CLIENT_SECRET; // Опционально
  private static final String REDIRECT_URI = "http://localhost:8080/callback";
  private static int CALLBACK_PORT = 8080;
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
        System.err.println("Warning: application.properties not found, using default values");
      }
    } catch (IOException e) {
      System.err.println("Error loading application.properties: " + e.getMessage());
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
    System.out.println("Authorization URL: " + authUrl);
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
        System.out.println("Callback server stopped");
      } catch (Exception e) {
        System.err.println("Failed to stop existing server: " + e.getMessage());
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
        System.out.println("Callback server started on port: " + port);
        return;
      } catch (BindException e) {
        lastException = e;
        System.err.println("Failed to bind to port " + port + ": " + e.getMessage());
      }
    }

    throw new IOException("Unable to start callback server on any port", lastException);
  }

  private void handleCallback(HttpExchange exchange) throws IOException {
    String query = exchange.getRequestURI().getQuery();
    System.out.println("Callback query: " + query);
    String code = null;
    String error = null;

    if (query != null) {
      if (query.contains("code=")) {
        code = query.split("code=")[1].split("&")[0];
      } else if (query.contains("error=")) {
        error = query.split("error=")[1].split("&")[0];
        String errorDescription = query.contains("error_description=") ? query.split("error_description=")[1].split("&")[0] : "No description";
        System.out.println("OAuth error: " + error + ", description: " + errorDescription);
      }
    }

    if (code != null) {
      try {
        System.out.println("Received OAuth code: " + code);
        OAuth2AccessToken accessToken = oauthService.getAccessToken(code);
        System.out.println("OAuth token request sent to: " + oauthService.getApi().getAccessTokenEndpoint());
        System.out.println("Microsoft access token: " + (accessToken.getAccessToken().length() > 10 ? accessToken.getAccessToken().substring(0, 10) + "..." : accessToken.getAccessToken()));
        System.out.println("Token scope: " + accessToken.getScope());
        System.out.println("Refresh token: " + (accessToken.getRefreshToken() != null ? accessToken.getRefreshToken().substring(0, 10) + "..." : "<none>"));
        String msAccessToken = accessToken.getAccessToken();

        if (accessToken.getRefreshToken() != null) {
          try {
            OAuth2AccessToken refreshedToken = oauthService.refreshAccessToken(accessToken.getRefreshToken());
            System.out.println("Refreshed access token: " + (refreshedToken.getAccessToken().length() > 10 ? refreshedToken.getAccessToken().substring(0, 10) + "..." : refreshedToken.getAccessToken()));
            msAccessToken = refreshedToken.getAccessToken();
          } catch (Exception e) {
            System.out.println("Failed to refresh token: " + e.getMessage());
          }
        }

        String xboxLiveToken = authenticateXboxLive(msAccessToken);
        System.out.println("Xbox Live token: " + (xboxLiveToken.length() > 10 ? xboxLiveToken.substring(0, 10) + "..." : xboxLiveToken));
        String[] xstsData = getXSTSToken(xboxLiveToken);
        System.out.println("XSTS UHS: " + (xstsData[0].length() > 10 ? xstsData[0].substring(0, 10) + "..." : xstsData[0]));
        System.out.println("XSTS Token: " + (xstsData[1].length() > 10 ? xstsData[1].substring(0, 10) + "..." : xstsData[1]));
        String minecraftToken = authenticateMinecraft(xstsData[0], xstsData[1]);
        boolean ownsMinecraft = checkGameOwnership(minecraftToken);
        String[] profile = getMinecraftProfile(minecraftToken);

        if (ownsMinecraft) {
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
            System.out.println("Callback server stopped");
          } catch (Exception e) {
            System.err.println("Failed to stop callback server: " + e.getMessage());
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
    System.out.println("XboxLiveAuthRequest JSON: " + json);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("x-xbl-contract-version", "2")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    System.out.println("Xbox Live request URI: " + request.uri());
    System.out.println("Xbox Live request headers: " + request.headers());

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println("Xbox Live response status: " + response.statusCode());
    System.out.println("Xbox Live response headers: " + response.headers());
    System.out.println("Xbox Live response body: " + (response.body().isEmpty() ? "<empty>" : response.body()));

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
    System.out.println("XSTSAuthRequest JSON: " + json);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("x-xbl-contract-version", "1")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    System.out.println("XSTS request URI: " + request.uri());
    System.out.println("XSTS request headers: " + request.headers());

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println("XSTS response status: " + response.statusCode());
    System.out.println("XSTS response headers: " + response.headers());
    System.out.println("XSTS response body: " + (response.body().isEmpty() ? "<empty>" : response.body()));

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
    System.out.println("MinecraftAuthRequest JSON: " + json);

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();

    System.out.println("Minecraft request URI: " + request.uri());
    System.out.println("Minecraft request headers: " + request.headers());

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println("Minecraft auth response status: " + response.statusCode());
    System.out.println("Minecraft auth response headers: " + response.headers());
    System.out.println("Minecraft auth response body: " + (response.body().isEmpty() ? "<empty>" : response.body()));

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
    System.out.println("Ownership check response status: " + response.statusCode());
    System.out.println("Ownership check response body: " + (response.body().isEmpty() ? "<empty>" : response.body()));

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
    System.out.println("Profile response status: " + response.statusCode());
    System.out.println("Profile response body: " + (response.body().isEmpty() ? "<empty>" : response.body()));

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
