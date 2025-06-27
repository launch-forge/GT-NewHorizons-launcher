package zi.zircky.gtnhlauncher.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import zi.zircky.gtnhlauncher.LauncherApplication;
import zi.zircky.gtnhlauncher.loading.MinecraftLauncher;
import zi.zircky.gtnhlauncher.loading.MinecraftUtils;
import zi.zircky.gtnhlauncher.settings.LauncherSettings;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

public class LauncherController {
  @FXML
  private ComboBox<String> versionSelector;

  @FXML
  private ComboBox<String> modpackSelector;

  @FXML
  private Label accountName;

  @FXML
  private Label progressLabel;

  @FXML
  protected void initialize() {
    versionSelector.getItems().addAll("GTNH 2.4.1-Release", "GTNH 2.3.9");
    versionSelector.getSelectionModel().selectFirst();

    modpackSelector.getItems().addAll("Default", "Custom");
    modpackSelector.getSelectionModel().selectFirst();

    progressLabel.setText("Запусков: 7 | Время в игре: 196ч");
    accountName.setText(loadAccountFromFile());
  }

  @FXML
  private void onSettingsClicked() {
    try {
      FXMLLoader loader = new FXMLLoader(LauncherApplication.class.getResource("settings.fxml"));
      Parent root = loader.load();
      Stage dialog = new Stage();
      dialog.setTitle("Настройки");
      dialog.setScene(new Scene(root));
      dialog.initModality(Modality.APPLICATION_MODAL);
      dialog.showAndWait();

      SettingsController settingsController = loader.getController();
//      String newSetting = settingsController

    } catch (Exception e) {
      showAlert("Ошибка", "Окно настроек пока не реализовано.");
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
    LauncherSettings settings = LauncherSettings.load();

    File javaFile = new File(settings.getJavaPath());
    if (!javaFile.exists()) {
      showAlert("Java не найдена", "Путь к Java недействителен.");
      return;
    }

    File gameDir = MinecraftUtils.getMinecraftDir();
    String version = "1.7.10";
    String username = "Player";

    try {
      MinecraftLauncher.launch(javaFile, settings.getAllocatedRam(), username, gameDir, version);
    } catch (IOException e) {
      e.printStackTrace();
      showAlert("Ошибка запуска", e.getMessage());
    }

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

  private void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }
}