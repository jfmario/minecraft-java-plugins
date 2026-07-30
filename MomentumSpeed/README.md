# MomentumSpeed

Simple Spigot plugin that increases a player's walk speed while they are moving and resets it immediately when the player stops.

Configuration:
- To change sensitivity or strength, edit the constants in the Java source:
  - MOVE_THRESHOLD
  - ACCELERATION
  - MAX_MOMENTUM

Build:
- This repository builds each plugin with Maven; the provided POM follows the same pattern as SpearUpgradesCow.
- Run `mvn -f MomentumSpeed/pom.xml clean package` or rely on your repo CI/build scripts.

Install:
- Drop the shaded JAR from target/ into your server's plugins folder.
