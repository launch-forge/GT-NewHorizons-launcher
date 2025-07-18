package zi.zircky.gtnhlauncher.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import zi.zircky.gtnhlauncher.service.settings.versionJava.JavaInstallation;
import zi.zircky.gtnhlauncher.service.settings.versionJava.JavaScannerTask;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

public class JavaSelectController {
  @FXML
  private TableView<JavaInstallation> javaTable;
  @FXML
  private TableColumn<JavaInstallation, String> versionColumn;
  @FXML
  private TableColumn<JavaInstallation, String> pathColumn;
  @FXML
  private Label javaScanStatus;

  private JavaInstallation selectedJava = null;

  @FXML
  public void initialize() {
    versionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVersion()));
    pathColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPath()));

    javaScanStatus.setVisible(true);
    javaScanStatus.setText("\uD83D\uDD0D Идёт поиск Java на диске C...");

    startJavaScan();
  }

  public void setJavaList(List<JavaInstallation> javaList) {
    if (javaTable != null) {
      javaTable.getItems().setAll(javaList);
    }
  }

  public JavaInstallation getSelectedJava() {
    return selectedJava;
  }

  @FXML
  private void onSelect() {
    selectedJava = javaTable.getSelectionModel().getSelectedItem();
    ((Stage) javaTable.getScene().getWindow()).close();
  }

  @FXML
  private void onCancel() {
    selectedJava = null;
    ((Stage) javaTable.getScene().getWindow()).close();
  }

  private void startJavaScan() {
    Set<String> seenPaths = ConcurrentHashMap.newKeySet();
    Consumer<String> onUpdata = msg -> Platform.runLater(() -> javaScanStatus.setText(msg));
    Consumer<JavaInstallation> onFound = java -> Platform.runLater(() -> javaTable.getItems().add(java));

    ForkJoinPool.commonPool().submit(() -> {
      File root = new File("C:\\Program Files");
      long startTime = System.nanoTime();

      JavaScannerTask task = new JavaScannerTask(root, 0, 4, seenPaths, onUpdata, onFound, root);
      List<JavaInstallation> result = task.invoke();

      long endTime = System.nanoTime();   // ⏱️ Время окончания
      long durationMillis = (endTime - startTime) / 1_000_000;
      double durationSeconds = durationMillis / 1000.0;

      String timeInfo = String.format("✅ Поиск завершён за %.2f сек. Найдено: %d", durationSeconds, result.size());

      System.out.println("[INFO] " + timeInfo); // лог в консоль

      Platform.runLater(() -> javaScanStatus.setText(timeInfo));
    });
  }
}
