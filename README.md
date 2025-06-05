# Niord S-124 XML Bindings

This repository contains JAXB XML bindings for S-100 and S-124 maritime standards for use in Niord and Baleen projects.

## Modules

- **s-100**: XML bindings for S-100 5.2.0 Exchange Catalogue
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

### 5. Automatic Deployment

Once the tag is pushed, GitHub Actions will automatically:

1. Build the project with Java 21
2. Run tests
3. Deploy artifacts to both:
   - GitHub Packages (requires authentication)
   - GitHub Pages Maven repository (publicly accessible)

Monitor the build status in the Actions tab of the GitHub repository.

### 6. Verify Release

After successful deployment, verify the packages are available:

1. Go to the repository page: https://github.com/NiordOrg/niord-s124-xml-bindings
2. Click on "Packages" in the right sidebar
3. Verify both artifacts are published:
   - `s100-5_2_0-xml-bindings`
   - `s124-2_0_1-xml-bindings`

## Using Released Packages

The packages are available from three sources:

### Option 1: JitPack (Recommended - No Authentication Required)

JitPack automatically builds and publishes artifacts from GitHub releases.

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

### Option 2: GitHub Pages (No Authentication Required)

Add the following to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>niord-s124-xml-bindings</id>
        <url>https://niordorg.github.io/niord-s124-xml-bindings/repository</url>
    </repository>
</repositories>

<dependencies>
    <!-- S-100 XML Bindings -->
    <dependency>
        <groupId>dma.dk.niord.s100.xml-bindings</groupId>
        <artifactId>s100-5_2_0-xml-bindings</artifactId>
        <version>0.0.5</version>
    </dependency>
    
    <!-- S-124 XML Bindings -->
    <dependency>
        <groupId>dma.dk.niord.s100.xml-bindings</groupId>
        <artifactId>s124-2_0_1-xml-bindings</artifactId>
        <version>0.0.5</version>
    </dependency>
</dependencies>
```

### Option 3: GitHub Packages (Authentication Required)

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/NiordOrg/niord-s124-xml-bindings</url>
    </repository>
</repositories>

<dependencies>
    <!-- S-100 XML Bindings -->
    <dependency>
        <groupId>dma.dk.niord.s100.xml-bindings</groupId>
        <artifactId>s100-5_2_0-xml-bindings</artifactId>
        <version>0.0.5</version>
    </dependency>
    
    <!-- S-124 XML Bindings -->
    <dependency>
        <groupId>dma.dk.niord.s100.xml-bindings</groupId>
        <artifactId>s124-2_0_1-xml-bindings</artifactId>
        <version>0.0.5</version>
    </dependency>
</dependencies>
```

For GitHub Packages, you need to authenticate. Add to your `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```

Create a GitHub Personal Access Token with `read:packages` scope at: https://github.com/settings/tokens

## Manual Deployment

If you need to deploy manually:

```bash
mvn clean deploy
```

Ensure your `~/.m2/settings.xml` contains GitHub authentication as described above.

## Troubleshooting

### Build Fails

- Check Java version: `java -version` (should be 21)
- Check Maven version: `mvn -version` (should be 3.6.0+)
- Ensure all XML schemas are present in `src/main/resources/xsd/`

### Deployment Fails

- Verify GitHub token has `write:packages` permission
- Check repository permissions
- Ensure version number doesn't already exist in GitHub Packages

### Package Not Found

- Verify the repository URL in consuming project
- Check authentication in `settings.xml`
- Ensure the package was successfully published

### JitPack Build Issues

JitPack requires specific configuration due to the complex JAXB generation:

- The project uses a `jitpack.yml` file to specify Maven 3.6.3 (required for the compiler plugin)
- JitPack builds may take several minutes due to JAXB code generation
- Monitor build status at: https://jitpack.io/#NiordOrg/niord-s124-xml-bindings

If JitPack builds fail:
1. Check the build log at: https://jitpack.io/com/github/NiordOrg/niord-s124-xml-bindings/[VERSION]/build.log
2. Verify the `jitpack.yml` configuration
3. Ensure all required XSD files are present in the repository

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
- Skips tests during build (faster builds, JAXB generation is the main concern)

## Testing Dependencies

The repository includes a `test-pom.xml` file that can be used to test dependency resolution and verify that JitPack artifacts are available:

```bash
# Test if dependencies can be resolved
mvn -f test-pom.xml dependency:resolve

# Test compilation with dependencies
mvn -f test-pom.xml compile
```

This test project:
- Uses the same JitPack repository configuration as end users
- Downloads and tests the latest artifacts
- Verifies that the dependency resolution works correctly
- Can be used to debug JitPack issues or test new releases

The `test-pom.xml` is particularly useful when:
- Debugging JitPack build issues
- Verifying that new releases work correctly
- Testing different dependency configurations
- Providing examples for users