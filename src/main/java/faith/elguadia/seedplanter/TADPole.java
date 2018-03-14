package faith.elguadia.seedplanter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.HashMap;

class TADPole {
    private static final String[] content_list = {"tmd", "srl.nds", "2.bin", "3.bin", "4.bin", "5.bin", "6.bin", "7.bin", "8.bin", "public.sav", "banner.sav"};

    //Returns true if they are equal, false if they are not equal
    private static boolean compareArrays(byte[] arr1, int offset1, byte[] arr2, int offset2, int elements) {
        for (int i = 0; i < elements; i++)
            if (arr1[offset1 + i] != arr2[offset2 + i])
                return false;
        return true;
    }

    private static byte[] getDump(Crypto crypto, byte[] DSiWare, int data_offset, int size) throws InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] key =  crypto.normalKey;
        byte[] iv = new byte[0x10];
        byte[] enc = new byte[size];

        System.arraycopy(DSiWare, data_offset, enc, 0, size);
        System.arraycopy(DSiWare, (data_offset + size + 0x10), iv, 0, 0x10);

        return crypto.decryptMessage(enc, key, iv);
    }

    private static int[] getContentSize(byte[] header){
        ByteBuffer buffer = ByteBuffer.allocate(0x2C).order(ByteOrder.LITTLE_ENDIAN).put(header, 0x48, 0x2C);
        buffer.rewind();

        int[] content_sizelist = new int[11];
        for (int i = 0; i < content_sizelist.length; i++) {
            int c = buffer.getInt();
            content_sizelist[i] = (c == 0xB34) ? 0xB40 : c;
        }
        return content_sizelist;
    }

    public static void decrypt(Crypto crypto, byte[] DSiWare, HashMap<String, byte[]> hashmap) throws InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] dsiware = hashmap.get("dsiware.bin");
        hashmap.put("banner.bin", getDump(crypto, dsiware, 0x0, 0x4000));
        hashmap.put("header.bin", getDump(crypto, dsiware, 0x4020, 0xF0));
        hashmap.put("footer.bin", getDump(crypto, dsiware, 0x4130, 0x4E0));

        MessageDigest md = null; try { md = MessageDigest.getInstance("SHA-256"); } catch (Exception e) {}
        byte[] footer = hashmap.get("footer.bin");
        byte[] banner_calc_hash = md.digest(hashmap.get("banner.bin"));
        byte[] header_calc_hash = md.digest(hashmap.get("header.bin"));

        //If decryption went bad, then the hashes won't match up
        if (!compareArrays(footer, 0, banner_calc_hash, 0, 0x20) || !compareArrays(footer, 0x20, header_calc_hash, 0, 0x20))
            throw new InvalidKeyException("Failed to decrypt! Are you sure you used the correct movable.sed?");

        int[] contents = getContentSize(hashmap.get("header.bin"));
        int off = 0x4630;
        for (int i = 0; i < 11; i++) {
            if (contents[i] != 0) {
                hashmap.put(content_list[i], getDump(crypto, DSiWare, off, contents[i]));
                off += (contents[i] + 0x20);
            }
        }
    }

    public static void fixHash(HashMap<String, byte[]> hashmap) {
        MessageDigest md = null; try { md = MessageDigest.getInstance("SHA-256"); } catch (Exception e) {}

        int[] sizes = new int[11]; //the 2 are missing because they are constant and we don't need to log them
        byte[][] hash = new byte[13][];
        String[] footer_namelist = new String[13];
        footer_namelist[0] = "banner.bin";
        footer_namelist[1] = "header.bin";
        System.arraycopy(content_list, 0, footer_namelist, 2, 11);

        //load up the header array with sizes
        for (int i = 0; i < sizes.length; i++) {
            byte[] curarr = hashmap.get(content_list[i]);
            sizes[i] = (curarr == null) ? 0 : ((curarr.length == 0xB40) ? 0xB34 : curarr.length);
        }

        //load up the footer array with hashes
        for (int i = 0; i < hash.length; i++) {
            byte[] curarr = hashmap.get(footer_namelist[i]);
            if (curarr != null)
                hash[i] = md.digest(curarr);
            else
                hash[i] = new byte[32]; //Arrays are initialized to zero, so by initializing an array here we are essentially filling it with zeroes
        }

        //write the sizes in the header
        for (int i = 0; i < sizes.length; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(0, sizes[i]);
            System.arraycopy(buffer.array(), 0, hashmap.get("header.bin"), (0x48 + i * 0x4), 0x4);
        }

        //write the hashes in the footer
        for (int i = 0; i < hash.length; i++)
            System.arraycopy(hash[i], 0, hashmap.get("footer.bin"), (i * 0x20), 0x20);
    }

    public static byte[] rebuildTad(Crypto crypto, HashMap<String, byte[]> hashmap) throws InvalidAlgorithmParameterException, InvalidKeyException {
        String[] full_namelist = new String[14];
        full_namelist[0] = "banner.bin";
        full_namelist[1] = "header.bin";
        full_namelist[2] = "footer.bin";
        System.arraycopy(content_list, 0, full_namelist, 3, 11);

        byte[] bm;
        byte[] section;
        byte[] iv = new byte[0x10];
        byte[] targetarr = new byte[hashmap.get("dsiware.bin").length];

        int offset = 0;
        for (String s : full_namelist) {
            byte[] content = hashmap.get(s);
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


