/**
 * Packaging and digital signing of S-124 v2.0.0 navigational warnings as S-100 Part 17
 * exchange sets.
 *
 * <p>{@link dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.S124ExchangeSetFactory}
 * is the entry point; it marshals the datasets, writes the catalogue and signs both. This
 * document explains the parts a caller has to get right from outside the code: the trust
 * model, how certificates must be configured, and what the receiving system does with the
 * result.</p>
 *
 * <h2>The trust model</h2>
 *
 * <p>An ECDIS must be able to prove that a dataset really came from the producer it claims,
 * without contacting anything on the network. S-100 Part 15 achieves this with a chain of
 * certificates that ends at a root the receiving system already holds.</p>
 *
 * <p>Three roles take part:</p>
 * <ul>
 *   <li><b>Scheme Administrator (SA)</b> — the root of trust, normally the IHO. Its root
 *       certificate is installed on the receiving system independently of any exchange set
 *       and, per Part 15 clause 15-8.11.1, is <em>never</em> shipped inside one. The exchange
 *       set only names it, via {@code <schemeAdministrator id="IHO"/>}.</li>
 *   <li><b>Domain Coordinator (DC)</b> — optional intermediate authority (the spec's example
 *       is the IMO) that the SA authorises to issue certificates to producers in its
 *       domain.</li>
 *   <li><b>Data Server</b> — the producer, for example a national hydrographic office. Its
 *       certificate signs the data and is always carried in the exchange set.</li>
 * </ul>
 *
 * <p>Whoever issued the Data Server certificate decides how the exchange set must be
 * configured, and getting it wrong yields output that passes XSD validation but cannot be
 * authenticated on board.</p>
 *
 * <h3>Case 1 — the SA issued the Data Server certificate directly</h3>
 *
 * <p>Chain: {@code IHO -> Data Server}. Supply only the producer's certificate:</p>
 * <pre>{@code
 * S124ExchangeSetFactory.builder()
 *         .certificatePem(dataServerPem)
 *         ...
 * }</pre>
 * <p>which emits</p>
 * <pre>{@code
 * <schemeAdministrator id="IHO"/>
 * <certificate id="cer1" issuer="IHO">...</certificate>
 * }</pre>
 *
 * <h3>Case 2 — a Domain Coordinator issued the Data Server certificate</h3>
 *
 * <p>Chain: {@code IHO -> Domain Coordinator -> Data Server}. The receiving system holds the
 * SA root but knows nothing of the coordinator, so Part 15 clause 15-8.7 obliges the producer
 * to ship the coordinator's certificate too: "The Data Server must always include the digital
 * certificate of its Domain Coordinator to ensure the Data Client OEM has all the certificates
 * required to perform a full certificate path validation without any external access."</p>
 * <pre>{@code
 * S124ExchangeSetFactory.builder()
 *         .certificatePem(dataServerPem)
 *         .intermediateCertificatePems(List.of(domainCoordinatorPem))
 *         ...
 * }</pre>
 * <p>which emits</p>
 * <pre>{@code
 * <schemeAdministrator id="IHO"/>
 * <certificate id="ca1"  issuer="IHO">...</certificate>
 * <certificate id="cer1" issuer="ca1">...</certificate>
 * }</pre>
 *
 * <p>Longer chains work the same way: pass every intermediate between the producer and the
 * SA. Order does not matter — the chain is derived by matching each certificate's issuer name
 * to the subject name of the certificate above it, and a certificate that issues nothing else
 * in the set is rejected at build time rather than shipped as an unresolvable path.</p>
 *
 * <h2>Why {@code issuer} is an id and not a name</h2>
 *
 * <p>The {@code issuer} attribute holds the <em>id of the issuing element</em> — either the
 * {@code schemeAdministrator} id or the id of another certificate in the same container — and
 * not the issuer's X.500 distinguished name. Part 15 clause 15-8.6 puts it as: "Each XML
 * certificate definition will also include an attribute, 'issuer' defining the id of the
 * issuer, either the SA (identified by the schemeAdministrator id) or a domain coordinator
 * (whose certificate will also be included in the Exchange Set)."</p>
 *
 * <p>The distinction matters because the receiving system resolves {@code issuer} by lookup
 * within the exchange set, falling back to the separately installed SA root. A distinguished
 * name matches neither, so verification fails even though the XML is schema-valid. The
 * identity used for {@code schemeAdministrator} is therefore an agreed label ("IHO" by
 * default, see {@code Builder.schemeAdministrator(String)}) rather than anything read out of
 * the certificate itself.</p>
 *
 * <h2>What gets signed</h2>
 *
 * <p>Two different signature containers appear in an exchange set, for two different
 * purposes:</p>
 * <ul>
 *   <li><b>Dataset files</b> are signed individually and their signatures live in the
 *       catalogue, in each dataset's discovery metadata. Part 15 clause 15-8.11.4 realises
 *       these as {@code S100_SE_SignatureOnData}, carrying the mandatory {@code dataStatus}
 *       — always {@code unencrypted} for S-124, which is neither compressed nor encrypted.</li>
 *   <li><b>CATALOG.XML</b> is not covered by its own metadata, so it is signed as an
 *       auxiliary file. Clauses 15-8.7 and 15-8.11.2 require a self-contained
 *       {@code StandaloneDigitalSignature} document — the signed file name, every certificate
 *       needed to authenticate it, and the signature — written here as CATALOG.SIGN. Bare
 *       signature bytes are not sufficient.</li>
 * </ul>
 *
 * <p>Both declare the same certificate chain, so either file can be verified on its own.</p>
 *
 * <p>Signatures are produced by the caller-supplied
 * {@link dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.S124Signer}, which keeps key
 * material out of this library: back it with a keystore, an HSM or a remote signing service.
 * The algorithm defaults to {@code ECDSA-384-SHA2}, which clause 15-8.7 mandates
 * ("The digitalSignatureReference field must be encoded 'ECDSA-384-SHA2'"). The signer must
 * return the ECDSA R,S pair; the factory base64-encodes it into the XML.</p>
 *
 * <h2>Cancellations and certificate rotation</h2>
 *
 * <p>A fileless cancellation reuses the cancelled dataset's original signature (Part 17
 * clause 17-4.4.1), which was made with whatever certificate was current when the dataset was
 * published. Certificates get replaced when they expire, so a cancellation issued after a
 * rotation carries a signature that the current certificate cannot verify.</p>
 *
 * <p>Clause 17-4.4.1 also requires the entry to keep "all other mandatory metadata fields
 * also set to the same values as the original, with the exception of the issueDate", so a
 * cancellation is built from the original catalogue entry rather than from the current
 * configuration, which may have moved on. Only the producer knows which certificate signed
 * the original, so pass that too when it has since been replaced:</p>
 * <pre>{@code
 * new Cancellation(originalDiscoveryMetadata, cancellationIssueDate,
 *         List.of(certificateThatSignedTheOriginal));
 * }</pre>
 * <p>The original entry is copied, never modified; only its purpose, issue date and the
 * certificate reference of the reused signature differ in what is emitted. That certificate
 * is carried alongside the current one, sharing ids with it where the chains overlap. The
 * shorter constructor omits the list and means "signed with the exchange set's current Data
 * Server certificate", which is correct as long as no rotation has happened in between.</p>
 *
 * <h2>Verification, from the receiving system's point of view</h2>
 *
 * <ol>
 *   <li>Read a signature and follow its {@code certificateRef} to a certificate in the
 *       container — for datasets the Data Server certificate, likewise for CATALOG.SIGN.</li>
 *   <li>Follow that certificate's {@code issuer} to the element that issued it, repeating
 *       until the reference is the {@code schemeAdministrator} id.</li>
 *   <li>Authenticate the chain against the locally installed SA root certificate, then verify
 *       the signature with the Data Server public key.</li>
 * </ol>
 *
 * <p>Every hop must resolve inside the exchange set, which is the reason intermediates have
 * to be supplied.</p>
 *
 * @see <a href="https://iho.int/en/standards-and-specifications">S-100 Edition 5.2.0, Part 15
 *      (Data Protection Scheme) and Part 17 (Discovery Metadata for Information Exchange
 *      Catalogues)</a>
 */
package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;
