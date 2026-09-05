# Using the bindings

[Project overview](../README.md) · [Exchange sets](exchange-sets.md) · [Development](development.md)

The examples use the current S-124 API under
`dk.dma.niord.s100.xmlbindings.s124.v2_0_0`. Older utilities remain under
`dk.baleen.s100.xmlbindings.s124.v1_0_0`; use the `v2_0_0` package for new integrations.

## Read, validate and write a dataset

An S-124 dataset is a GML document containing identification information and
members such as a warning preamble, warning parts and references. The generated
`Dataset` interface represents that document in Java.

This complete example reads a file, validates its XML, converts it into Java
objects, then writes validated XML to another file. Run it with an input and an
output path, using a project with the S-124 dependency from the README.

```java
import java.nio.file.Files;
import java.nio.file.Path;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124Utils;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124XsdValidator;

public class DatasetRoundTrip {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: DatasetRoundTrip input.GML output.GML");
        }
        String input = Files.readString(Path.of(args[0]));
        S124XsdValidator.validate(input);
        Dataset dataset = S124Utils.unmarshallS124(input);

        String output = S124Utils.marshalS124(dataset);
        S124XsdValidator.validate(output);
        Files.writeString(Path.of(args[1]), output);
    }
}
```

The method is spelled **`unmarshallS124`**, with two `l` characters.
Unmarshalling alone does not perform XSD or dataset-rule validation. A round trip
preserves the object model rather than the original XML bytes: formatting,
namespace prefixes, dates and coded values can change. Re-serialized XML needs
a new signature if it was previously signed.

## Understand the two validation layers

| API | Checks | Failure |
| --- | --- | --- |
| `S124DatasetValidator.validate(dataset)` | Implemented rules that the XSD cannot express: one preamble, profile/purpose agreement, UTC times of day and association roles, among others | `S124ConformanceException` |
| `S124XsdValidator.validate(xml)` | XML structure, required elements and types against the bundled S-124 2.0.0 schema | `SAXException` (or `IOException` while reading) |

`S124Utils.marshalS124(dataset)` first fills missing numeric codes for recognized
enumeration and closed code-list labels, then runs the dataset-rule validator.
**This normalization mutates the dataset.** Conflicting labels and codes are
rejected. The method does not run XSD validation; call `S124XsdValidator` on the
result, as above, or use the exchange-set factory, which does so by default.

For diagnostics, `S124DatasetValidator.violations(dataset)` returns a list without
throwing. Each violation has `clause()` and `message()` accessors. The same list
is available through `S124ConformanceException.getViolations()`. Direct validation
does not fill missing codes; `S124CodedValues.fillMissingCodes(dataset)` provides
that operation separately.

Two marshal overloads expose formatting and diagnostic controls:

- `marshalS124(dataset, false)` produces unindented XML with normal rule checks.
- `marshalS124(dataset, true, false)` skips code completion and rule checks. Use
  this to inspect intentionally invalid data, such as a failing test fixture.

The S-124 XSD validator resolves its schemas from the JAR's `/xsd/` resources and
does not need network access. It validates datasets, not exchange catalogues.
The implemented checks are not complete certification of an operational service:
for example, the producer-code check tests its shape, not membership in the live
producer registry, and open `navwarnTypeDetails` values are not assigned numeric codes.

## Create Java objects and populate the header

The JAXB configuration generates interfaces with implementation classes in
`impl` subpackages. Use generated `ObjectFactory` classes to create objects.
Many collection getters return a live list; add members directly to that list.

`S124DatasetInfo` supplies default identification values and copies them into a
dataset header. The following fragment belongs inside a method:

```java
var factory = new dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ObjectFactory();
var gmlFactory = new dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.ObjectFactory();

var dataset = factory.createDataset();
dataset.setId("DK.NW.011.26");
dataset.setDatasetIdentificationInformation(gmlFactory.createDataSetIdentificationType());
dataset.setMembers(factory.createDatasetMembers());

var info = new dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124DatasetInfo(
        "DKNW01126", "DK00");
info.setTitle("The Sound. Drogden Channel. Light buoy unlit.");
info.setAbstractText("Demonstration navigational warning.");
info.applyTo(dataset);
```

This creates **only the container and header**. Add a complete `NavwarnPreamble`
and the appropriate warning members before marshalling. The
[Danish waters generator](../s-124/src/test/java/dk/dma/niord/s100/xmlbindings/s124/v2_0_0/examples/DanishWatersExamplesGenerator.java)
shows complete warnings, bilingual text, member associations and an in-force
bulletin. The smaller
[test dataset helper](../s-124/src/test/java/dk/dma/niord/s100/xmlbindings/s124/v2_0_0/util/S124TestDatasets.java)
helps explain required fields. Both are test sources, not public APIs shipped
in the library JAR.

