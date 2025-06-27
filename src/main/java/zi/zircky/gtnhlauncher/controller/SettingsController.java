package zi.zircky.gtnhlauncher.controller;


import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import zi.zircky.gtnhlauncher.settings.LauncherSettings;

import java.io.File;
import java.lang.management.OperatingSystemMXBean;

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
    int systemMax = getMaxAllowedRam();
    int maxRecommendad = Math.max(2, systemMax - 2);

    settings = LauncherSettings.load();

    ramSlider.setMax(maxRecommendad);
    ramSlider.setValue(Math.min(settings.getAllocatedRam(), maxRecommendad));

    javaPathField.setText(settings.getJavaPath());

    ramLabel.setText(String.valueOf(ramSlider.getValue()));
    ramSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
      int value = newVal.intValue();
      ramLabel.setText(String.valueOf(value));

      if (value > maxRecommendad) {
        ramLabel.setStyle("-fx-text-fill: red;");
        Tooltip.install(ramSlider, new Tooltip("Выделено слишком много памяти!"));
      } else {
        ramLabel.setStyle("-fx-text-fill: #00ffaa");
        Tooltip.uninstall(ramSlider, null);
        settings.setAllocatedRam(value);
        settings.save();
      }
    });

    javaPathField.textProperty().addListener((obs, oldVal, newVal) -> {
      settings.setJavaPath(newVal);
      settings.save();
    });
  }

  @FXML
  public void onSelectJavaPath() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Выберите исполняемый файл Java");
    File file = chooser.showOpenDialog(null);
    if (file != null) {
      javaPathField.setText(file.getAbsolutePath());
    }
  }

  private int getMaxAllowedRam() {
    long maxMemoryBytes = Runtime.getRuntime().maxMemory();
    try {
      com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();

      long totalPhysical = os.getTotalMemorySize();
      return (int) (totalPhysical / 1024 / 1024 / 1024);
    } catch (Exception e) {
      return 8;
    }
  }
}
