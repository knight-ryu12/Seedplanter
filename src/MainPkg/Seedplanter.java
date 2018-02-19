package MainPkg;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Seedplanter {
    private Data data;

    //The constructor will read in all of the files into byte arrays
    Seedplanter(String DSiWareStr, String movableSedStr, String injectionZipStr, String ctcertStr) throws IOException {
        if (DSiWareStr == null || Files.notExists(Paths.get(DSiWareStr)))
            throw new IOException("DSiWare not found!");
        if (movableSedStr == null || Files.notExists(Paths.get(movableSedStr)))
            throw new IOException("movable.sed not found!");
        if (injectionZipStr == null || Files.notExists(Paths.get(injectionZipStr)))
            throw new IOException("Injection ZIP not found!");
        if (ctcertStr == null || Files.notExists(Paths.get(ctcertStr)))
            throw new IOException("ctcert.bin not found!");

        data = new Data(DSiWareStr, movableSedStr, injectionZipStr);
    }

    public void DoInjection() throws IOException {
        /*
            DSiWare is in RAM, movable.sed is in RAM, the (extracted) stuff from the injection ZIP is in RAM
            1) Decrypt the DSiWare. Theoretically there should now be a multi-dimensional array returned with all the decrypted data
                Maybe instead of returning a multi-dimensional array, make it slot everything into the Data POD class?
            2) Now that the decrypted data is in RAM, we can call arraycopy to inject the srl.nds, and we can use the FAT code to inject savedata
            3) Now we export footer.bin and ctr-dsiwaretool.exe to the OS's temp directory
            4) Call ctr-dsiwaretool.exe to sign the footer
            5) Re-import footer.bin into RAM
            6) Encrypt the DSiWare
            7) Export the encrypted DSiWare to desktop
            Done!
         */
    }
}
