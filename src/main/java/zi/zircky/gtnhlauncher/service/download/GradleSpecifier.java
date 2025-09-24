package zi.zircky.gtnhlauncher.service.download;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleSpecifier {
  private static final Pattern PATTERN = Pattern.compile(
      "([^:@]+):([^:@]+):([^:@]+)(?::([^:@]+))?(?:@([^:@]+))?"
  );

  private String invalidValue;
  @Getter
  private String groupID;
  @Getter
  private String artifactID;
  @Getter
  private String version;
  @Getter
  @Setter
  private String classifier;
  @Getter
  private String extension = "jar";
  @Getter
  private boolean valid;

  public GradleSpecifier() {
    this.valid = false;
  }

  public GradleSpecifier(String value) {
    parse(value);
  }

  public boolean parse(String value) {
    Matcher matcher = PATTERN.matcher(value);
    if (!matcher.matches()) {
      this.valid = false;
      this.invalidValue = value;
      return false;
    }

    this.groupID = matcher.group(1);
    this.artifactID = matcher.group(2);
    this.version = matcher.group(3);
    this.classifier = matcher.group(4);
    if (matcher.group(5) != null) {
      this.extension = matcher.group(5);
    }
    this.valid = true;
    return true;
  }

  public String serialize() {
    if (!valid) return invalidValue;
    StringBuilder sb = new StringBuilder();
    sb.append(groupID).append(":").append(artifactID).append(":").append(version);
    if (classifier != null &&  !classifier.isEmpty()) {
      sb.append(":").append(classifier);
    }
    if (extension != null && !extension.equals("jar")) {
      sb.append("@").append(extension);
    }
    return sb.toString();
  }

  public String getFileName() {
    if (!valid) return "";
    StringBuilder sb = new StringBuilder();
    sb.append(artifactID).append("-").append(version);
    if (classifier != null &&  !classifier.isEmpty()) {
      sb.append("-").append(classifier);
    }
    sb.append(".").append(extension);
    return sb.toString();
  }

  public String toPath(String fileNmaeOverride) {
    if (!valid) return "";
    String fileName = (fileNmaeOverride == null || fileNmaeOverride.isEmpty()) ? getFileName() : fileNmaeOverride;
    return groupID.replace('.', '/') + "/"  + artifactID + "/" + version + "/" + fileName;
  }

  public String toUrl(String repoBase) {
    if (!valid) return "";
    String base = repoBase.endsWith("/") ? repoBase : repoBase + "/";
    return base + toPath(null);
  }

  public String getArtifactPrefix() {
    return groupID + ":"  + artifactID;
  }

  public boolean matchName(GradleSpecifier other) {
    return Objects.equals(this.artifactID, other.artifactID) && Objects.equals(this.groupID, other.groupID) && Objects.equals(this.classifier, other.classifier);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GradleSpecifier)) return false;
    GradleSpecifier that = (GradleSpecifier) o;
    return Objects.equals(groupID, that.groupID) &&
        Objects.equals(artifactID, that.artifactID) &&
        Objects.equals(version, that.version) &&
        Objects.equals(classifier, that.classifier) &&
        Objects.equals(extension, that.extension);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupID, artifactID, version, classifier, extension);
  }

  @Override
  public String toString() {
    return serialize();
  }
}
