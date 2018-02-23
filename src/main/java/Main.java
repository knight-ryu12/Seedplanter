import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Seedplanter");
        primaryStage.setScene(new Scene(FXMLLoader.load(getClass().getResource("MainGUI.fxml")), 400, 300));
        primaryStage.show();
    }

    public static void showAlertBox(String Title, String Header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Title);
        alert.setHeaderText(Header); //can be null
        alert.setContentText(content);
        alert.showAndWait();
    }
}
