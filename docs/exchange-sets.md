# Creating S-124 exchange sets

[Project overview](../README.md) · [Using the bindings](usage.md) · [Development](development.md)

An exchange set packages datasets together with a discovery catalogue and digital
signatures. `S124ExchangeSetFactory` builds the complete ZIP in memory. You supply
the datasets, producer metadata, certificate and signing callback; the factory
handles serialization, catalogue construction and packaging.

## Build a ZIP

This helper accepts populated datasets, the Base64 body of a Data Server
certificate and an application-provided signer:

```java
import java.util.List;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.S124ExchangeSetFactory;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets.S124Signer;

public class ExchangeSetExample {
    public static byte[] create(List<Dataset> datasets, String certificatePem,
                                S124Signer signer) {
        return S124ExchangeSetFactory.builder()
                .datasets(datasets)
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .emails(List.of("warnings@example.org"))
                .certificatePem(certificatePem)
                .signer(signer)
                .description("Example S-124 navigational warnings")
                .notForNavigation(true)
                .build()
                .toBytes();
    }
}
```

Write the returned bytes with `Files.write(path, zipBytes)`. Use your own
organization, contact details and registered producer code in an integration. At least one
dataset or cancellation is required. The producer code must contain exactly
four alphanumeric characters.

Producer contact information is mandatory for dataset metadata. Supply an email
address, phone number, postal address fields, `onlineResource(...)` or
`contactInstructions(...)`; the example email above is a placeholder.

Each dataset must have a usable bounding envelope or member geometry from which
the factory can calculate its extent. A geometry-free in-force bulletin therefore
needs an explicit envelope. The example generator demonstrates that case.

## Supply a signer

`S124Signer.sign(algorithm, payload)` receives the exact bytes to sign and returns
the signature. The factory invokes it for each dataset and for the final
catalogue. Key storage and signing are the application's responsibility; a
keystore, HSM or signing service can implement the callback.

The builder defaults to `ECDSA_384_SHA_2` and rejects other algorithms.

### Signature encoding

Return the signature as S-100 Part 15, clause 15-8.4, embeds it: the ASN.1 DER
`SEQUENCE` of the two ECDSA integers r and s, **not** Base64 encoded. That is what
Java's `SHA384withECDSA` returns and what OpenSSL produces:

```java
S124Signer signer = (algorithm, payload) -> {
    Signature ecdsa = Signature.getInstance("SHA384withECDSA");
    ecdsa.initSign(privateKey);
    ecdsa.update(payload);
    return ecdsa.sign();
};
```

The factory performs no conversion. JAXB applies the single Base64 layer when it
writes the `xs:base64Binary` signature elements. A value that is not a DER sequence
of two integers is rejected with `ExchangeSetException` before anything is
packaged. The common mistake is the raw 96-byte `r||s` form produced by
`SHA384withECDSAinP1363Format`: it verifies in your own code, but no Part 15
reader can decode it.

The repository's tests generate throwaway P-384 keys and certificates in the JVM
(`SigningIdentityFixture`, BouncyCastle in test scope only) and verify real signatures
end to end. The
[example generator](../s-124/src/test/java/dk/dma/niord/s100/xmlbindings/s124/v2_0_0/examples/DanishWatersExamplesGenerator.java)
signs with the same class.

Use a signing key that matches `certificatePem`. If a Domain Coordinator issued
the certificate, supply its intermediate certificate chain through
`intermediateCertificatePems(...)`. The factory orders and checks issuer links
cryptographically. The Scheme Administrator root is installed separately by the
consumer. The default administrator ID is `IHO` and can be changed with
`schemeAdministrator(...)`.

Despite the `certificatePem` name, certificate strings must contain **only the
Base64 body**, without PEM header/footer lines or whitespace. This also applies
to intermediate and cancellation certificate chains. To read a certificate file
inside a method:

```java
String certificatePem = java.nio.file.Files.readString(java.nio.file.Path.of("signer-cert.pem"))
        .replaceAll("-----(BEGIN|END) CERTIFICATE-----", "")
        .replaceAll("\\s", "");
```

The factory checks the shape of the callback output, not the key behind it. It is
not a signature verifier or a substitute for consumer trust validation, and a
successful ZIP build alone does not prove the callback used the private key that
matches `certificatePem`.

## ZIP layout

```text
S100_ROOT/
├── CATALOG.XML
├── CATALOG.SIGN
└── S-124/
    ├── DATASET_FILES/
    │   └── 124DK00DKNW01126.GML
    ├── CATALOGUES/
    └── SUPPORT_FILES/
```

