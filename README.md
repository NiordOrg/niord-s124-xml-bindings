# Project Overview

This repository contains XML bindings for S-100 and S-124 for use in Niord and Baleen.


# Release Process

This document describes the release process for the Niord S-124 XML Bindings project.

## Prerequisites

- Write access to the repository
- Maven 3.6.0 or higher
- Java 21

## Release Steps

### 1. Update Version Numbers

Before releasing, ensure all POMs have the correct version number:

- Parent POM: `/pom.xml`
- S-100 module: `/s-100/pom.xml`
- S-124 module: `/s-124/pom.xml`

All three should have matching versions (e.g., `0.0.1`).

### 2. Commit Changes

```bash
git add .
git commit -m "Prepare release v0.0.1"
git push origin main
```

### 3. Create and Push Tag

```bash
git tag v0.0.1
git push origin v0.0.1
```

The tag format must be `v<version>` (e.g., `v0.0.1`, `v1.0.0`).

### 4. Automatic Deployment

Once the tag is pushed, GitHub Actions will automatically:

1. Build the project with Java 21
2. Run tests
3. Deploy artifacts to GitHub Packages

Monitor the build status in the Actions tab of the GitHub repository.

### 5. Verify Release

After successful deployment, verify the packages are available:

1. Go to the repository page: https://github.com/NiordOrg/niord-s124-xml-bindings
2. Click on "Packages" in the right sidebar
3. Verify both artifacts are published:
   - `s100-5_2_0-xml-bindings`
   - `s124-2_0_1-xml-bindings`

## Using Released Packages

To use the released packages in other projects, add the following to your `pom.xml`:

### Repository Configuration

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/NiordOrg/niord-s124-xml-bindings</url>
    </repository>
</repositories>
```

### Dependencies

```xml
<dependencies>
    <!-- S-100 XML Bindings -->
    <dependency>
        <groupId>dma.dk.niord.s100.xml-bindings</groupId>
        <artifactId>s100-5_2_0-xml-bindings</artifactId>
        <version>0.0.1</version>
    </dependency>
    
    <!-- S-124 XML Bindings -->
    <dependency>
        <groupId>dma.dk.niord.s100.xml-bindings</groupId>
        <artifactId>s124-2_0_1-xml-bindings</artifactId>
        <version>0.0.1</version>
    </dependency>
</dependencies>
```

### Authentication

To download packages from GitHub Packages, you need to authenticate. Add to your `~/.m2/settings.xml`:

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