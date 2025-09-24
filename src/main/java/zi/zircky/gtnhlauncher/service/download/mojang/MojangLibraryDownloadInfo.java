package zi.zircky.gtnhlauncher.service.download.mojang;

import java.util.HashMap;
import java.util.Map;

public class MojangLibraryDownloadInfo {
  private MojangDownloadInfo artifact;
  private Map<String, MojangDownloadInfo> classifiers = new HashMap<>();

  public MojangLibraryDownloadInfo() {}

  public MojangLibraryDownloadInfo(MojangDownloadInfo artifact) {
    this.artifact = artifact;
  }

  public MojangDownloadInfo getDownloadInfo(String classifier) {
    if (classifier == null) return artifact;
    return classifiers.get(classifier);
  }

  public MojangDownloadInfo getArtifact() {
    return artifact;
  }

  public void setArtifact(MojangDownloadInfo artifact) {
    this.artifact = artifact;
  }

  public Map<String, MojangDownloadInfo> getClassifiers() {
    return classifiers;
  }

  public void setClassifiers(Map<String, MojangDownloadInfo> classifiers) {
    this.classifiers = classifiers;
  }
}
