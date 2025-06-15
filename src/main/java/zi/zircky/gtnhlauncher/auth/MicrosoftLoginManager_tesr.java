package zi.zircky.gtnhlauncher.auth;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class MicrosoftLoginManager_tesr {
  private static final String CLIENT_ID = "00000000402b5328"; // официальное Mojang Minecraft client ID
  private static final String REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
  private static final int PORT = 8080;
  private static final String SCOPE = "XboxLive.signin offline_access";

  public MinecraftSession login() throws Exception {
    OAuth20Service service = new ServiceBuilder(CLIENT_ID)
        .defaultScope(SCOPE)
        .callback(REDIRECT_URI)
        .build(new MicrosoftOAuthApi());

    String authUrl = service.getAuthorizationUrl();

    // Открыть браузер
    Desktop.getDesktop().browse(new URI(authUrl));

    // Получить код авторизации через локальный сервер
//    String code = waitForCode();

    System.out.println("Enter the code from the browser:");
    String code = new Scanner(System.in).nextLine();

    // Получить токен Microsoft
    OAuth2AccessToken msToken = service.getAccessToken(code);

    // Продолжить цепочку авторизации
    String xboxToken = XboxAuthenticator.getXboxLiveToken(msToken.getAccessToken());
    XboxAuthenticator.XSTS xsts = XboxAuthenticator.getXstsToken(xboxToken);
    String mcToken = XboxAuthenticator.getMinecraftToken(xsts.userHash, xsts.token);

    return XboxAuthenticator.getMinecraftProfile(mcToken, msToken.getRefreshToken());
  }

  private String waitForCode() throws IOException {
    final String[] codeHolder = new String[1];
    HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

    server.createContext("/callback", exchange -> {
      String query = exchange.getRequestURI().getQuery();
      String code = Arrays.stream(query.split("&"))
          .filter(p -> p.startsWith("code="))
          .map(p -> p.substring("code=".length()))
          .findFirst()
          .orElse(null);

      String response = "<html><body><h2>Authorization was successful. You can close the window.</h2></body></html>";
      exchange.sendResponseHeaders(200, response.getBytes().length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(response.getBytes());
      }

      codeHolder[0] = code;
      Executors.newSingleThreadExecutor().submit(() -> {
        try {
          Thread.sleep(1000);
          server.stop(0);
        } catch (InterruptedException ignored) {}
      });
    });

    server.setExecutor(Executors.newCachedThreadPool());
    server.start();

    System.out.println("Ожидаем код авторизации...");

    // Ждём, пока не получим код
    while (codeHolder[0] == null) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    return codeHolder[0];
  }

  private static class MicrosoftOAuthApi extends DefaultApi20 {
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
