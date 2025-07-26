package zi.zircky.gtnhlauncher.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import zi.zircky.gtnhlauncher.auth.AuthStorage;
import zi.zircky.gtnhlauncher.auth.MicrosoftLoginManager;

import java.awt.*;
import java.net.URI;
import java.util.logging.Logger;

public class
AccountDialogController {
  Logger logger = Logger.getLogger(getClass().getName());

  @FXML
  private ComboBox<String> accountTypeSelector;

  @FXML
  private TextField usernameField;

  @FXML
  private Label statusLabel;

  private MicrosoftLoginManager loginManager = new MicrosoftLoginManager();

  private String result = null;
  private AuthStorage.AuthInfo auth = AuthStorage.load();

  public void initialize() {

    accountTypeSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
      if (selected != null) authList(selected);
    });
  }

  @FXML
  private void onSave() {
    String username = usernameField.getText().trim();
    if (username.isEmpty()) {
      showAlert("Ошибка", "Введите имя пользователя");
    }
    result = username;
    ((Stage) usernameField.getScene().getWindow()).close();
  }

  @FXML
  private void onCancel() {
    result = null;
    ((Stage) usernameField.getScene().getWindow()).close();
  }

  @FXML
  private void onMicrosoftLogin() {
    try {
      String authUrl = loginManager.getAuthorizationUrl();
      if (authUrl == null || authUrl.isEmpty()) {
        statusLabel.setText("Ошибка: не удалось получить URL авторизации.");
        return;
      }

      // Open the default browser
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(new URI(authUrl));
        statusLabel.setText("Пожалуйста, войдите через Microsoft в браузере...");
      } else {
        statusLabel.setText("Ошибка: браузер не поддерживается.");
        return;
      }

      // Start authentication process
      loginManager.login(
          profile ->
            Platform.runLater(() ->
              statusLabel.setText("Успех! Игрок: " + profile[0] + ", UUID: " + profile[1])
              // Use profile[2] (minecraftToken) to launch Minecraft
            ),
          error ->
            Platform.runLater(() -> {
              logger.info(error);
              statusLabel.setText("Ошибка: " + error);
            })

      );
    } catch (Exception e) {
      statusLabel.setText("Ошибка: " + e.getMessage());
    }
  }

  private void authList(String list) {
    System.out.println(list);
  }

  public String getResult() {
    return result;
  }

  private void showAlert(String title, String msg) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setContentText(msg);
    alert.showAndWait();
  }
}
