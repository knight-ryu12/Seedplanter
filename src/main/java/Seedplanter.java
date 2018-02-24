import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

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

    public void DoInjection() throws IOException, InvalidKeyException, InvalidAlgorithmParameterException {
        //This constructor calculates normal key
        Crypto crypto = new Crypto(Data.MainData.get("movable.sed"));
        //Decrypt - everything will be put in the main hashmap with its own entries
        TADPole.decrypt(crypto, Data.MainData.get("dsiware.bin"), Data.MainData);

        //Inject the sudoku/4swords app into srl.nds
        System.arraycopy(Data.MainData.get("app"), 0, Data.MainData.get("srl.nds"), 0, Data.MainData.get("app").length);

        //Inject the save
        FAT fat = new FAT(Data.MainData.get("public.sav"));
        fat.clearRoot();
        if (Data.getZipRegion() == ZipHandling.ZipRegion.ZIP_EUR || Data.getZipRegion() == ZipHandling.ZipRegion.ZIP_USA)
            fat.copySingleFileToRoot("SAVEDATABIN", Data.MainData.get("savedata.bin"));
        else if (Data.getZipRegion() == ZipHandling.ZipRegion.ZIP_JPN)
            throw new IOException("I said JPN region doesn't work yet!!!");
        fat.copyFATable();

        //Fix footer/header
        TADPole.fixHash(Data.MainData);

        //Export footer.bin
        String tmpDirPath = Data.getTmpdirPath().toString();
        Path footerPath = Paths.get(tmpDirPath, "/footer.bin");
        Data.exportToFile(Data.MainData.get("footer.bin"), footerPath);

        //Sign footer.bin
        Runtime.getRuntime().exec(
                tmpDirPath + "/ctr-dsiwaretool.exe " + tmpDirPath + "/footer.bin " + tmpDirPath + "/ctcert.bin --write"
        );

        //Re-import it back
        System.arraycopy(Data.importToByteArray(footerPath), 0, Data.MainData.get("footer.bin"), 0, Data.MainData.get("footer.bin").length);

        byte[] patchedBin = TADPole.rebuildTad(crypto, Data.MainData);
        Files.write(Paths.get(tmpDirPath, "dsiware.bin.patched"), patchedBin);
    }
}