| Header field | Default |
| --- | --- |
| Encoding specification | `S-100 Part 10b`, edition `1.0` |
| Product | `S-124`, edition `2.0.0` |
| Language | `eng` |
| Purpose / application profile | `BASE` / `"1"` |
| Update number | `0` |
| Reference date | Current date in UTC |
| Topic category | `OCEANS` |
| File identifier in this example | `124DK00DKNW01126.GML` |

Set the reference date explicitly for reproducible output. `applyTo(dataset)`
requires an existing identification object and replaces its identification fields.
Calling `setPurpose(UPDATE)` also changes the profile to `"2"`; setting either
supported profile changes the purpose to match. Changing the agency or dataset ID
after constructing `S124DatasetInfo` does not recalculate its file identifier;
set that field explicitly if needed.

The marshal helper writes `LocalDate` values in ISO date form and converts
`OffsetDateTime` values to UTC. Time-of-day attributes represented by
`XMLGregorianCalendar` must already specify UTC. Use three-letter language codes
such as `eng` and `dan`, as required by the dataset schema.

## Convert geometry

`GeometryS124Converter` translates between JTS geometries and S-124 spatial
properties. Its input coordinates use **longitude, latitude**; it swaps them to
**latitude, longitude** for GML. Supply geographic coordinates in EPSG:4326;
the converter does not reproject another coordinate system.

This fragment creates a point and converts it in both directions:

```java
var geometryFactory = new org.locationtech.jts.geom.GeometryFactory(
        new org.locationtech.jts.geom.PrecisionModel(), 4326);
var point = geometryFactory.createPoint(
        new org.locationtech.jts.geom.Coordinate(12.7200, 55.5467));

var properties = dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.GeometryS124Converter
        .geometryToS124PointCurveSurfaceGeometry(point);
var restored = dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.GeometryS124Converter
        .pointCurveSurfaceToGeometry(properties);
```

Attach the resulting properties to the geometry wrapper of the relevant feature;
the generator's `part` method shows how to populate `NavwarnPart.Geometry`.

Points, line strings, polygons and collections are supported on the generation
path. Collections become separate properties, polygon holes are preserved, and
ring orientation is normalized. Empty geometries and invalid minimum position
counts are rejected. An overload accepts a `Supplier<String>` for deterministic
GML IDs; ensure those IDs are unique within the dataset.

Reading properties back combines them using geometric union. It can merge
overlapping members and change ring or vertex order. Unsupported encodings such
as composite curves or unsupported interpolation raise
`UnsupportedOperationException`; that does not necessarily mean the source GML
is invalid.

## Read and write S-100 catalogues

For a `CATALOG.XML` file, use
`org.grad.eNav.s100.utils.S100ExchangeSetUtils`:

- `unmarshallS100ExchangeSetCatalogue(xml)` returns an `S100ExchangeCatalogue`.
- `marshalS100ExchangeSetCatalogue(catalogue)` returns formatted XML; its second
  argument can disable formatting.

These helpers bind XML; they do not verify signatures or run catalogue XSD
validation. The S-100 module also provides `S100ExchangeCatalogueBuilder` and
builders for dataset, support-file and catalogue discovery metadata. For S-124
delivery, start with the [S-124 exchange-set factory](exchange-sets.md), which
applies the product-specific metadata rules.

## Generate example datasets

From the repository root:

```bash
mvn -pl s-124 -am test -Dtest=DanishWatersExamplesGenerator -Dsurefire.failIfNoSpecifiedTests=false
```

`-am` builds the S-100 dependency in the same reactor. The Surefire flag allows
that module to proceed even though it has no test named `DanishWatersExamplesGenerator`.
The generator is explicitly selected because its name is not included in the
normal test suite.

It writes to `target/danish-nw-examples/`:

| Path | Contents |
| --- | --- |
| `README.md` | Warning descriptions and the signing setup used |
| `datasets/` | Six warning datasets and one in-force bulletin |
| `exchange-sets/` | A ZIP per dataset, a combined ZIP and its extracted contents |
| `signing/` | Temporary signing material |

The scenarios cover an unlit buoy, cable work, drifting containers, a firing
exercise, an unlit turbine and a bridge closure. They demonstrate points, curves,
surfaces, English/Danish text and references to warnings in force.

Each dataset and each catalogue is XSD-validated before being written. The
generator creates a temporary P-384 key and self-signed certificate in the JVM and
signs with real `SHA384withECDSA` signatures in the DER form S-100 Part 15 embeds;
OpenSSL is not needed. The examples are for development, not operational
navigation or a trusted certificate chain.
Re-running the generator clears its previous dataset and exchange-set output.
