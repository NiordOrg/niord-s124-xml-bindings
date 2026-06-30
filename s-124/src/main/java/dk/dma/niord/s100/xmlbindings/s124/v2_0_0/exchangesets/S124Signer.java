package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignatureReference;

/**
 * Callback for signing the payloads that go into an S-124 exchange set:
 * each dataset XML file (referenced from CATALOG.XML) and CATALOG.XML itself
 * (yielding CATALOG.SIGN).
 *
 * <p>The caller owns the key material — wire a Java keystore, BouncyCastle, an
 * HSM or a remote signing service. The {@link S124ExchangeSetFactory} forwards
 * the algorithm requested via {@code signatureAlgorithm(...)} and the raw bytes
 * to sign.</p>
 */
@FunctionalInterface
public interface S124Signer {

    /**
     * Produce a signature over {@code payload} using {@code algorithm}.
     *
     * @param algorithm the requested S-100 SE signature algorithm
     * @param payload   the bytes to sign
     * @return the raw signature bytes
     */
    byte[] sign(S100SEDigitalSignatureReference algorithm, byte[] payload);
}
