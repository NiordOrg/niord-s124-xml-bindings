# Development and releases

[Project overview](../README.md) · [Using the bindings](usage.md) · [Exchange sets](exchange-sets.md)

## Source map

This is a two-module Maven library. The root POM builds `s-100` before `s-124`,
which depends on it. There is no application server to start.

| Location | Purpose |
| --- | --- |
| [pom.xml](../pom.xml) | Shared versions, Java level, build plugins and publication configuration |
| [s-100/pom.xml](../s-100/pom.xml), [s-124/pom.xml](../s-124/pom.xml) | Module dependencies and JAXB generation inputs |
| `s-*/src/main/resources/xsd/` | XML schemas and related resources |
| `s-*/src/main/resources/xjb/bindings.xjb` | JAXB customizations: packages, adapters and generated property behavior |
| `s-*/src/main/generated/` | Generated Java interfaces, implementations and factories; ignored by Git |
| `s-*/src/main/java/` | Handwritten adapters, builders, validators and utilities |
| `s-*/src/test/java/` | Unit tests, conformance regression tests and the example generator |
| [s-100/src/main/schemas.isotc211.org/](../s-100/src/main/schemas.isotc211.org/) | Local ISO schema mirror used by catalogue validation tests |
| [various documents/](../various%20documents/) | Reference specifications checked into the repository |
| [.github/workflows/](../.github/workflows/) | CI and publication workflows |

S-100 catalogue types are generated under `dk.dma.niord.s100.catalog._5_2`.
Handwritten S-100 helpers retain the `org.grad.eNav.s100` package. S-124 types
and current utilities use `dk.dma.niord.s100.xmlbindings.s124.v2_0_0`; its GML
types use sibling `s100.gml.base._5_0` and `s100.gml.profiles._5_0` packages.

## Build and test

Use JDK 21 to match CI and Maven 3.6.3 or newer. `mvn -version` shows the JDK Maven
actually uses, which may differ from the `java` on your shell's path. No Maven
wrapper is checked in; JitPack generates one during its own build.

Run commands from the root:

```bash
# Generate sources, compile, test and package both modules
mvn clean verify

# Also install both modules and the parent POM locally for consumers
mvn clean install

# Generate bindings without compiling the project
mvn generate-sources

# Test S-124 together with its S-100 dependency
mvn -pl s-124 -am test

# Run one test class across the reactor
mvn -pl s-124 -am test -Dtest=S124DatasetValidatorTest -Dsurefire.failIfNoSpecifiedTests=false
```

