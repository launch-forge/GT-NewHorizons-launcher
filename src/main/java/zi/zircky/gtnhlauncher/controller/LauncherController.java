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
import zi.zircky.gtnhlauncher.controller.settings.LauncherSettings;
import zi.zircky.gtnhlauncher.service.download.MinecraftLauncher;
import zi.zircky.gtnhlauncher.service.download.MinecraftUtils;
import zi.zircky.gtnhlauncher.service.download.MojangInstaller;


import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static zi.zircky.gtnhlauncher.gtnh.ZipUtils.unzip;

public class LauncherController {

  @FXML
  private ComboBox<String> versionSelector;

  @FXML
  private ComboBox<String> gtnhSelector;

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

  private final File mcDir = MinecraftUtils.getMinecraftDir();
  private static final String GTNH_DOWNLOAD_LIST = "https://downloads.gtnewhorizons.com/Multi_mc_downloads/?raw";


  @FXML
  protected void initialize() {
    versionSelector.getItems().addAll("1.7.10", "1.20.1");
    versionSelector.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
      if (selected != null) {
        updateActionButton(selected);
      }
    });

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
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @FXML
  private void onLaunch() {
    String version = versionSelector.getValue(); // или gtnhVersionSelector
    String zip = gtnhSelector.getValue();
    if (version == null || version.isEmpty() && zip == null || zip.isEmpty()) {
      showError("Выберите версию Minecraft или GTNH.");
      return;
    }

      File jar = new File(mcDir, "versions/" + version + "/" + version + ".jar");
      if (jar.exists()) {
        runMinecraft(version);
      } else {
        installMinecraft(version);
      }

      String modpack = zip.replace(".zip", "");
      File jarGtnh = new File(mcDir, "versions/" + version + "/" + version + ".jar");
      if (jar.exists()) {
        runMinecraft(version);
      } else {
        installGtnhBuild(zip);
      }
  }

  private void updateActionButton(String versionId) {
    File jar = new File(mcDir, "versions/" + versionId + "/" + versionId + ".jar");
    actionButton.setText(jar.exists() ? "Запустить" : "Установить");
  }

  private void updateGtnhAction(String zipName) {
    String versionName = zipName.replace(".zip", "");
    File dir = new File(mcDir, "versions/" + versionName);
    File jar = new File(dir, versionName + ".jar");
    actionButton.setText(jar.exists() ? "Запустить" : "Установить");
  }

  // Проверка: установлена ли версия
  private boolean isVersionInstalled(String versionId) {
    File versionDir = new File(mcDir, "versions/" + versionId);
    File jar = new File(versionDir, versionId + ".jar");
    return jar.exists();
  }

  private void saveAccountToFile(String name) {
    try (FileWriter writer = new FileWriter("account.txt")) {
      writer.write(name);
    } catch (IOException e) {
      e.printStackTrace();
    }
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

  private void installGtnhBuild(String version) {
    actionButton.setDisable(true);
    actionButton.setText("Устанавливаем GTNH...");
    progressBar.setVisible(true);
    progressLabel.setVisible(true);

    String downloadUrl = "https://downloads.gtnewhorizons.com/Multi_mc_downloads/" + version;

    File zipFile = new File("gtnh_temp.zip");
    File versionsDir = new File(mcDir, "versions");

    new Thread(() -> {
      try {
        // Скачиваем ZIP
        try (InputStream in = new URL(downloadUrl).openStream()) {
          Files.copy(in, zipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        // Распаковываем
        unzip(zipFile, versionsDir);
        zipFile.delete();

        Platform.runLater(() -> {
          updateActionButton(version);
          showInfo("Установка GTNH", "Сборка установлена: " + version);
        });
      } catch (IOException e) {
        Platform.runLater(() -> showError("Ошибка установки GTNH: " + e.getMessage()));
      } finally {
        Platform.runLater(() -> {
          actionButton.setDisable(false);
          progressBar.setVisible(false);
          progressLabel.setVisible(false);
        });
      }
    }).start();
  }

  private void runMinecraft(String version) {
    try {
      File javaFile = new File(LauncherSettings.load().getJavaPath());
      int ram = LauncherSettings.load().getAllocatedRam();
      String username = "Player"; // Пока без авторизации

      MinecraftLauncher.launch(javaFile, ram, username, mcDir, version);
    } catch (Exception e) {
      e.printStackTrace();
      showError("Не удалось запустить Minecraft: " + e.getMessage());
    }
  }

  private void loadGtnhBuilds() {
    new Thread(() -> {
      try {
        List<String> allVersions = downloadVersionList();
        List<String> filtered = filterByType(allVersions);

        Platform.runLater(() -> {
          gtnhSelector.getItems().setAll(filtered);
          gtnhSelector.getSelectionModel().selectFirst();
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();
  }

  private List<String> downloadVersionList() throws IOException {
    List<String> versions = new ArrayList<>();
    URL url = new URL(GTNH_DOWNLOAD_LIST);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.endsWith(".zip")) {
          versions.add(line);
        }
      }
    }
    return versions;
  }

  private List<String> filterByType(List<String> allVersions) {
    List<String> result = new ArrayList<>();
    for (String version : allVersions) {
      boolean isRelease = version.toLowerCase().contains("release");
      boolean isBeta = version.toLowerCase().contains("beta");

      if ((releaseCheckBox.isSelected() && isRelease) ||
          (betaCheckBox.isSelected() && isBeta) ||
          (!isRelease && !isBeta)) {
        result.add(version);
      }
    }
    return result;
  }

  private boolean isGtnh(String versionName) {
    return versionName.toLowerCase().contains("gtnh") || versionName.toLowerCase().contains("newhorizons");
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