package faith.elguadia.seedplanter;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.zip.ZipFile;

class DataHandling {
    private ZipHandling.ZipRegion ZIPR;
    public HashMap<String, byte[]> MainData = new HashMap<>();

    public ZipHandling.ZipRegion getZipRegion()    { return ZIPR; }

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
        }
        byte[] movablesed = Files.readAllBytes(movableSed);
        if (movablesed.length != 0x140) {
            throw new IOException("movable.sed is not 320 bytes in size!");
        }

        MainData.put("key_y", new byte[16]);
        System.arraycopy(movablesed, 0x110, MainData.get("key_y"), 0, 0x10);
        MainData.put("dsiware.bin", Files.readAllBytes(DSiWare));
        MainData.put("ctcert.bin", Files.readAllBytes(ctcert));
        if (MainData.get("ctcert.bin").length != (0x180 + 0x1E)) {
            throw new IOException("Invalid ctcert.bin! The filesize is not 414 bytes exactly!");
        }
    }
}
