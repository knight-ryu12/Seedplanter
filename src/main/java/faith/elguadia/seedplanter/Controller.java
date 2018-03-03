package faith.elguadia.seedplanter;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Paths;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

public class Controller {
    @FXML private TextField dsiWare_TextField, movableSed_TextField, injectionZip_TextField, ctcert_TextField;

    // 0 is dsiWare, 1 is movableSed, 2 is injectionZip, 3 is ctcert
    private static String[] TextFields_Strings = new String[4];

    @FXML private void dsiWare_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select your DSiWare .bin file");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("bin", "*.bin"));
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) dsiWare_TextField.setText(file.getAbsolutePath());
    }

    @FXML private void movableSed_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select your movable.sed file");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("sed", "*.sed"));
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) movableSed_TextField.setText(file.getAbsolutePath());
    }

    @FXML private void injectionZip_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select your ZIP injection file");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("zip", "*.zip"));
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) injectionZip_TextField.setText(file.getAbsolutePath());
    }

    @FXML private void ctcert_BrowseButton() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select your ctcert.bin file");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("bin", "*.bin"));
        File file = fileChooser.showOpenDialog(new Stage());
        if (file != null) ctcert_TextField.setText(file.getAbsolutePath());
    }

    @FXML private void injectButton() {
        TextFields_Strings[0] = dsiWare_TextField.getText();
        TextFields_Strings[1] = movableSed_TextField.getText();
        TextFields_Strings[2] = injectionZip_TextField.getText();
        TextFields_Strings[3] = ctcert_TextField.getText();

        try {
            Seedplanter planter = new Seedplanter(Paths.get(TextFields_Strings[0]), Paths.get(TextFields_Strings[1]), Paths.get(TextFields_Strings[2]), Paths.get(TextFields_Strings[3]));
            planter.DoInjection();
            Main.showAlertBox("Done!", null, "Everything went well! Your DSiWare bin file has been replaced by the injected version", false);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException | IOException | InterruptedException e) {
            Main.showAlertBox("An exception occurred!", null, e.getMessage(), true);
        }
    }
}
