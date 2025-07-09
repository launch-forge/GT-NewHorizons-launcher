package zi.zircky.gtnhlauncher.controller;

import com.sun.management.OperatingSystemMXBean;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import zi.zircky.gtnhlauncher.controller.settings.LauncherSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;

public class SettingsController {
  @FXML
  private Slider ramSlider;
  @FXML
  private Label ramLabel;
  @FXML
  private TextField javaPathField;

  private LauncherSettings settings;

  @FXML
  public void initialize() {
    settings = LauncherSettings.load();
    int systemMax = getMaxAllowedRam();
    int maxRecommended = Math.max(2, systemMax - 2); // 2 ГБ оставим для системы

    ramSlider.setMax(maxRecommended);
    ramSlider.setValue(Math.min(settings.getAllocatedRam(), maxRecommended));
    javaPathField.setText(settings.getJavaPath());
    ramLabel.setText((int) ramSlider.getValue() + " ГБ");

    ramSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
      int value = newVal.intValue();
      ramLabel.setText(value + " ГБ");

      if (value > maxRecommended) {
        ramLabel.setStyle("-fx-text-fill: red;");
        Tooltip.install(ramSlider, new Tooltip("Выделено слишком много памяти!"));
      } else {
        ramLabel.setStyle("-fx-text-fill: #00ffaa;");
        Tooltip.uninstall(ramSlider, null);
        settings.setAllocatedRam(value);
        settings.save();
      }
    });

    javaPathField.textProperty().addListener((obs, oldVal, newVal) -> {
      if (isValidJavaPath(newVal)) {
        settings.setJavaPath(newVal);
        settings.save();
        javaPathField.setStyle("-fx-border-color: green;");
        Tooltip.install(javaPathField, new Tooltip("Путь к Java действителен."));
      } else {
        javaPathField.setStyle("-fx-border-color: red;");
        Tooltip.install(javaPathField, new Tooltip("Ошибка: недопустимый путь к Java."));
      }
    });
  }

  @FXML
  public void onSelectJavaPath() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Выберите Java");
    File file = chooser.showOpenDialog(null);
    if (file != null) {
      javaPathField.setText(file.getAbsolutePath());
    }
    if (isValidJavaPath(file.getAbsolutePath())) {
      javaPathField.setText(file.getAbsolutePath());
    } else {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Ошибка");
      alert.setHeaderText("Неверный путь к Java");
      alert.setContentText("Выбранный файл не является допустимой исполняемой Java.");
      alert.showAndWait();
    }
  }

  private boolean isValidJavaPath(String path) {
    File file = new File(path);
    if (!file.exists() || !file.isFile()) {
      return false;
    }

    String fileName = file.getName().toLowerCase();
    if (!(fileName.equals("java") || fileName.equals("java.exe"))) {
      return false;
    }

    try {
      Process process = new ProcessBuilder(path, "-version")
          .redirectErrorStream(true)
          .start();

      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line = reader.readLine();
        return line != null && line.contains("version");
      }
    } catch (IOException e) {
      return false;
    }
  }

  private int getMaxAllowedRam() {
    long maxMemoryBytes = Runtime.getRuntime().maxMemory(); // только для JVM
    try {
      OperatingSystemMXBean os =
          (OperatingSystemMXBean)
              ManagementFactory.getOperatingSystemMXBean();

      long totalPhysical = os.getTotalMemorySize();
      return (int) (totalPhysical / 1024 / 1024 / 1024); // в ГБ
    } catch (Exception e) {
      return 8; // fallback
    }
  }
}
