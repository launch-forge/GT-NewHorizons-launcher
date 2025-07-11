package zi.zircky.gtnhlauncher.controller.versionJava;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

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

    JavaFullScanner javaFullScanner = new JavaFullScanner(new File("C:/"));
    javaFullScanner.scanAll(
        msg -> Platform.runLater(() -> javaScanStatus.setText(msg)),
        java -> Platform.runLater(() -> javaTable.getItems().add(java))
    );
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

}
