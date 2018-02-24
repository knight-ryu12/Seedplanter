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

    DataHandling(String DSiWareStr, String movableSedStr, String injectionZipStr, String ctcertStr) throws IOException {
        try (ZipFile zf = new ZipFile(injectionZipStr)) {
            //===============================================================
            //========Read in DSiWare, movable.sed, and ZIP entries==========
            //===============================================================
            ZIPR = ZipHandling.CheckRegion(Files.readAllBytes(Paths.get(injectionZipStr)));
            if (ZIPR == ZipHandling.ZipRegion.ZIP_ERROR)
                throw new IOException("ZIP Region Error");

            if (ZIPR == ZipHandling.ZipRegion.ZIP_EUR || ZIPR == ZipHandling.ZipRegion.ZIP_USA) {
                MainData.put("app", ZipHandling.ReadAllBytesFromZipEntry(zf, "sudoku_v0.app"));
                MainData.put("savedata.bin", ZipHandling.ReadAllBytesFromZipEntry(zf, "savedata/savedata.bin"));
            }
            else if (ZIPR == ZipHandling.ZipRegion.ZIP_JPN) {
                throw new IOException("JPN region is not supported yet!");
            }

            MainData.put("movable.sed", new byte[16]);
            System.arraycopy(Files.readAllBytes(Paths.get(movableSedStr)), 0x110, MainData.get("movable.sed"), 0, 16);

            MainData.put("dsiware.bin", Files.readAllBytes(Paths.get(DSiWareStr)));

            //===============================================================
            //==========Copy dsiwaretool and DLLs/ctcert to tmpDir===========
            //===============================================================
            tmpDir = Files.createTempDirectory("Seedplanter");

            Files.copy(getClass().getResourceAsStream("ctr-dsiwaretool.exe"), Paths.get(tmpDir.toString(), "ctr-dsiwaretool.exe"));
            Files.copy(getClass().getResourceAsStream("libcrypto-1_1-x64__.dll"), Paths.get(tmpDir.toString(), "libcrypto-1_1-x64__.dll"));
            Files.copy(getClass().getResourceAsStream("zlib1__.dll"), Paths.get(tmpDir.toString(), "zlib1__.dll"));

            Files.copy(Paths.get(ctcertStr), Paths.get(tmpDir.toString(), "ctcert.bin"));
        } catch (IOException e) {
            throw e;
        }
    }

    public void exportToFile(byte[] filedata, Path filename) throws IOException {
        Files.write(filename, filedata, StandardOpenOption.CREATE);
    }

    public byte[] importToByteArray(Path filename) throws IOException {
        return Files.readAllBytes(filename);
    }
}
