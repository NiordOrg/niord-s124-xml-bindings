package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignatureReference;

/**
 * Callback for signing the payloads that go into an S-124 exchange set:
 * each dataset XML file (referenced from CATALOG.XML) and CATALOG.XML itself
 * (yielding CATALOG.SIGN).
 *
 * <p>The caller owns the key material - wire a Java keystore, BouncyCastle, an
 * HSM or a remote signing service. The {@link S124ExchangeSetFactory} forwards
 * the algorithm requested via {@code signatureAlgorithm(...)} and the exact bytes
 * to sign.</p>
 *
 * <h2>Signature encoding</h2>
 *
 * <p>The returned bytes are embedded in the catalogue as they are, Base64 encoded once by JAXB
 * because the signature elements are typed {@code xs:base64Binary}. They must therefore already
 * be in the one form S-100 Part 15, clause 15-8.4, defines: the ASN.1 DER {@code SEQUENCE} of
 * the two ECDSA {@code INTEGER}s r and s - "The encoding of the two R,S large integers is a
 * Base64 ASN.1 byte sequence. These are produced natively by the openssl implementation". That
 * is exactly what {@code java.security.Signature} returns for {@code "SHA384withECDSA"}:</p>
 * <pre>{@code
 * S124Signer signer = (algorithm, payload) -> {
 *     Signature ecdsa = Signature.getInstance("SHA384withECDSA");
 *     ecdsa.initSign(privateKey);
 *     ecdsa.update(payload);
 *     return ecdsa.sign();
 * };
 * }</pre>
 *
 * <p>Do not return the raw {@code r||s} concatenation - the IEEE P1363 form, which the JCA
 * algorithm {@code "SHA384withECDSAinP1363Format"} yields as exactly 96 bytes for P-384. It
 * signs and verifies within the producer's own code, but an ECDIS following Part 15 cannot
 * decode it. The factory converts nothing and rejects a value that is not a DER sequence of two
 * integers with an {@link S124ExchangeSetFactory.ExchangeSetException}, so that mistake surfaces
 * at build time rather than on board.</p>
 */
@FunctionalInterface
public interface S124Signer {

    /**
     * Produce a signature over {@code payload} using {@code algorithm}.
     *
     * @param algorithm the requested S-100 SE signature algorithm
     * @param payload   the bytes to sign
     * @return the signature as the DER {@code SEQUENCE} of the ECDSA integers r and s (S-100
     *         Part 15, clause 15-8.4), without Base64 encoding
     */
    byte[] sign(S100SEDigitalSignatureReference algorithm, byte[] payload);
}
