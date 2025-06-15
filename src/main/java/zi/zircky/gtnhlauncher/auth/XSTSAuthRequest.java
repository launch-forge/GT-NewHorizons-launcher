package zi.zircky.gtnhlauncher.auth;

public class XSTSAuthRequest {
  public Properties properties;
  public String relyingParty = "http://xboxlive.com";
  public String tokenType = "JWT";

  public XSTSAuthRequest(String[] userTokens) {
    this.properties = new Properties(userTokens);
  }

  public static class Properties {
    public String sandboxId = "RETAIL";
    public String[] userTokens;

    public Properties(String[] userTokens) {
      this.userTokens = userTokens;
    }
  }
}
