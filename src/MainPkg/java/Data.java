package MainPkg.java;
import MainPkg.java.ZipHandling.ZipRegion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipFile;

public class Data {
    private ZipRegion ZIPR;
    private byte[][] injectiondata = new byte[4][]; //app, savedata/payload, all, backup
    private byte[]   movableSed = new byte[16];
    private byte[]   DSiWareBin;

    public ZipRegion returnZipRegion()          { return ZIPR; }
    public byte[]    returnInjectionData(int x) { return injectiondata[x]; }
    public byte[]    returnMovableSed()         { return movableSed; }
    public byte[]    returnDSiWareBin()         { return DSiWareBin; }

    private Path tmpDir = null;

    Data(String DSiWareStr, String movableSedStr, String injectionZipStr) throws IOException {
        try (ZipFile zf = new ZipFile(injectionZipStr)) {
            //===============================================================
            //========Read in DSiWare, movable.sed, and ZIP entries==========
            //===============================================================
            ZIPR = ZipHandling.CheckRegion(Files.readAllBytes(Paths.get(injectionZipStr)));
            if (ZIPR == ZipRegion.ZIP_ERROR)
                throw new IOException("ZIP Region Error");

            if (ZIPR == ZipRegion.ZIP_EUR || ZIPR == ZipRegion.ZIP_USA)
                readFilesEURUSA(zf);
            else if (ZIPR == ZipRegion.ZIP_JPN)
                throw new IOException("JPN region is not supported yet!");

            readMovableSed(movableSedStr);
            readDSiWare(DSiWareStr);

            //===============================================================
            //========Copy dsiwaretool and libcrypto/zlib to tmpDir==========
            //===============================================================
            tmpDir = Files.createTempDirectory("Seedplanter");

            Files.copy(getClass().getResourceAsStream("/MainPkg/resources/ctr-dsiwaretool.exe"), Paths.get(tmpDir.toString(), "ctr-dsiwaretool.exe"));
            Files.copy(getClass().getResourceAsStream("/MainPkg/resources/libcrypto-1_1-x64__.dll"), Paths.get(tmpDir.toString(), "libcrypto-1_1-x64__.dll"));
            Files.copy(getClass().getResourceAsStream("/MainPkg/resources/zlib1__.dll"), Paths.get(tmpDir.toString(), "zlib1__.dll"));
        } catch (IOException e) {
            throw e;
        }
    }

    private void readFilesEURUSA(ZipFile zf) throws IOException {
        injectiondata[0] = ZipHandling.ReadAllBytesFromZipEntry(zf, "sudoku_v0.app");
        injectiondata[1] = ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata.bin");
    }

    private void readMovableSed(String movableSedStr) throws IOException {
        System.arraycopy(Files.readAllBytes(Paths.get(movableSedStr)), 0x110, movableSed, 0, 16);
    }

    private void readDSiWare(String DSiWareStr) throws IOException {
        DSiWareBin = Files.readAllBytes(Paths.get(DSiWareStr));
    }

    private void exportToTmpDir(byte[] filedata, String filename) throws IOException {
        ByteArrayInputStream BAIS = new ByteArrayInputStream(filedata);
        Files.copy(BAIS, Paths.get(tmpDir.toString(), filename));
    }
}
