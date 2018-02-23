import org.apache.commons.codec.binary.Hex;

import java.math.BigInteger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

public class Crypto {
    private final BigInteger keyx = new BigInteger("6FBB01F872CAF9C01834EEC04065EE53", 16);
    private final BigInteger F128 = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16);
    private final BigInteger C    = new BigInteger("1FF9E9AAC5FE0408024591DC5D52768A", 16);
    public final BigInteger cmac_keyx = new BigInteger("B529221CDDB5DB5A1BF26EFF2041E875", 16);
    private BigInteger keyy;
    public final byte[] normalKey;

    Crypto(byte[] movableSed) {
        keyy = new BigInteger(Hex.encodeHexString(movableSed), 16);
        normalKey = getNormalKey(keyx, keyy);
    }

    private byte[] getNormalKey(BigInteger x, BigInteger y) {
        BigInteger n = rol_128(x,2);
        n = n.xor(y);
        n = add_128(n, C);
        n = rol_128(n, 87);
        byte[] array = n.toByteArray();
        if (array[0] == 0) {
            byte[] tmp = new byte[array.length - 1];
            System.arraycopy(array, 1, tmp, 0, tmp.length);
            array = tmp;
        }
        return array;
    }

    private BigInteger add_128(BigInteger a, BigInteger b){
        return a.add(b).and(F128);
    }

    private BigInteger rol_128(BigInteger n, int shift) {
        BigInteger left = n.shiftLeft(shift % 128);
        BigInteger right = n.shiftRight(128 - (shift % 128));
        return left.or(right).and(F128);
    }

    public byte[] decryptMessage(byte[] s, byte[] key, byte[] iv) throws InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = null; try { cipher = Cipher.getInstance("AES/CBC/NoPadding"); } catch (Exception e) { }
        final SecretKey sk = new SecretKeySpec(key,"AES");
        cipher.init(Cipher.DECRYPT_MODE, sk, new IvParameterSpec(iv));
        return cipher.update(s);
    }
}
