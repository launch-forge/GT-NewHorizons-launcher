package zi.zircky.gtnhlauncher.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import zi.zircky.gtnhlauncher.LauncherApplication;
import zi.zircky.gtnhlauncher.gtnh.GtnhBuild;
import zi.zircky.gtnhlauncher.service.download.MinecraftLauncher;
import zi.zircky.gtnhlauncher.service.download.MinecraftUtils;
import zi.zircky.gtnhlauncher.service.download.MojangInstaller;
import zi.zircky.gtnhlauncher.service.settings.SettingsConfig;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LauncherController {

  @FXML
  private ComboBox<GtnhBuild> gtnhSelector;

  @FXML
  private Label accountName;

  @FXML
  private Label progressLabel;

  @FXML
  private Button actionButton;

  @FXML
  private ProgressBar progressBar;

  @FXML
  private Label progressBarLabel;

  @FXML
  private CheckBox releaseCheckBox;

  @FXML
  private CheckBox betaCheckBox;

  private SettingsConfig settings;

  private final File mcDir = MinecraftUtils.getMinecraftDir();
  private static final String GTNH_DOWNLOAD_LIST = "https://downloads.gtnewhorizons.com/Multi_mc_downloads/?raw";


  @FXML
  protected void initialize() {
    settings = SettingsConfig.load();

    gtnhSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
      if (selected != null) updateGtnhAction(selected);
    });

    releaseCheckBox.setOnAction(e -> loadGtnhBuilds());
    betaCheckBox.setOnAction(e -> loadGtnhBuilds());

    loadGtnhBuilds();

    progressLabel.setText("Запусков: 7 | Время в игре: 196ч");
    accountName.setText(loadAccountFromFile());
  }

  public void reloadBuildList() {
    loadGtnhBuilds();
  }

  @FXML
  private void onSettingsClicked() {
    try {
      FXMLLoader loader = new FXMLLoader(LauncherApplication.class.getResource("setting-view.fxml"));
      Parent root = loader.load();

      SettingsController controller = loader.getController();
      controller.setLauncherController(this);

      Stage stage = new Stage();
      stage.setTitle("Настройки лаунчера");
      stage.setScene(new Scene(root));
      stage.initModality(Modality.APPLICATION_MODAL); // блокирует основное окно
      stage.setResizable(false);
      stage.showAndWait();
    } catch (IOException e) {
      e.printStackTrace();
      showAlert("Ошибка", "Не удалось открыть окно настроек");
    }
  }

  @FXML
  private void onChangeAccount() {
    try {
      FXMLLoader loader = new FXMLLoader(LauncherApplication.class.getResource("account-dialog.fxml"));
      Parent root = loader.load();

      Stage dialog = new Stage();
      dialog.setTitle("Выбор аккаунта");
      dialog.setScene(new Scene(root));
      dialog.initModality(Modality.APPLICATION_MODAL);
      dialog.showAndWait();

      AccountDialogController controller = loader.getController();
      String newAccount = controller.getResult();
      if (newAccount != null) {
        accountName.setText(newAccount);
      }
    } catch (IOException e) {
      showAlert("Ошибка", "Не удалось открыть окно аккаунта.");
    }
  }

  @FXML
  private void onOpenFolder() {
    try {
      Desktop.getDesktop().open(new File("m/"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @FXML
  private void onOpenWiki() {
    try {
      Desktop.getDesktop().browse(new URI("https://wiki.gtnewhorizons.com/wiki/Main_Page"));
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @FXML
  private void onLaunch() {
    String version = "1.17.10"; // или gtnhVersionSelector
    GtnhBuild zip = gtnhSelector.getValue();
    if (version == null || version.isEmpty() && zip == null) {
      showError("Выберите версию Minecraft или GTNH.");
      return;
    }

      File jar = new File(mcDir, "versions/" + version + "/" + version + ".jar");
      if (jar.exists()) {
        runMinecraft(version);
      } else {
        installMinecraft(version);
      }

      String modpack = zip.nameToShow;
      File jarGtnh = new File(mcDir, "versions/" + modpack + "/" + modpack + ".jar");
      if (jarGtnh.exists()) {
        runMinecraft(modpack);
      } else {
        installGtnhBuild(zip);
      }
  }

  private void updateActionButton(String versionId) {
    File jar = new File(mcDir, "versions/" + versionId + "/" + versionId + ".jar");
    actionButton.setText(jar.exists() ? "Запустить" : "Установить");
  }

  private void updateGtnhAction(GtnhBuild zipName) {
    String versionName = zipName.nameToShow;
    File dir = new File(mcDir, "versions/" + versionName);
    File jar = new File(dir, versionName + ".jar");

    actionButton.setText(jar.exists() ? "Запустить" : "Установить");
  }

  private String loadAccountFromFile() {
    File file = new File("account.txt");
    if (file.exists()) {
      try {
        return Files.readString(file.toPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return "Not logged in";
  }

  private void installMinecraft(String version) {
    actionButton.setDisable(true);
    actionButton.setText("Устанавливаем...");
    progressBar.setVisible(true);
    progressBarLabel.setVisible(true);
    progressBar.setProgress(0);
    progressBarLabel.setText("0%");

    new Thread(() -> {
      try {
        MojangInstaller.installVersion(version, mcDir, (progress, message) -> {
          Platform.runLater(() -> {
            progressBar.setProgress(progress);
            progressBarLabel.setText((int) (progress * 100) + "% • " + message);
          });
        });

        Platform.runLater(() -> {
          progressBarLabel.setText("✅ Готово!");
          updateActionButton(version);
          showInfo("Установка завершена", "Версия " + version + " установлена.");
        });
      } catch (Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> showError("Ошибка установки: " + e.getMessage()));
      } finally {
        Platform.runLater(() -> {
          actionButton.setDisable(false);
          new Thread(() -> {
            try {
              Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
            Platform.runLater(() -> {
              progressBar.setVisible(false);
              progressBarLabel.setVisible(false);
            });
          }).start();
        });
      }
    }).start();
  }

  private void installGtnhBuild(GtnhBuild build) {
    String versionId = build.nameToShow;
    String downloadUrl = build.downloadUrl;
    File zipFile = new File("gtnh_temp.zip");
    File versionsDir = new File(mcDir, "versions");

    actionButton.setDisable(true);
    progressBar.setVisible(true);
    progressLabel.setVisible(true);
    progressBar.setProgress(0);
    progressLabel.setText("Скачиваем GTNH...");
    new Thread(() -> {
      try {
        // ========== ШАГ 1. СКАЧИВАНИЕ ==========
        URL url = new URL(downloadUrl);
        URLConnection connection = url.openConnection();
        int contentLength = connection.getContentLength();

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(zipFile)) {

          byte[] buffer = new byte[4096];
          int bytesRead;
          int totalRead = 0;

          while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            totalRead += bytesRead;

            final double progress = (double) totalRead / contentLength;
            Platform.runLater(() -> {
              progressBar.setProgress(progress);
              progressLabel.setText("Скачивание: " + (int)(progress * 100) + "%");
            });
          }
        }

        // ========== ШАГ 2. РАСПАКОВКА ==========
        Platform.runLater(() -> progressLabel.setText("Распаковка..."));
        unzipWithProgress(zipFile, versionsDir);

        // ========== ШАГ 3. ГОТОВО ==========
        zipFile.delete();
        Platform.runLater(() -> {
          updateGtnhAction(build);
          showInfo("GTNH установлена", "Сборка " + versionId + " установлена.");
        });

      } catch (IOException e) {
        Platform.runLater(() -> showError("Ошибка установки GTNH: " + e.getMessage()));
      } finally {
        Platform.runLater(() -> {
          actionButton.setDisable(false);
          // скрываем через 2 секунды
          new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
              progressBar.setVisible(false);
              progressBarLabel.setVisible(false);
            });
          }).start();
        });
      }
    }).start();
  }

  private void runMinecraft(String version) {
    try {
      File javaFile = new File(SettingsConfig.load().getJavaPath());
      int ram = SettingsConfig.load().getAllocatedRam();
      String username = "Player"; // Пока без авторизации

      MinecraftLauncher.launch(javaFile, ram, username, mcDir, version);
    } catch (Exception e) {
      e.printStackTrace();
      showError("Не удалось запустить Minecraft: " + e.getMessage());
    }
  }

  private void loadGtnhBuilds() {
    int javaVersion = settings.getVersionJava();
    new Thread(() -> {
      try {
        List<GtnhBuild> builds = new ArrayList<>();
        URL url = new URL(GTNH_DOWNLOAD_LIST);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            if (!line.endsWith(".zip")) continue;

            GtnhBuild build = new GtnhBuild(line);
            String lower = line.toLowerCase();
            boolean isBeta = lower.contains("beta");
            boolean isRelease = !isBeta; // всё остальное — релиз

            boolean isJava8 = lower.contains("java_8");
            boolean isJava17 = lower.matches(".*java_1[7-9].*|.*java_2[0-1].*");

            boolean matchesJava = (javaVersion == 8 && isJava8) || (javaVersion == 17 && isJava17);

            if (matchesJava && ((releaseCheckBox.isSelected() && isRelease)
                || (betaCheckBox.isSelected() && isBeta))) {
              builds.add(build);
            }
          }
        }

        Platform.runLater(() -> {
          gtnhSelector.getItems().setAll(builds);
          if (!builds.isEmpty()) gtnhSelector.getSelectionModel().selectFirst();
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();
  }

  private void unzipWithProgress(File zipFile, File outputDir) throws IOException {
    // Подсчёт общего количества записей (для прогресса)
    int entryCount = 0;
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      while (zis.getNextEntry() != null) entryCount++;
    }

    int currentEntry = 0;

    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        File newFile = new File(outputDir, entry.getName());

        if (entry.isDirectory()) {
          newFile.mkdirs();
        } else {
          newFile.getParentFile().mkdirs();
          try (FileOutputStream fos = new FileOutputStream(newFile)) {
            zis.transferTo(fos);
          }
        }

        currentEntry++;
        final double progress = (double) currentEntry / entryCount;
        final String name = entry.getName();

        Platform.runLater(() -> {
          progressBar.setProgress(progress);
          progressLabel.setText("Распаковка: " + (int)(progress * 100) + "% • " + name);
        });

        zis.closeEntry();
      }
    }
  }

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private void showInfo(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private void showError(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private void showError(String message) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Ошибка");
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}