package zi.zircky.gtnhlauncher.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import zi.zircky.gtnhlauncher.auth.AuthStorage;
import zi.zircky.gtnhlauncher.service.download.MinecraftLauncher;
import zi.zircky.gtnhlauncher.service.download.MojangInstaller;
import zi.zircky.gtnhlauncher.service.gtnh.GtnhBuild;
import zi.zircky.gtnhlauncher.service.settings.SettingsConfig;
import zi.zircky.gtnhlauncher.utils.MinecraftUtils;

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
      Desktop.getDesktop().open(new File(System.getenv("APPDATA"), "/.gtnh-launcher"));
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
    GtnhBuild zip = gtnhSelector.getValue();
    if (zip == null) {
      showError("Выберите версию Minecraft или GTNH.");
      return;
    }

//      File jar = new File(mcDir, "versions/" + version + "/" + version + ".jar");
//      if (jar.exists()) {
//        runMinecraft(version);
//      } else {
//        installMinecraft(version);
//      }

      String modpack = zip.nameToShow;
      if (isGTNHInstalled()) {
        runMinecraft();
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
    actionButton.setText(isGTNHInstalled() ? "Запустить" : "Установить");
  }

  private String loadAccountFromFile() {
    File file = new File(MinecraftUtils.getMinecraftDir(), "account.json");
    if (file.exists()) {
      try {
        String content = Files.readString(file.toPath());
        JsonObject json = JsonParser.parseString(content).getAsJsonObject();
        return json.get("username").getAsString();
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
            progressBarLabel.setText((int)(progress * 100) + "% • " + message);
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
            } catch (InterruptedException ignored) {}
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
    File versionsDir = new File(mcDir.toURI());

    actionButton.setDisable(true);
    progressBar.setVisible(true);
    progressBarLabel.setVisible(true);
    progressBar.setProgress(0);
    progressBarLabel.setText("Скачиваем GTNH...");

    new Thread(() -> {
      try {
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
              progressBarLabel.setText("Скачивание: " + (int)(progress * 100) + "%");
            });
          }
        }

        Platform.runLater(() -> progressBarLabel.setText("Распаковка..."));
        unzipWithProgress(zipFile, versionsDir);
        zipFile.delete();

        Platform.runLater(() -> {
          updateGtnhAction(build);
          showInfo("GTNH установлена", "Сборка " + versionId + " установлена.");
        });

      } catch (IOException e) {
        Platform.runLater(() -> showError("Ошибка установки GTNH: " + e.getMessage()));
      }  finally {
        Platform.runLater(() -> {
          actionButton.setDisable(false);
          new Thread(() -> {
            Platform.runLater(() -> {
              progressBar.setVisible(false);
              progressBarLabel.setVisible(false);
            });
          }).start();
        });
      }
    }).start();
  }

  private void runMinecraft() {
    try {
      // Получаем настройки
      SettingsConfig config = SettingsConfig.load();
      File javaFile = new File(config.getJavaPath());
      int ram = config.getAllocatedRam();
      AuthStorage.AuthInfo auth = AuthStorage.load();
      boolean isJava17 = config.isVersionJava();

      ProcessBuilder builder = MinecraftLauncher.launch(javaFile, ram, auth.username, auth.uuid, auth.accessToken, isJava17);
      Process process = builder.start();
      System.out.println("Runned Minecraft PID: " + process.pid());

// Поток вывода Minecraft
      new Thread(() -> {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            System.out.println("[MC-OUT] " + line);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();

// Поток ошибок Minecraft
      new Thread(() -> {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            System.err.println("[MC-ERR] " + line);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();

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
            boolean isJava17 = lower.matches(".*java_1[7-9].*|.*java_2[0-9].*");

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
    List<String> allowedTopLevelDirs = List.of(".minecraft", "libraries", "patches", "gtnh_icon.png", "instance.cfg", "mmc-pack.json");

    // Подсчёт общего количества записей (для прогресса)
    int entryCount = 0;
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      while (zis.getNextEntry() != null) entryCount++;
    }

    int currentEntry = 0;

    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String normalizedName = normalizeEntryName(entry.getName());
        if (!isAllowed(normalizedName, allowedTopLevelDirs)) continue;

        File outFile = new File(outputDir, normalizedName);

        if (entry.isDirectory()) {
          outFile.mkdirs();
        } else {
          outFile.getParentFile().mkdirs();
          try (FileOutputStream fos = new FileOutputStream(outFile)) {
            zis.transferTo(fos);
          }
        }

        currentEntry++;
        final double progress = (double) currentEntry / entryCount;
        final String displayName = normalizedName;

        Platform.runLater(() -> {
          progressBar.setProgress(progress);
          progressBarLabel.setText("Распаковка: " + (int)(progress * 100) + "% • " + displayName);
        });

        zis.closeEntry();
      }
    }
  }

  private boolean isAllowed(String entryName, List<String> allowed) {
    for (String allowedPrefix : allowed) {
      if (entryName.startsWith(allowedPrefix)) {
        return true;
      }
    }

    return false;
  }

  private String normalizeEntryName(String name) {
    int firstSlash = name.indexOf('/');
    if (firstSlash != 1 && name.length() > firstSlash + 1) {
      return name.substring(firstSlash + 1);
    }
    return name;
  }

  private boolean isGTNHInstalled() {
    System.out.println(mcDir);
    return new File(mcDir, ".minecraft/config").exists() && new File(mcDir, ".minecraft/mods").exists();
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