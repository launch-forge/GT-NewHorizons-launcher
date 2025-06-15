package zi.zircky.gtnhlauncher.auth;

public class XboxLiveAuthRequest {
  public Properties properties;
  public String relyingParty = "http://auth.xboxlive.com";
  public String tokenType = "JWT";

  public XboxLiveAuthRequest(String authMethod, String siteName, String rpsTicket) {
    this.properties = new Properties(authMethod, siteName, rpsTicket);
  }

  public static class Properties {
    public String authMethod;
    public String siteName;
    public String rpsTicket;

    public Properties(String authMethod, String siteName, String rpsTicket) {
      this.authMethod = authMethod;
      this.siteName = siteName;
      this.rpsTicket = rpsTicket;
    }
  }
}
