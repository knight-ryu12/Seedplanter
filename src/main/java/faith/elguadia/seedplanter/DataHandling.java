package faith.elguadia.seedplanter;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.zip.ZipFile;

public class DataHandling {
    private ZipHandling.ZipRegion ZIPR;
    private Path tmpDir;
    public HashMap<String, byte[]> MainData = new HashMap<>();

    public ZipHandling.ZipRegion getZipRegion()    { return ZIPR; }
    public Path      getTmpdirPath()               { return tmpDir; }

    DataHandling(Path DSiWare, Path movableSed, Path injectionZip, Path ctcert) throws IOException {
        try (ZipFile zf = new ZipFile(injectionZip.toFile())) {
            //===============================================================
            //========Read in DSiWare, movable.sed, and ZIP entries==========
            //===============================================================
            ZIPR = ZipHandling.CheckRegion(Files.readAllBytes(injectionZip));
            if (ZIPR == ZipHandling.ZipRegion.ZIP_ERROR)
                throw new IOException("ZIP Region Error");

            if (ZIPR == ZipHandling.ZipRegion.ZIP_EUR || ZIPR == ZipHandling.ZipRegion.ZIP_USA) {
                MainData.put("app", ZipHandling.ReadAllBytesFromZipEntry(zf, "sudoku_v0.app"));
                MainData.put("savedata.bin", ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata.bin"));
            }
            else if (ZIPR == ZipHandling.ZipRegion.ZIP_JPN) {
                MainData.put("app", ZipHandling.ReadAllBytesFromZipEntry(zf, "4swords.app"));
                MainData.put("jpn_public.sav", IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("jpn_public.sav")));
            }

            MainData.put("movable.sed", new byte[16]);
            System.arraycopy(Files.readAllBytes(movableSed), 0x110, MainData.get("movable.sed"), 0, 0x10);

            MainData.put("dsiware.bin", Files.readAllBytes(DSiWare));

            //===============================================================
            //==========Copy dsiwaretool and DLLs/ctcert to tmpDir===========
            //===============================================================
            tmpDir = Files.createTempDirectory("Seedplanter");

            Files.copy(getClass().getClassLoader().getResourceAsStream("ctr-dsiwaretool.exe"), Paths.get(tmpDir.toString(), "ctr-dsiwaretool.exe"));
            Files.copy(getClass().getClassLoader().getResourceAsStream("libcrypto-1_1-x64__.dll"), Paths.get(tmpDir.toString(), "libcrypto-1_1-x64__.dll"));
            Files.copy(getClass().getClassLoader().getResourceAsStream("zlib1__.dll"), Paths.get(tmpDir.toString(), "zlib1__.dll"));

            Files.copy(ctcert, Paths.get(tmpDir.toString(), "ctcert.bin"));
        }
    }

    public void exportToFile(byte[] filedata, Path filename) throws IOException {
        Files.write(filename, filedata, StandardOpenOption.CREATE);
    }

    public byte[] importToByteArray(Path filename) throws IOException {
        return Files.readAllBytes(filename);
    }
}
