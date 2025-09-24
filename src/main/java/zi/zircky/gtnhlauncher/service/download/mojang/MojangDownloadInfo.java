package zi.zircky.gtnhlauncher.service.download.mojang;

public class MojangDownloadInfo {
  private String path;
  private String url;
  private String sha1;
  private long size;

  public MojangDownloadInfo() {}

  public MojangDownloadInfo(String path, String url, String sha1, long size) {
    this.path = path;
    this.url = url;
    this.sha1 = sha1;
    this.size = size;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String getSha1() {
    return sha1;
  }

  public void setSha1(String sha1) {
    this.sha1 = sha1;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
