import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

public class Controller {
    @FXML private TextField dsiWare_TextField, movableSed_TextField, injectionZip_TextField, ctcert_TextField;

    // 0 is dsiWare, 1 is movableSed, 2 is injectionZip, 3 is ctcert
    private static String[] TextFields_Strings = new String[4];

    @FXML private void dsiWare_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("bin", "*.bin"));
        fileChooser.setTitle("Select your DSiWare .bin file");
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            TextFields_Strings[0] = file.getAbsolutePath();
            dsiWare_TextField.setText(TextFields_Strings[0]);
        }
    }

    @FXML private void movableSed_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("sed", "*.sed"));
        fileChooser.setTitle("Select your movable.sed file");
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            TextFields_Strings[1] = file.getAbsolutePath();
            movableSed_TextField.setText(TextFields_Strings[1]);
        }
    }

    @FXML private void injectionZip_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("sed", "*.zip"));
        fileChooser.setTitle("Select your ZIP injection file");
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            TextFields_Strings[2] = file.getAbsolutePath();
            injectionZip_TextField.setText(TextFields_Strings[2]);
        }
    }

    @FXML private void ctcert_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("bin", "*.bin"));
        fileChooser.setTitle("Select your ctcert.bin file");
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) {
            TextFields_Strings[3] = file.getAbsolutePath();
            ctcert_TextField.setText(TextFields_Strings[3]);
        }
    }

    @FXML private void injectButton() {
        TextFields_Strings[0] = dsiWare_TextField.getText();
        TextFields_Strings[1] = movableSed_TextField.getText();
        TextFields_Strings[2] = injectionZip_TextField.getText();
        TextFields_Strings[3] = ctcert_TextField.getText();

        try {
            Seedplanter planter = new Seedplanter(Paths.get(TextFields_Strings[0]), Paths.get(TextFields_Strings[1]), Paths.get(TextFields_Strings[2]), Paths.get(TextFields_Strings[3]));
            planter.DoInjection();
            Main.showAlertBox("Done!", null, "Everything went well!", false);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IOException e) {
            Main.showAlertBox("An exception occurred!", null, e.getMessage(), true);
        }
    }
}
