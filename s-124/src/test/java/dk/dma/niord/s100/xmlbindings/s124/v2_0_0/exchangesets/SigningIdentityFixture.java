package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * A throwaway signing identity for tests: an EC P-384 key pair and the X.509 certificate that
 * certifies it, generated inside the JVM so the suite needs neither openssl nor committed
 * private keys.
 * <p/>
 * {@link #signer()} produces the signature value S-100 Part 15, clause 15-8.4, embeds in the
 * catalogue - the ASN.1 DER {@code SEQUENCE} of the two ECDSA {@code INTEGER}s r and s - which
 * is exactly what the JCA {@value #JCA_ALGORITHM} algorithm returns. Nothing here converts
 * between signature representations, and nothing in the library does either: the bytes the
 * signer returns are the bytes the catalogue Base64-encodes.
 */
public final class SigningIdentityFixture {

    /**
     * The JCA name of the algorithm S-100 Part 15, clause 15-8.7, mandates (ECDSA over P-384
     * with SHA-384) in the DER output form the same Part embeds. Its sibling
     * {@code SHA384withECDSAinP1363Format} returns the raw 96-byte r||s concatenation, which the
     * factory rejects.
     */
    public static final String JCA_ALGORITHM = "SHA384withECDSA";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final KeyPair keyPair;
    private final X509Certificate certificate;

    private SigningIdentityFixture(KeyPair keyPair, X509Certificate certificate) {
        this.keyPair = keyPair;
        this.certificate = certificate;
    }

    /** An identity whose certificate is self-signed, standing in for one the SA issued directly. */
    public static SigningIdentityFixture selfSigned(String commonName) {
        KeyPair keyPair = newKeyPair();
        X500Name name = nameOf(commonName);
        return new SigningIdentityFixture(keyPair, certify(name, keyPair.getPublic(), name, keyPair.getPrivate()));
    }

    /** An identity whose certificate {@code issuer} signed, as a domain coordinator issues a Data Server's. */
    public static SigningIdentityFixture issuedBy(SigningIdentityFixture issuer, String commonName) {
        KeyPair keyPair = newKeyPair();
        X500Name issuerName = X500Name.getInstance(issuer.certificate.getSubjectX500Principal().getEncoded());
        return new SigningIdentityFixture(keyPair,
                certify(nameOf(commonName), keyPair.getPublic(), issuerName, issuer.keyPair.getPrivate()));
    }

    public X509Certificate certificate() {
        return certificate;
    }

    public PublicKey publicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey privateKey() {
        return keyPair.getPrivate();
    }

    /** The certificate in the form the factory takes it: the Base64 body of its PEM, without armour. */
    public String certificatePem() {
        try {
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A signer producing real {@value #JCA_ALGORITHM} signatures with this identity's private key. */
    public S124Signer signer() {
        return (algorithm, payload) -> sign(payload);
    }

    public byte[] sign(byte[] payload) {
        try {
            Signature signature = Signature.getInstance(JCA_ALGORITHM);
            signature.initSign(keyPair.getPrivate());
            signature.update(payload);
            return signature.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signing failed", e);
        }
    }

    /** Whether {@code signature}, in the Part 15 DER form, authenticates {@code payload} under {@code key}. */
    public static boolean verify(PublicKey key, byte[] payload, byte[] signature) {
        try {
            Signature verifier = Signature.getInstance(JCA_ALGORITHM);
            verifier.initVerify(key);
            verifier.update(payload);
            return verifier.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp384r1"), RANDOM);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static X500Name nameOf(String commonName) {
        return new X500Name("CN=" + commonName + ", O=Test Only, C=DK");
    }

    private static X509Certificate certify(X500Name subject, PublicKey subjectKey, X500Name issuer,
            PrivateKey issuerKey) {
        try {
            Instant now = Instant.now();
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    issuer,
                    new BigInteger(64, RANDOM),
                    Date.from(now.minus(1, ChronoUnit.HOURS)),
                    Date.from(now.plus(365, ChronoUnit.DAYS)),
                    subject,
                    subjectKey);
            ContentSigner signer = new JcaContentSignerBuilder(JCA_ALGORITHM).build(issuerKey);
            X509CertificateHolder holder = builder.build(signer);
            return new JcaX509CertificateConverter().getCertificate(holder);
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate a test certificate", e);
        }
    }
}
