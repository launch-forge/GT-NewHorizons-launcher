package zi.zircky.gtnhlauncher.service.gtnh;

public class GtnhBuild {
  public final String nameToShow;
  public final String zipFilename;
  public final String downloadUrl;

  public GtnhBuild(String zipFilename) {
    this.zipFilename = zipFilename;
    this.downloadUrl = zipFilename;

    // Например: GT_New_Horizons_Release_2.4.0.zip, GT_New_Horizons_2.7.0_Java_17-21.zip → GTNH_2.4.0
    this.nameToShow = "GTNH " + zipFilename
        .replace("GT_New_Horizons_", "")
        .replace("http://downloads.gtnewhorizons.com/Multi_mc_downloads/", "")
        .replace("betas/", "")
        .replace("-beta-", " Beta ")
        .replace("-RC-", " RC ")
        .replace("_Java_", " Java ")
        .replace(".zip", "");
  }

  @Override
  public String toString() {
    return nameToShow;
  }
}
