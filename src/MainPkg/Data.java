package MainPkg;
import MainPkg.ZipHandling.ZipRegion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipFile;

public class Data{
    ZipRegion ZIPR;
    byte[][] injectiondata = new byte[4][];
    byte[]   movableSed = new byte[16];
    byte[]   DSiWareBin;

    Data(String DSiWareStr, String movableSedStr, String injectionZipStr) throws IOException {
        ZipFile zf = null;
        try {
            zf = new ZipFile(injectionZipStr);
            ZIPR = ZipHandling.CheckRegion(Files.readAllBytes(Paths.get(injectionZipStr)));
            if (ZIPR == ZipRegion.ZIP_ERROR)
                throw new IOException("ZIP Region Error");

            if (ZIPR == ZipRegion.ZIP_EUR || ZIPR == ZipRegion.ZIP_USA)
                readFilesEURUSA(zf, injectiondata[0], injectiondata[1]);
            else if (ZIPR == ZipRegion.ZIP_JPN)
                readFilesJPN(zf, injectiondata[0], injectiondata[1], injectiondata[2], injectiondata[3]);

            readMovableSed(movableSedStr, movableSed);
            readDSiWare(DSiWareStr, DSiWareBin);
        } catch (IOException e) {
            throw e;
        } finally {
            try { if (zf != null) { zf.close(); } } catch (IOException e) {}
        }
    }

    private void readFilesEURUSA(ZipFile zf,  byte[] sudoku_v0, byte[] savedata) throws IOException {
        sudoku_v0   = ZipHandling.ReadAllBytesFromZipEntry(zf, "sudoku_v0.app");
        savedata    = ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata.bin");
    }

    private void readFilesJPN(ZipFile zf, byte[] fourswords, byte[] payload, byte[] all, byte[] backup) throws IOException {
        fourswords  = ZipHandling.ReadAllBytesFromZipEntry(zf, "4swords.app");
        payload     = ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata/payload.dat");
        all         = ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata/all.dat");
        backup      = ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata/backup.dat");
    }

    private void readMovableSed(String movableSedStr, byte[] movableSed) throws IOException {
        System.arraycopy(Files.readAllBytes(Paths.get(movableSedStr)), 0x110, movableSed, 0, 16);
    }

    private void readDSiWare(String DSiWareStr, byte[] DSiWare) throws IOException {
        DSiWare = Files.readAllBytes(Paths.get(DSiWareStr));
    }
}
