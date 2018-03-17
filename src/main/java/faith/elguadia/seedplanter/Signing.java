package faith.elguadia.seedplanter;

import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.math.BigInteger;
import org.bouncycastle.asn1.*;
import java.security.*;
import java.security.spec.*;

/*

footer.bin signing in a nutshell:

1) Read in the Public/Private key pair from the ctcert.bin into the KeyPair object

2) Copy the ctcert.bin to the CTCert section of the footer.bin

3) Take the 13 hashes at the top, and hash them all to get a single master hash of all the contents of the DSiWare container
4) Sign that hash. Retrieve the ECDSA (X, Y) coordinates in the form of byte arrays, each one of size 0x1E.
   If the points retrieved are not 0x1E in size, add padding 0s at the start. Then, take those two arrays,
   combine them and you'll get a single big byte array of size 0x3C. Place that in the correct spot for the footer. (it's placed
   immediately after the 13 hashes, i.e. 13 * 0x20 == 0x1A0)

5) Make a new byte array of size 0x40. Then, fill it up with this formula:
    snprintf(your_byte_array, 0x40, "%s-%s", ctcert->issuer, ctcert->key_id);

6) Copy that byte array into the issuer section for the APCert (it's at offset 0x80 relative to the start of the APCert)

7) Hash the APCert's bytes in the range of 0x80 to 0x180 (in total 0x100 bytes).
   Essentially skip the signature portion of the APCert (cause you don't sign a signature)
8) Sign that hash you just created with your KeyPair. Do the same coordinate retrieval process as for step 4.
9) Take your coordinates byte array (2 * 0x1E = 0x3C in size), and place it in the signature
   section of the APCert (it's at offset 0x04 relative to the start of the APCert)

10) Copy the public key byte array into the APCert's public key field (it's at offset 0x108 relative to the start of the APCert)

 */

class Signing {
    private final int totalhashsize = 13 * 0x20;
    private byte[] ctcert_bin;
    private byte[] footer;
    private byte[] hashes = new byte[totalhashsize];
    private KeyPair pair;

    Signing(byte[] Ctcert_bin, byte[] Footer) throws NoSuchAlgorithmException {
        ctcert_bin = Ctcert_bin;
        footer = Footer;
        System.arraycopy(footer, 0, hashes, 0, totalhashsize);
        pair = RetrieveKeyPair();
    }

    //A lot of this is just boilerplate code
    //If I could simply do "getKey(arr1, arr2)" then I would use that, but you need
    //to specify what algorithm you are using and in the case of ECDSA, what curve you are using
    private KeyPair RetrieveKeyPair() throws NoSuchAlgorithmException {
        byte[] tmpkey = new byte[0x1E];

        System.arraycopy(ctcert_bin, 0x108, tmpkey , 0, 0x1E);
        BigInteger publickey_R = new BigInteger(tmpkey);
        System.arraycopy(ctcert_bin, 0x108 + 0x1E, tmpkey , 0, 0x1E);
        BigInteger publickey_S = new BigInteger(tmpkey);
        System.arraycopy(ctcert_bin, 0x180, tmpkey , 0, 0x1E);
        BigInteger private_key = new BigInteger(tmpkey);

        try {
            AlgorithmParameters AlgParameters = AlgorithmParameters.getInstance("EC");
            ECGenParameterSpec ecgps = new ECGenParameterSpec("sect233r1");
            AlgParameters.init(ecgps);
            ECParameterSpec ecParameters = AlgParameters.getParameterSpec(ECParameterSpec.class);
            KeyFactory factory = KeyFactory.getInstance("EC");

            return new KeyPair(
                    factory.generatePublic (new ECPublicKeySpec(new ECPoint(publickey_R, publickey_S), ecParameters)),
                    factory.generatePrivate(new ECPrivateKeySpec(private_key, ecParameters))
            );
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Could not retrieve the algorithms needed for ECDSA!");
        }
    }

