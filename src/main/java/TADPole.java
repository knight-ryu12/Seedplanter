import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.HashMap;

public class TADPole {
    private static final String[] content_list = {"tmd", "srl.nds", "2.bin", "3.bin", "4.bin", "5.bin", "6.bin", "7.bin", "8.bin", "public.sav", "banner.sav"};

    private static byte[] getDump(Crypto crypto, byte[] DSiWare, int data_offset, int size) throws InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] key =  crypto.normalKey;
        byte[] iv = new byte[16];
        byte[] enc = new byte[size];

        System.arraycopy(DSiWare, data_offset, enc, 0, size);
        System.arraycopy(DSiWare, (data_offset + size + 0x10), iv, 0, 0x10);

        return crypto.decryptMessage(enc, key, iv);
    }

    private static long[] getContentSize(byte[] h){
        ByteBuffer buffer = ByteBuffer.allocate(0x2C).order(ByteOrder.LITTLE_ENDIAN).put(h, 0x48, 0x2C);
        long[] content_sizelist = new long[11];
        buffer.rewind();
        for(int i =0; i<11; i++) {
            long c = Integer.toUnsignedLong(buffer.getInt());
            if(c == 0xB34)
                c = 0xB40; //Dirtyhaxx
            content_sizelist[i] = c;
        }
        return content_sizelist;
    }

    public static void decrypt(Crypto crypto, byte[] DSiWare, HashMap<String, byte[]> hashmap) throws InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] dsiware = hashmap.get("dsiware.bin");
        hashmap.put("banner.bin", getDump(crypto, dsiware, 0x0, 0x4000));
        hashmap.put("header.bin", getDump(crypto, dsiware, 0x4020, 0xF0));
        hashmap.put("footer.bin", getDump(crypto, dsiware, 0x4130, 0x4E0));
        long[] contents = getContentSize(hashmap.get("header.bin"));
        int off = 0x4630;
        for (int i = 0; i < 11; i++) {

            if (contents[i] != 0) {
                hashmap.put(content_list[i], getDump(crypto, DSiWare, off, (int)contents[i]));
                off += (contents[i] + 0x20);
            }
        }
    }

    public static void fixHash(HashMap<String, byte[]> hashmap) {
        MessageDigest md = null; try { md = MessageDigest.getInstance("SHA-256"); } catch (Exception e) {}

        int[] sizes = new int[11]; //the 2 are missing because they are constant and we don't need to log them
        String[] hash = new String[13];
        String[] footer_namelist = new String[13];
        footer_namelist[0] = "banner.bin";
        footer_namelist[1] = "header.bin";
        for (int i = 2; i < 13; i++) {
            footer_namelist[i] = content_list[i-2];
        }

        //load up the header array with sizes
        for (int i = 0; i < sizes.length; i++) {
            byte[] curarr = hashmap.get(content_list[i]);
            sizes[i] = (curarr == null) ? 0 : ((curarr.length == 0xB40) ? 0xB34 : curarr.length);
        }

        //load up the footer array with hashes
        for (int i = 0; i < hash.length; i++) {
            byte[] curarr = hashmap.get(footer_namelist[i]);
            if (curarr != null)
                hash[i] = Hex.encodeHexString(md.digest(curarr));
            else
                hash[i] = "0000000000000000000000000000000000000000000000000000000000000000";
        }

        //write the sizes in the header
        for (int i = 0; i < 11; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(0, sizes[i]);
            System.arraycopy(buffer.array(), 0, hashmap.get("header.bin"), (0x48 + i * 0x4), 0x4);
        }

        //write the hashes in the footer
        try {
            for (int i = 0; i < 13; i++) {
                System.arraycopy(Hex.decodeHex(hash[i]), 0, hashmap.get("footer.bin"), (i * 0x20), 0x20);
            }
        } catch (Exception e) {} //TODO: Move from strings to byte arrays for the hashes
    }

    public static byte[] rebuildTad(Crypto crypto, HashMap<String, byte[]> hashmap) throws InvalidAlgorithmParameterException, InvalidKeyException {
        String[] full_namelist = new String[14];
        full_namelist[0] = "banner.bin";
        full_namelist[1] = "header.bin";
        full_namelist[2] = "footer.bin";
        for (int i = 3; i < content_list.length; i++) {
            full_namelist[i] = content_list[i - 3];
        }
        byte[] bm;
        byte[] content;
        byte[] section;
        byte[] iv = new byte[0x10];
        byte[] targetarr = new byte[hashmap.get("dsiware.bin").length];

        int offset = 0;
        for (String s : full_namelist) {
            content = hashmap.get(s);
            if (content != null) {
                bm = crypto.generateBlockMetadata(content); // 0x0+10 = AES MAC over SHA256(SHA256 of PlainData), 0x10+10 = IV (RandGen)
                System.arraycopy(bm, 0x10, iv, 0, iv.length); // bm -> iv
                content = crypto.encryptMessage(content, crypto.normalKey, iv); // Encrypt.
                section = new byte[content.length + bm.length]; // section is content+bm

                System.arraycopy(content, 0, section, 0, content.length); // Merge1
                System.arraycopy(bm, 0, section, content.length, bm.length); // Merge2
                System.arraycopy(section, 0, targetarr, offset, section.length);
                offset += section.length;
            }
        }

        return targetarr;
    }
}


