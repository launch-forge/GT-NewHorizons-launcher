package zi.zircky.gtnhlauncher.controller.versionJava;

import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.util.List;

public class JavaSelectController {
  @FXML
  private TableView<JavaInstallation> javaTable;
  @FXML
  private TableColumn<JavaInstallation, String> versionColumn;
  @FXML
  private TableColumn<JavaInstallation, String> pathColumn;

  private JavaInstallation selectedJava = null;

  @FXML
  public void initialize() {
    versionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVersion()));
    pathColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPath()));
  }

  public void setJavaList(List<JavaInstallation> javaList) {
    javaTable.getItems().setAll(javaList);
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