`CATALOG.XML` describes the datasets and carries their signatures and certificate
metadata. `CATALOG.SIGN` is an XML `StandaloneDigitalSignature` document signing
the catalogue bytes. The `CATALOGUES` and `SUPPORT_FILES` directories are currently
created empty by this factory.

Dataset filenames follow `124<producer code><alphanumeric unique code>.GML`.
When a header specifies `datasetFileIdentifier`, the factory uses that name and
checks its format and producer prefix. It rejects duplicate filenames and invalid
declared names. Keeping the header and packaged filename aligned matters because
the header is part of the signed payload.

## Checks and defaults

Before signing a dataset, the factory completes missing codes, checks the
implemented dataset rules, validates the XML against the bundled S-124 schema
and enforces a **51,200-byte (50 × 1024) limit** on the serialized dataset.
The factory uses formatted dataset XML, so whitespace counts toward that limit.

`validateAgainstSchema(false)` disables the dataset XSD check only. It does not
disable the dataset-rule validator or the size limit. Keep the default for
normal exchange-set creation.

Catalogue XSD validation is exercised in the repository's tests and generator;
the production factory does not run it. See [validation details](usage.md#understand-the-two-validation-layers).

| Setting | Default or behavior |
| --- | --- |
| `notForNavigation` | `true`; set deliberately for the intended delivery |
| Product specification | Navigational Warnings, S-124 2.0.0, category 3 |
| `specificUsage` | `Navigational Warning Service`; `null` omits it, other values are rejected |
| Classification | Unclassified |
| Locale | English |
| Exchange-set identifier | `urn:mrn:iho:s124:exchangeset:<random UUID>` |
| Dataset identifier | Preamble interoperability identifier, or `urn:mrn:iho:s124:<dataset gml:id>` |

Dataset identifiers must be Marine Resource Names (MRNs). A custom
`datasetMrnPrefix(...)` changes the fallback prefix. The factory derives geographic
and temporal metadata from each dataset: it pads point/line extents into bounding
boxes with positive spans and includes a temporal extent when the preamble has
a cancellation date. The catalogue description for a dataset comes from its
preamble's general area and locality.

`S124ConformanceException` reports dataset-rule failures with clause references.
`S124ExchangeSetFactory.ExchangeSetException` reports packaging, schema,
certificate and other exchange-set failures. Invalid or missing builder settings
can fail earlier with `IllegalArgumentException` or `NullPointerException`.

## Cancel a previously delivered dataset

A fileless cancellation is a catalogue entry that tells a consumer to remove a
previous dataset. It retains the original filename, signature and mandatory
metadata, sets its purpose to cancellation and ships no replacement dataset file.

Create `new S124ExchangeSetFactory.Cancellation(originalMetadata, issueDate)` and
pass the entries to `builder.cancellations(...)`. `originalMetadata` is the
`S100DatasetDiscoveryMetadata` from the original catalogue. A cancellation-only
set still needs the organization, producer code, certificate and signer because
the new catalogue must be signed.

The two-argument constructor assumes the original signature used the current
Data Server certificate. If the signing certificate has changed, use the overload
that also accepts the original certificate chain, signing certificate first.
For counter-signed originals, the full constructor additionally accepts a map
of counter-signer chains keyed by their original `certificateRef` values. This
allows the factory to preserve signature chains and update certificate references
within the new catalogue.

Retain the original catalogue entry and the certificate chain that signed it
together. The factory copies the entry and never modifies it, so the same record
can be reused in later exchange sets under whichever certificate is then current.
Passing the current certificate as the original chain is harmless: identical
certificates are carried once, and the reused signature references the current
entry.

### Migrating from 0.0.12

`Cancellation` used to take the file name, identifiers, bounding box and a list of
signature objects whose `certificateRef` the caller had to set to the factory's
internal `cer1`. That constructor is gone, and it never survived a certificate
rotation. Pass the retained `S100DatasetDiscoveryMetadata` entry instead, together
with the chain that signed it; the factory allocates every certificate id and
rewrites the references itself. Signers must return DER-encoded signatures (see
above); the factory now rejects any other form.

See the [exchange-set tests](../s-124/src/test/java/dk/dma/niord/s100/xmlbindings/s124/v2_0_0/exchangesets/S124ExchangeSetFactoryTest.java)
for cancellation, rollover and certificate-chain examples.