    public void DoSign() throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, IOException {
        //Preparing to sign things
        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
        ecdsaSign.initSign(pair.getPrivate());

        //!!! Copying the CTCert into the footer !!!
        System.arraycopy(ctcert_bin, 0,     footer, totalhashsize + 0x1BC, 0x180);

        //Copy the public key from the CTCert to the correct spot for the APCert
        System.arraycopy(ctcert_bin, 0x108, footer, totalhashsize + 0x3C + 0x108, 0x3C);

        //Sign the hashes at the top, and place the signature in the right spot of the footer
        ecdsaSign.update(hashes);
        byte[] hashes_signature = ecdsaSign.sign();
        byte[] hashes_signature_R_S = parseSignature(hashes_signature);
        System.arraycopy(hashes_signature_R_S, 0, footer, totalhashsize, 0x3C);

        //Get that string with the issuer and the keyid and then copy it to the correct spot
        byte[] keyid = getKeyID();
        System.arraycopy(keyid, 0, footer, totalhashsize + 0x3C + 0x80, 0x40);

        //Sign the APCert in the offset of 0x80-0x180 (0x100 bytes in total)
        //Here we are retrieving the bytes
        byte[] APCert_bytes_to_sign = new byte[0x100];
        System.arraycopy(footer, totalhashsize + 0x3C + 0x80, APCert_bytes_to_sign, 0, 0x100);

        //Here we sign the bytes, and then place them in the correct spot where the signature goes for the APCert
        ecdsaSign.update(APCert_bytes_to_sign);
        byte[] apcert_signature = ecdsaSign.sign();
        byte[] apcert_signature_R_S = parseSignature(apcert_signature);
        System.arraycopy(apcert_signature_R_S, 0, footer, totalhashsize + 0x3C + 4, 0x3C);
    }

    //All this entire method does is snprintf(apcert->issuer, 0x40, "%s-%s", ctcert->issuer, ctcert->key_id)
    //because Java's strings aren't null terminated, and you have to do your own implementation
    private byte[] getKeyID() {
        byte[] issuer = new byte[0x40];
        System.arraycopy(footer, totalhashsize + 0x1BC + 0x80, issuer, 0, 33);
        String issuer_str = new String(issuer, 0, 33);

        byte[] keyid = new byte[0x40];
        System.arraycopy(footer, totalhashsize + 0x1BC + 0x80 + 0x40 + 4, keyid, 0, 0x40);
        int i; for (i = 0; i < keyid.length && keyid[i] != 0; i++) { ; }
        String keyid_str = new String(keyid, 0, i);

        String str = issuer_str + "-" + keyid_str;
        byte[] str_bytes = str.getBytes();
        byte[] returnvalue = new byte[0x40];
        System.arraycopy(str_bytes, 0, returnvalue, 0, str_bytes.length);

        return returnvalue;
    }

    // stackoverflow.com/questions/39385718
    private byte[] parseSignature(byte[] signature) throws IOException {
        ArrayList<BigInteger> signature_R_S = new ArrayList<>();
        try (ByteArrayInputStream inStream = new ByteArrayInputStream(signature);
             ASN1InputStream asnInputStream = new ASN1InputStream(inStream))
        {
            ASN1Primitive asn1 = asnInputStream.readObject();
            if (asn1 instanceof ASN1Sequence) {
                ASN1Sequence asn1Sequence = (ASN1Sequence) asn1;
                ASN1Encodable[] asn1Encodables = asn1Sequence.toArray();
                for (ASN1Encodable asn1Encodable : asn1Encodables) {
                    ASN1Primitive asn1Primitive = asn1Encodable.toASN1Primitive();
                    if (asn1Primitive instanceof ASN1Integer) {
                        ASN1Integer asn1Integer = (ASN1Integer) asn1Primitive;
                        signature_R_S.add(asn1Integer.getValue());
                    }
                }
            }
        }

        byte[] return_value = new byte[2 * 0x1E];
        byte[] R = signature_R_S.get(0).toByteArray();
        byte[] S = signature_R_S.get(1).toByteArray();

        //These weird arraycopies are so that there are padding 0s at the start
        //Remember, the R/S coordinates are in **big** endian in the footer and CTCert/APCert!
        System.arraycopy(R, 0, return_value, 0x1E - R.length, R.length);
        System.arraycopy(S, 0, return_value, 0x1E + (0x1E - S.length), S.length);

        return return_value;
    }
}