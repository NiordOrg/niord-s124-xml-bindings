# Niord XML Bindings

Java bindings and utilities for reading and writing S-100 exchange catalogues and
S-124 navigational warnings, used by Niord and Baleen projects. The build generates
Java types from the bundled XML schemas using Jakarta XML Binding (JAXB).

The library also provides dataset validation, conversion between JTS geometry and
S-124 geometry, and a builder for signed exchange-set ZIP files.

## Modules

| Module | Schema edition | Maven artifact | What it provides |
| --- | --- | --- | --- |
| [s-100](s-100/) | S-100 5.2.0 | `s100-5_2_0-xml-bindings` | Exchange catalogue types, metadata builders, XML adapters and certificate helpers |
| [s-124](s-124/) | S-124 2.0.0 | `s124-2_0_1-xml-bindings` | Navigational warning types, validation, geometry conversion and exchange-set creation |

The S-124 artifact name contains `2_0_1`, but its schema and Java API target
**S-124 2.0.0**. The Maven library version is separate: the POMs currently declare
`0.3.0`. S-124 depends on the S-100 module.

## Build and use locally

Use **JDK 21** (the version used by CI) and **Maven 3.6.3 or newer**. Run these
commands from the repository root:

```bash
java -version
mvn -version
mvn clean install
```

This generates the Java bindings, runs the tests, builds both JARs and installs
them in your local Maven repository. The first build needs network access to
resolve dependencies and schema imports. Generated Java sources appear in each
module's `src/main/generated/` directory.

After the local install, add this dependency inside your application's
`<dependencies>` element:

```xml
<dependency>
    <groupId>dma.dk.niord.s100.xml-bindings</groupId>
    <artifactId>s124-2_0_1-xml-bindings</artifactId>
    <version>0.3.0</version>
</dependency>
```

For catalogue-only applications, use `s100-5_2_0-xml-bindings` instead. See
[distribution and releases](docs/development.md#distribution-and-releases) for
the configured remote repositories and their different coordinates.

## See real examples

Generate sample Danish waters warnings and exchange sets:

```bash
mvn -pl s-124 -am test -Dtest=DanishWatersExamplesGenerator -Dsurefire.failIfNoSpecifiedTests=false
```

Open `target/danish-nw-examples/README.md` after the command finishes. The output
contains seven GML datasets, eight exchange-set ZIPs, an extracted combined set
and signing material. These are demonstration warnings; the exchange sets are
marked `notForNavigation`.

The generator uses OpenSSL for temporary signing keys and falls back to dummy
signatures if that setup fails. See the [example guide](docs/usage.md#generate-example-datasets)
for details and source links.

## Documentation

- [Using the bindings](docs/usage.md): read and write XML, populate dataset headers,
  validate content and convert geometry.
- [Creating exchange sets](docs/exchange-sets.md): configure metadata, supply a
  signer, understand the ZIP layout and issue cancellations.
- [Development and releases](docs/development.md): navigate the source, regenerate
  bindings, run tests, troubleshoot builds and prepare a release.

## License

This project is licensed under [Apache License 2.0](LICENSE).
