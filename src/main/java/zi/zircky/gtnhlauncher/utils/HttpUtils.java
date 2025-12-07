package zi.zircky.gtnhlauncher.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpUtils {
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String USER_AGENT = "GTNHLauncher/1.0";
  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(30))
      .build();
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

  public static JsonNode postJson(String url, String body) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(REQUEST_TIMEOUT)
        .header("User-Agent", USER_AGENT)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();

    return sendJsonRequest(request);
  }

  public static JsonNode getJson(String url, String bearerToken) throws IOException, InterruptedException {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(REQUEST_TIMEOUT)
        .header("User-Agent", USER_AGENT)
        .header("Accept", "application/json");

    if (bearerToken != null && !bearerToken.isBlank()) {
      builder.header("Authorization", "Bearer " + bearerToken);
    }

    return sendJsonRequest(builder.GET().build());
  }

  private static JsonNode sendJsonRequest(HttpRequest request) throws IOException, InterruptedException {
    HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

    int statusCode = response.statusCode();
    if (statusCode < 200 || statusCode >= 300) {
      throw new IOException(
          "HTTP " + statusCode + " from " + request.uri() + ": " + response.body());
    }

    try {
      return mapper.readTree(response.body());
    } catch (IOException e) {
      throw new IOException("Failed to parse JSON from " + request.uri(), e);
    }
  }
}
