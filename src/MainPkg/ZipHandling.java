package MainPkg;

import java.util.Arrays;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.zip.ZipFile;

public class ZipHandling {
    public enum ZipRegion {ZIP_EUR, ZIP_USA, ZIP_JPN, ZIP_ERROR}

    //The (byte)s everywhere don't look good I know, but that's what you get for having java use only signed integers...
    private static final byte[] Sudoku_EUR_SHA256 = {0x01, 0x39, 0x22, (byte)0xF6, (byte)0xD3, (byte)0x98, 0x25, (byte)0xB0, (byte)0x96, (byte)0xFE, (byte)0xEB, (byte)0xCF, 0x54, (byte)0xA8, (byte)0xAF, (byte)0xE1, (byte)0xA0, (byte)0xDE, (byte)0xB5, 0x47, 0x57, (byte)0xDC, 0x7A, (byte)0xC5, (byte)0xF9, 0x4E, 0x7B, 0x4B, (byte)0xF9, 0x1C, 0x5A, 0x7A};
    private static final byte[] Sudoku_USA_SHA256 = {(byte)0xBA, (byte)0xDD, 0x51, (byte)0xAF, 0x66, (byte)0x80, (byte)0xAE, 0x37, (byte)0xBF, 0x64, 0x25, 0x1A, 0x11, 0x2D, (byte)0xB7, 0x29, 0x7B, 0x3A, (byte)0x8F, 0x3F, (byte)0x8F, 0x29, (byte)0xD7, (byte)0xC4, (byte)0xC6, (byte)0xEE, (byte)0xD1, 0x2E, (byte)0xA9, (byte)0x81, 0x55, 0x2F};
    private static final byte[] Fourswords_JPN_SHA256 = {(byte)0xC8, (byte)0x8A, 0x23, (byte)0xEE, 0x32, 0x79, (byte)0xB3, 0x70, 0x6E, 0x06, 0x4A, (byte)0xFC, (byte)0xBA, 0x02, (byte)0xB1, (byte)0xE1, 0x33, 0x5E, 0x6E, 0x7B, (byte)0x92, 0x2C, 0x74, (byte)0xD2, (byte)0xBA, (byte)0xBB, 0x51, (byte)0xFE, (byte)0xA1, (byte)0xFF, 0x1F, 0x06};

    public static ZipRegion CheckRegion(byte[] ZIPdata) throws IOException {
        MessageDigest ZIP_MSGD = null; try { ZIP_MSGD = MessageDigest.getInstance("SHA-256"); } catch (Exception e) {} //Stupid NoSuchAlgorithmException wants me to catch it
        ZIP_MSGD.update(ZIPdata);
        byte[] ZIP_SHA256 = ZIP_MSGD.digest();

        if (Arrays.equals(ZIP_SHA256, Sudoku_EUR_SHA256) ) {
            return ZipRegion.ZIP_EUR;
        } else if (Arrays.equals(ZIP_SHA256, Sudoku_USA_SHA256)) {
            return ZipRegion.ZIP_USA;
        } else if (Arrays.equals(ZIP_SHA256, Fourswords_JPN_SHA256)) {
            return ZipRegion.ZIP_JPN;
        } else {
            return ZipRegion.ZIP_ERROR;
        }
    }

    public static byte[] ReadAllBytesFromZipEntry(ZipFile ZF, String entry) throws IOException {
        return ZF.getInputStream(ZF.getEntry(entry)).readAllBytes();
    }
}
