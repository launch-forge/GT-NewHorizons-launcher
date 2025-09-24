package zi.zircky.gtnhlauncher.service.download;

@FunctionalInterface
public interface  ProgressCallback {
  void update(double progress, String message);
}