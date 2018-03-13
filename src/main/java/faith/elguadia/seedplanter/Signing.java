package faith.elguadia.seedplanter;

import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.math.BigInteger;
import org.bouncycastle.asn1.*;
import java.security.*;
import java.security.spec.*;

class Signing {
    private final int totalhashsize = 13 * 0x20;
    private byte[] ctcert_bin;
    private byte[] footer;
    private byte[] hashes = new byte[totalhashsize];
    private KeyPair pair;

    Signing(byte[] Ctcert_bin, byte[] Footer) throws NoSuchAlgorithmException {
        ctcert_bin = Ctcert_bin;
        footer = Footer;
        pair = RetrieveKeyPair();
    }

    private KeyPair RetrieveKeyPair() throws NoSuchAlgorithmException {
        byte[] tmpkey = new byte[0x1E];
        System.arraycopy(ctcert_bin, 0x108, tmpkey , 0, 0x1E);
        BigInteger publickey_R = new BigInteger(tmpkey);
        System.arraycopy(ctcert_bin, 0x108 + 0x1E, tmpkey , 0, 0x1E);
        BigInteger publickey_S = new BigInteger(tmpkey);
        System.arraycopy(ctcert_bin, 0x180, tmpkey , 0, 0x1E);
        BigInteger private_key = new BigInteger(tmpkey);

        System.arraycopy(footer, 0, hashes, 0, totalhashsize);

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
        System.arraycopy(ctcert_bin, 0x108, footer, totalhashsize + 0x3C + 4, 0x3C);

        //Sign the hashes at the top, and place the signature in the right spot of the footer
        ecdsaSign.update(hashes);
        byte[] hashes_signature = ecdsaSign.sign();
        byte[] hashes_signature_R_S = parseSignature(hashes_signature);
        System.arraycopy(hashes_signature_R_S, 0, footer, totalhashsize, 0x3C);

        //create_ecdsa_signature(&sign_ctcert[0x180] <--- private key
        //                      , tmphash, <----- hash of everything besides the signature sub-struct
        //                      0x20, <---- size of hash (always 32 bytes)
        //                      &apcert->sig.val <---- this goes into apcert signature sub-struct (ecpoint to be exact)


        byte[] keyid = getKeyID();
        System.arraycopy(keyid, 0, footer, totalhashsize + 0x3C + 0x80, 0x40);

        byte[] APCert_bytes_to_sign = new byte[0x100];
        System.arraycopy(footer, totalhashsize + 0x3C + 0x80, APCert_bytes_to_sign, 0, 0x100);

        ecdsaSign.update(APCert_bytes_to_sign);
        byte[] apcert_signature = ecdsaSign.sign();
        byte[] apcert_signature_R_S = parseSignature(apcert_signature);
        System.arraycopy(apcert_signature_R_S, 0, footer, totalhashsize + 0x3C + 4, 0x3C);
    }

    private byte[] getKeyID() {
        int i;

        byte[] issuer = new byte[0x40];
        System.arraycopy(footer, totalhashsize + 0x1BC + 0x80, issuer, 0, 33);
        String issuer_str = new String(issuer, 0, 33);

        byte[] keyid = new byte[0x40];
        System.arraycopy(footer, totalhashsize + 0x1BC + 0x80 + 0x40 + 4, keyid, 0, 0x40);
        for (i = 0; i < keyid.length && keyid[i] != 0; i++) { }
        String keyid_str = new String(keyid, 0, i);

        String str = issuer_str + "-" + keyid_str;
        byte[] str_bytes = str.getBytes();
        byte[] returnvalue = new byte[0x40];
        System.arraycopy(str_bytes, 0, returnvalue, 0, str_bytes.length);

        return returnvalue;
    }

    //Since the JCE's ECDSA methods return ASN1 encoded signatures (in the form of byte arrays),
    //and footer.bin only takes in ECPoint arrays, you need to parse the byte array into two distinct points
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
        System.arraycopy(R, 0, return_value, 0x1E - R.length, R.length);
        System.arraycopy(S, 0, return_value, 0x1E + (0x1E - S.length), S.length);

        return return_value;
    }
}