Test reports are in each module's `target/surefire-reports/`. JARs and source
JARs are in `s-100/target/` and `s-124/target/` after packaging. For generated
sample data, use the [example generator command](usage.md#generate-example-datasets).

The [CI workflow](../.github/workflows/ci.yml) runs on pushes and pull requests
to `main`. It uses Temurin 21, runs `mvn clean compile test --batch-mode`, checks
that both modules generated Java sources, packages JARs and uploads them as
build artifacts.

## Change schemas or bindings

1. Identify whether the change belongs in an XSD, an XJB customization or a
   handwritten helper. Edit those inputs rather than generated Java files.
2. Run `mvn generate-sources` and inspect the generated API. S-100 generation
   starts from `S100_ExchangeCatalogue.xsd`; S-124 starts from `124_2.0.0.xsd`
   plus the extension, XLink and profile-level inputs listed in its POM.
3. Update handwritten callers when the generated API changes. For behavior
   changes, add or adjust a regression test for the affected XML or rule.
4. Run `mvn clean verify`. Check generated XML as well as compilation when
   changing namespaces, adapters, geometry or coded values.

Generated files live outside Maven's usual `target/` directory. `mvn clean`
does not itself delete `src/main/generated/`; JAXB controls those directories
when generation runs. If obsolete types remain after a schema or package
change, remove only the two generated directories and regenerate. They should
contain no handwritten work.

Keep schema edition, Maven artifact name and library release version distinct.
In particular, do not infer a new schema edition from the existing S-124
artifact's `2_0_1` name.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| `invalid target release: 21` | Point Maven at JDK 21 and check `mvn -version`. |
| IDE cannot find `Dataset`, `ObjectFactory` or catalogue types | Run `mvn generate-sources`, then reload the Maven project. Confirm `src/main/generated/` is recognized as a source root. |
| S-124 cannot resolve the S-100 artifact | Build from the root with `-am`, or run `mvn install` at the root before building S-124 alone. |
| A targeted test fails in S-100 with “No tests matching pattern” | Add `-Dsurefire.failIfNoSpecifiedTests=false` when selecting an S-124-only test with `-am`. |
| JAXB generation fails while loading an imported schema | Read the first schema-resolution error. Generation may need network access for imported schemas; runtime S-124 XSD validation uses bundled resources. |
| Marshal succeeds but downstream XML validation fails | Marshal performs dataset-rule checks, not XSD validation. Run `S124XsdValidator.validate(xml)`. |
| Exchange-set build rejects a dataset | Check the exception's filename, clause and cause. Common issues are missing fields, an invalid filename, no extent or geometry, and the serialized size limit. |
| Geometry has swapped coordinates | Supply longitude/latitude to JTS. The converter handles the swap for GML. |
| Remote dependency cannot be resolved | Verify repository URL, group ID, artifact ID and exact published version. Local Maven and JitPack coordinates differ. |

## Distribution and releases

The repository configures three remote distribution paths. Configuration alone
does not establish that a particular version has been published successfully;
check the relevant build and resolve the artifacts before advertising a release.
The README's local-install instructions work without relying on a remote release.

| Destination | Configuration | Consumer group ID |
| --- | --- | --- |
| GitHub Packages | Root `distributionManagement` and [maven-publish.yml](../.github/workflows/maven-publish.yml) | `dma.dk.niord.s100.xml-bindings` |
| GitHub Pages Maven repository | Root `github-pages` profile and [maven-gh-pages.yml](../.github/workflows/maven-gh-pages.yml) | `dma.dk.niord.s100.xml-bindings` |
| JitPack | [jitpack.yml](../jitpack.yml) | `com.github.NiordOrg.niord-xml-bindings` for the current repository name |

The configured Packages URL is
`https://maven.pkg.github.com/NiordOrg/niord-xml-bindings`. The Pages workflow
advertises `https://niordorg.github.io/niord-xml-bindings/repository`. Both use the
same artifact IDs and group ID as a local Maven install. Packages consumers need
credentials with package-read access; keep those in Maven settings, not the POM.

For JitPack, add the following repository to the consuming POM and use the
JitPack group ID from the table. Select a successfully built tag or commit as
the dependency version:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

**Legacy coordinate in the smoke-test POM:** [test-pom.xml](../test-pom.xml)
still uses `com.github.NiordOrg.niord-s124-xml-bindings`, the old repository name.
Before using it to verify a new release, update its group ID and version to
the intended publication. `dependency:resolve` checks availability; `compile`
on this standalone POM does not exercise a real consumer because it contains
no Java sources.

### Prepare a release

1. Update the root version, both module versions and parent references, and the
   explicit S-100 dependency version in `s-124/pom.xml`. Keep these aligned.
   Update documentation examples and the smoke-test POM as appropriate.
2. Run `mvn clean verify`, review the changes and merge the release preparation
   through the repository's normal review process.
3. Tag the intended release commit with its exact Maven version (for example,
   `0.1.1` without a `v` prefix), then push that specific tag.
4. Check both publication workflows. Each is configured for numeric
   `major.minor.patch` tags and also supports manual dispatch. The Packages
   workflow runs `mvn deploy`; the Pages workflow runs
   `mvn clean deploy -Pgithub-pages` and copies artifacts onto `gh-pages`.
5. For JitPack, request/check the build for the selected version. Its configured
   install step generates a Maven 3.6.3 wrapper and runs `./mvnw install -DskipTests`
   with OpenJDK 21. A JitPack build is therefore not a substitute for the tested
   release build.
6. Resolve the release from the distribution channel consumers will use. After
   correcting `test-pom.xml` for JitPack, run
   `mvn -f test-pom.xml dependency:resolve`. For the other channels, test a
   consumer POM with their repository URL and native coordinates.

The parent artifact is spelled `xml-bindingings-root-pom` in the POM. Preserve
that spelling when checking publication paths or resolving parent artifacts.
