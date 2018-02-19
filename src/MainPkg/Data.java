package MainPkg;
import MainPkg.ZipHandling.ZipRegion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipFile;

public class Data{
    private ZipRegion ZIPR;
    private byte[][] injectiondata = new byte[4][]; //app, savedata/payload, all, backup
    private byte[]   movableSed = new byte[16];
    private byte[]   DSiWareBin;

    public ZipRegion returnZipRegion()          { return ZIPR; }
    public byte[]    returnInjectionData(int x) { return injectiondata[x]; }
    public byte[]    returnMovableSed()         { return movableSed; }
    public byte[]    returnDSiWareBin()         { return DSiWareBin; }

    Data(String DSiWareStr, String movableSedStr, String injectionZipStr) throws IOException {
        ZipFile zf = null;
        try {
            zf = new ZipFile(injectionZipStr);
            ZIPR = ZipHandling.CheckRegion(Files.readAllBytes(Paths.get(injectionZipStr)));
            if (ZIPR == ZipRegion.ZIP_ERROR)
                throw new IOException("ZIP Region Error");

            if (ZIPR == ZipRegion.ZIP_EUR || ZIPR == ZipRegion.ZIP_USA)
                readFilesEURUSA(zf);
            else if (ZIPR == ZipRegion.ZIP_JPN)
                readFilesJPN(zf);

            readMovableSed(movableSedStr);
            readDSiWare(DSiWareStr);
        } catch (IOException e) {
            throw e;
        } finally {
            try { if (zf != null) { zf.close(); } } catch (IOException e) {}
        }
    }

    private void readFilesEURUSA(ZipFile zf) throws IOException {
        injectiondata[0] = ZipHandling.ReadAllBytesFromZipEntry(zf, "sudoku_v0.app");
        injectiondata[1] = ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata.bin");
    }

    private void readFilesJPN(ZipFile zf) throws IOException {
        injectiondata[0]  = ZipHandling.ReadAllBytesFromZipEntry(zf, "4swords.app");
        injectiondata[1]  = ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata/payload.dat");
        injectiondata[2]  = ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata/all.dat");
        injectiondata[3]  = ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata/backup.dat");
    }

    private void readMovableSed(String movableSedStr) throws IOException {
        System.arraycopy(Files.readAllBytes(Paths.get(movableSedStr)), 0x110, movableSed, 0, 16);
    }

    private void readDSiWare(String DSiWareStr) throws IOException {
        DSiWareBin = Files.readAllBytes(Paths.get(DSiWareStr));
    }
}
