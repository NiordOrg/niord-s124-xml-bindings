# Niord S-124 XML Bindings

This repository contains JAXB XML bindings for S-100 and S-124 for use in Niord and Baleen projects.

## Modules

- **s-100**: XML bindings for S-100 5.2.0
- **s-124**: XML bindings for S-124 2.0.0 (Navigational Warnings)

## Quick Start

Add to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.NiordOrg.niord-s124-xml-bindings</groupId>
    <artifactId>s124-2_0_1-xml-bindings</artifactId>
    <version>v0.0.5</version>
</dependency>
```


# Release Process

This document describes the release process for the Niord S-124 XML Bindings project.

## Prerequisites

- Write access to the repository
- Maven 3.6.3 or higher (required for maven-compiler-plugin:3.13.0)
- Java 21 (required for the project compilation)
- Git (for version control and tagging)

## Release Steps

### 1. Update Version Numbers

Before releasing, ensure all POMs have the correct version number:

- Parent POM: `/pom.xml`
- S-100 module: `/s-100/pom.xml`
- S-124 module: `/s-124/pom.xml`

All three should have matching versions (e.g., `0.0.5`).

### 2. Commit Changes

```bash
git add .
git commit -m "Prepare release v0.0.5"
git push origin main
```

### 3. Create and Push Tag

```bash
git tag v0.0.5
git push origin v0.0.5
```

The tag format must be `v<version>` (e.g., `v0.0.5`, `v1.0.0`).

### 4. Verify JitPack Build

After pushing the tag, JitPack will automatically build the artifacts. Monitor the build at:
- https://jitpack.io/#NiordOrg/niord-s124-xml-bindings

You can also test the new release using the `test-pom.xml`:
```bash
# Update test-pom.xml to use the new version, then test
mvn -f test-pom.xml dependency:resolve
```

The release is complete once JitPack shows a successful build and the artifacts are available for download.

## Using Released Packages

All packages are distributed via JitPack, which automatically builds and publishes artifacts from GitHub releases.

Add to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- S-100 XML Bindings -->
    <dependency>
        <groupId>com.github.NiordOrg.niord-s124-xml-bindings</groupId>
        <artifactId>s100-5_2_0-xml-bindings</artifactId>
        <version>v0.0.5</version>
    </dependency>
    
    <!-- S-124 XML Bindings -->
    <dependency>
        <groupId>com.github.NiordOrg.niord-s124-xml-bindings</groupId>
        <artifactId>s124-2_0_1-xml-bindings</artifactId>
        <version>v0.0.5</version>
    </dependency>
</dependencies>
```

**Note:** Use the latest version available at https://jitpack.io/#NiordOrg/niord-s124-xml-bindings


## Troubleshooting

### JitPack Build Issues

- The project uses a `jitpack.yml` file to specify Maven 3.6.3 (required for the compiler plugin)
- JitPack builds may take several minutes due to JAXB code generation
- Monitor build status at: https://jitpack.io/#NiordOrg/niord-s124-xml-bindings

If JitPack builds fail:
1. Check the build log at: https://jitpack.io/com/github/NiordOrg/niord-s124-xml-bindings/[VERSION]/build.log
2. Verify the `jitpack.yml` configuration

### Dependency Resolution Issues

- Verify you're using the correct JitPack repository URL: `https://jitpack.io`
- Check that the version format includes the `v` prefix (e.g., `v0.0.5`)
- Ensure you're using the correct artifactId (e.g., `s124-2_0_1-xml-bindings`)
- Test dependency resolution using: `mvn -f test-pom.xml dependency:resolve`

## JitPack Configuration

The project includes a `jitpack.yml` file with the following configuration:

```yaml
jdk:
  - openjdk21
install:
  - mvn wrapper:wrapper -Dmaven=3.6.3
  - ./mvnw install -DskipTests
```

This ensures JitPack uses:
- OpenJDK 21 (required for the project)
- Maven 3.6.3 (required for the maven-compiler-plugin:3.13.0)

## Testing Dependencies

The repository includes a `test-pom.xml` file that can be used to test dependency resolution and verify that JitPack artifacts are available:

```bash
# Test if dependencies can be resolved
mvn -f test-pom.xml dependency:resolve

# Test compilation with dependencies
mvn -f test-pom.xml compile
```