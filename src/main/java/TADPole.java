import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.util.HashMap;

public class TADPole {
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

        final String[] content_list = {"tmd", "srl.nds", "2.bin", "3.bin", "4.bin", "5.bin", "6.bin", "7.bin", "8.bin", "public.sav", "banner.sav"};
        for (int i = 0; i < 11; i++) {
            int off = 0x4630;
            if (contents[i] != 0) {
                hashmap.put(content_list[i], getDump(crypto, DSiWare, off, (int)contents[i]));
                off += (contents[i] + 0x20);
            }
        }
    }

    /*public static void fixHash(File header, File footer) throws IOException, DecoderException {
        MessageDigest md = null; try { md = MessageDigest.getInstance("SHA-256"); } catch (Exception e) {}
        File tmp;
        int i = 0;
        int[] sizes = new int[11];
        String[] hash = new String[13];
        List<String> footer_namelist = new ArrayList<>();
        footer_namelist.add("banner.bin");
        footer_namelist.add("header.bin");
        Collections.addAll(footer_namelist, content_list);
        for (String s : content_list) {
            tmp = new File(default_dir + s);
            sizes[i] = tmp.exists() ? (int) tmp.length() : 0;
            if (tmp.length() == 0xB40) sizes[i] = 0xB34;
            i++;
        }

        i = 0; // Reset Counter.
        for (String s : footer_namelist) {
            tmp = new File(default_dir + s);
            hash[i] = tmp.exists() ? Hex.encodeHexString(md.digest(Files.readAllBytes(tmp.toPath()))) : "0000000000000000000000000000000000000000000000000000000000000000";
            i++;
        }
        try(RandomAccessFile raf = new RandomAccessFile(header,"rwd")) {
            ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            int off = 0x48;
            for(int sz : sizes) {
                buf.putInt(sz);
                buf.rewind();
                raf.seek(off);
                raf.write(buf.array());
                off+=4;
            }
        }
        try(RandomAccessFile raf = new RandomAccessFile(footer,"rwd")) {
            int off = 0;
            for(String s : hash) {
                raf.seek(off);
                raf.write(Hex.decodeHex(s));
                off+=0x20;
            }
        }
    }*/
}
