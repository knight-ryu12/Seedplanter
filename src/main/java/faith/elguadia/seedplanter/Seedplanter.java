package faith.elguadia.seedplanter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

public class Seedplanter {
    private DataHandling Data;
    private Path targetfile;
    private Path ctcertPath;

    //The constructor will read in all of the files into byte arrays
    Seedplanter(Path DSiWare, Path movableSed, Path injectionZip, Path ctcert) throws IOException {
        if (DSiWare == null || Files.notExists(DSiWare))
            throw new IOException("DSiWare not found!");
        if (movableSed == null || Files.notExists(movableSed))
            throw new IOException("movable.sed not found!");
        if (injectionZip == null || Files.notExists(injectionZip))
            throw new IOException("Injection ZIP not found!");
        if (ctcert == null || Files.notExists(ctcert))
            throw new IOException("ctcert.bin not found!");

        targetfile = DSiWare;
        ctcertPath = ctcert;

        Data = new DataHandling(DSiWare, movableSed, injectionZip);
    }

    public void DoInjection() throws IOException, InvalidKeyException, InvalidAlgorithmParameterException, InterruptedException {
        //This constructor calculates normal key
        Crypto crypto = new Crypto(Data.MainData.get("movable.sed"));
        //Decrypt - everything will be put in the main hashmap with its own entries
        TADPole.decrypt(crypto, Data.MainData.get("dsiware.bin"), Data.MainData);

        //Inject the sudoku/4swords app into srl.nds
        System.arraycopy(Data.MainData.get("app"), 0, Data.MainData.get("srl.nds"), 0, Data.MainData.get("app").length);

        //Inject the save
        if (Data.getZipRegion() == ZipHandling.ZipRegion.ZIP_EUR || Data.getZipRegion() == ZipHandling.ZipRegion.ZIP_USA) {
            FAT fat = new FAT(Data.MainData.get("public.sav"));
            fat.clearRoot();
            fat.copySingleFileToRoot("SAVEDATABIN", Data.MainData.get("savedata.bin"));
            fat.copyFATable();
        } else if (Data.getZipRegion() == ZipHandling.ZipRegion.ZIP_JPN) {
            System.arraycopy(Data.MainData.get("jpn_public.sav"), 0, Data.MainData.get("public.sav"), 0, Data.MainData.get("jpn_public.sav").length);
        }

        //Fix footer/header
        TADPole.fixHash(Data.MainData);

        //Export footer.bin
        Signing signer = new Signing();
        signer.ExportFiles(Data.MainData.get("footer.bin"), ctcertPath);
        signer.DoSigning();
        byte[] signed_footer = signer.ImportFooter();
        System.arraycopy(signed_footer, 0, Data.MainData.get("footer.bin"), 0, Data.MainData.get("footer.bin").length);

        byte[] patchedBin = TADPole.rebuildTad(crypto, Data.MainData);
        Files.write(targetfile, patchedBin);
    }
}