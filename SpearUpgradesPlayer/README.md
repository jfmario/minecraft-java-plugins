# SpearUpgradesPlayer

Spigot/Paper plugin: players get a custom spear that upgrades each time they kill another player with it. Tiers, spear material, vanilla Lunge, Sharpness, and bonus damage are configured in `src/main/resources/config.yml`.

## Requirements

- **Java 21** (matches `pom.xml` and current Minecraft server requirements)
- **Apache Maven** 3.x

## Build the plugin JAR

From this directory (`SpearUpgradesPlayer`):

```bash
mvn clean package
```

The plugin jar is written to:

`target/SpearUpgradesPlayer-1.0.0.jar`

Copy that file into your server's `plugins` folder and restart (or load the plugin with your server's plugin manager).
