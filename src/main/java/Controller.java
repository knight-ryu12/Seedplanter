import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

public class Controller {
    @FXML private TextField dsiWare_TextField, movableSed_TextField, injectionZip_TextField, ctcert_TextField;

    // 0 is dsiWare, 1 is movableSed, 2 is injectionZip, 3 is ctcert
    private static String[] TextFields_Strings = new String[4];

    @FXML private void dsiWare_UpdateTextField() { TextFields_Strings[0] = dsiWare_TextField.getText(); }

    @FXML private void dsiWare_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select your DSiWare .bin file");
        TextFields_Strings[0] = fileChooser.showOpenDialog(new Stage()).getAbsolutePath();
        dsiWare_TextField.setText(TextFields_Strings[0]);
    }

    @FXML private void movableSed_UpdateTextField() { TextFields_Strings[1] = movableSed_TextField.getText(); }

    @FXML private void movableSed_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select your movable.sed file");
        TextFields_Strings[1] = fileChooser.showOpenDialog(new Stage()).getAbsolutePath();
        movableSed_TextField.setText(TextFields_Strings[1]);
    }

    @FXML private void injectionZip_UpdateTextField() { TextFields_Strings[2] = injectionZip_TextField.getText(); }

    @FXML private void injectionZip_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select your ZIP injection file");
        TextFields_Strings[2] = fileChooser.showOpenDialog(new Stage()).getAbsolutePath();
        injectionZip_TextField.setText(TextFields_Strings[2]);
    }

    @FXML private void ctcert_UpdateTextField() { TextFields_Strings[3] = ctcert_TextField.getText(); }

    @FXML private void ctcert_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select your ctcert.bin file");
        TextFields_Strings[3] = fileChooser.showOpenDialog(new Stage()).getAbsolutePath();
        ctcert_TextField.setText(TextFields_Strings[3]);
    }

    @FXML private void injectButton() {
        TextFields_Strings[0] = "C:\\Users\\jason\\Desktop\\TADpole_jason\\4B554E56.bin";
        TextFields_Strings[1] = "C:\\Users\\jason\\Desktop\\TADpole_jason\\resources\\movable.sed";
        TextFields_Strings[2] = "C:\\Users\\jason\\Desktop\\DSiWare_eur_sudokuhax_injection.zip";
        TextFields_Strings[3] = "C:\\Users\\jason\\Desktop\\ctcert\\angelsl\\ctcert.bin";

        try {
            Seedplanter planter = new Seedplanter(TextFields_Strings[0], TextFields_Strings[1], TextFields_Strings[2], TextFields_Strings[3]);
            planter.DoInjection();
            Main.showAlertBox("Done!", null, "Everything went well!", false);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IOException e) {
            Main.showAlertBox("An exception occurred!", null, e.getMessage(), true);
        }
    }
}
