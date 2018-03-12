package faith.elguadia.seedplanter;

import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.math.BigInteger;
import org.bouncycastle.asn1.*;
import java.security.*;
import java.security.spec.*;


public class Signing {
    private byte[] ctcert_bin;
    private byte[] footer;
    private byte[] hashes = new byte[14*0x20];

    Signing(byte[] Ctcert_bin, byte[] Footer) {
        ctcert_bin = Ctcert_bin;
        footer = Footer;
    }

    private ECParameterSpec getSpec() throws NoSuchAlgorithmException {
        try {
            AlgorithmParameters AlgParameters = AlgorithmParameters.getInstance("EC");
            ECGenParameterSpec ecgps = new ECGenParameterSpec("sect233r1");
            AlgParameters.init(ecgps);
            return AlgParameters.getParameterSpec(ECParameterSpec.class);
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("Failed to get ECParameterSpec!");
        }
    }

    private ECPoint parseSignature(byte[] signature) throws IOException {
        ArrayList<BigInteger> signature_R_S = new ArrayList<>();
        try (ByteArrayInputStream inStream = new ByteArrayInputStream(signature);
             ASN1InputStream asnInputStream = new ASN1InputStream(inStream);)
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
        return new ECPoint(signature_R_S.get(0), signature_R_S.get(1));
    }

    public void DoSigning() throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] key = new byte[0x1E];
        System.arraycopy(ctcert_bin, 0x108, key, 0, 0x1E);
        BigInteger publickey_R = new BigInteger(key);
        System.arraycopy(ctcert_bin, 0x108 + 0x1E, key, 0, 0x1E);
        BigInteger publickey_S = new BigInteger(key);
        System.arraycopy(ctcert_bin, 0x180, key, 0, 0x1E);
        BigInteger private_key = new BigInteger(key);

        System.arraycopy(footer, 0, hashes, 0, 14*0x20);

        ECParameterSpec ecParameters = getSpec();
        KeyFactory factory = KeyFactory.getInstance("EC");

        KeyPair pair = new KeyPair(
                factory.generatePublic (new ECPublicKeySpec(new ECPoint(publickey_R, publickey_S), ecParameters)),
                factory.generatePrivate(new ECPrivateKeySpec(private_key, ecParameters))
        );

        try {
            Sign(pair);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void Sign(KeyPair pair) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
        ecdsaSign.initSign(pair.getPrivate());
        ecdsaSign.update(hashes);
        byte[] signature = ecdsaSign.sign();
        ECPoint signature_point = parseSignature(signature);
        byte[] R = signature_point.getAffineX().toByteArray();
        byte[] S = signature_point.getAffineY().toByteArray();

        System.out.println("R is " + R.length + ", and S is " + S.length);

        if (R.length != 0x1E) {
            byte[] tmp = new byte[0x1E];
            System.arraycopy(R, 0, tmp, 0, R.length);
            R = tmp;
        }

        if (S.length != 0x1E) {
            byte[] tmp = new byte[0x1E];
            System.arraycopy(S, 0, tmp, 0, S.length);
            S = tmp;
        }

        byte[] CompleteSignature = new byte[2 * 0x1E];
        System.arraycopy(R, 0, CompleteSignature, 0, 0x1E);
        System.arraycopy(S, 0, CompleteSignature, 0x1E, 0x1E);
        System.arraycopy(CompleteSignature, 0, footer, 14*0x20, 2*0x1E);
    }
}