package MainPkg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Seedinjector {
    Data data = null;

    //The constructor will read in all of the files into byte arrays
    Seedinjector(String DSiWareStr, String movableSedStr, String injectionZipStr, String ctcertStr) {
        try {
            if (DSiWareStr == null || Files.notExists(Paths.get(DSiWareStr)))
                throw new IOException("DSiWare not found!");
            if (movableSedStr == null || Files.notExists(Paths.get(movableSedStr)))
                throw new IOException("movableSedStr not found!");
            if (injectionZipStr == null || Files.notExists(Paths.get(injectionZipStr)))
                throw new IOException("injectionZipStr not found!");
            if (ctcertStr == null || Files.notExists(Paths.get(ctcertStr)))
                throw new IOException("ctcertStr not found!");

            data = new Data(DSiWareStr, movableSedStr, injectionZipStr);
            System.out.println("========== All went OK ==========");
        } catch (IOException e) {
            Main.showAlertBox("An exception occurred!", null, e.getMessage());
        }
    }
}
