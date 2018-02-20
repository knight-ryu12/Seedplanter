package Seedplanter.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class Seedplanter {
    private DataHandling Data;

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

        Data = new DataHandling(DSiWareStr, movableSedStr, injectionZipStr, ctcertStr);
    }

    public void DoInjection() throws IOException {
        /*
        DSiWare is in RAM, movable.sed is in RAM, the (extracted) stuff from the injection ZIP is in RAM. now what?

        1) Decrypt the DSiWare. Theoretically there should now be a multi-dimensional array returned with all the decrypted data
            Maybe instead of returning a multi-dimensional array, make it slot everything into the Data POD class?
            Maybe instead of working with raw arrays, use a Map object?
        2) Now that the decrypted data is in RAM, we can call arraycopy to inject the srl.nds, and we can use the FAT code to inject savedata
        */

        byte[] footer_array = {1}; //placeholder array
        String tmpDirPath = Data.getTmpdirPath().toString();
        Path footerPath = Paths.get(tmpDirPath, "/footer.bin");
        Data.exportToFile(footer_array, footerPath);

        Runtime.getRuntime().exec(
                tmpDirPath + "/ctr-dsiwaretool.exe " + tmpDirPath + "/footer.bin " + tmpDirPath + "/ctcert.bin --write"
        );

        footer_array = Data.importToByteArray(footerPath);

        /*
        6) Encrypt the DSiWare
        7) Export the encrypted DSiWare to desktop
             the cherry on top of the cake

         Done!
         */
    }
}
