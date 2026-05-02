## Cursor Cloud specific instructions

This is a **Minecraft Spigot/Paper plugin** (Java 21, Maven). There is no running application server, database, or frontend — only a JAR build.

### Build

```bash
cd SpearUpgradesCow && mvn -B clean package
```

Output JAR: `SpearUpgradesCow/target/SpearUpgradesCow-1.0.0.jar`

### Lint / Tests

- No dedicated linter or test suite exists. The Maven `compile` phase is the primary correctness check.
- The CI workflow (`.github/workflows/build-and-release.yml`) simply runs `mvn -B clean package` with JDK 21 Temurin.

### Runtime testing

End-to-end testing requires a Spigot/Paper 1.21.11 Minecraft server with the JAR loaded as a plugin and a Minecraft client — this is not feasible in a headless cloud VM. Verify correctness via a successful `mvn clean package` build.

### Key caveats

- The Spigot API dependency is `provided` scope (resolved from `hub.spigotmc.org` snapshots repo); initial builds download ~100 MB of dependencies.
- Maven is not pre-installed in the base VM image; the update script installs it via `apt-get`.
