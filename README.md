# Requester Golems

Requester Golems extends vanilla Copper Golems with requester-based logistics. Convert a Copper Golem into a Requester Golem, configure requests in a Requester Chest, and optionally assign owned golems to specific chests.

## Version 1.1.0

- Minecraft: **26.3**
- Mod loader: **Fabric**
- Java: **25 or newer**
- Required dependency: **Fabric API**

## Installation

1. Install Fabric Loader for Minecraft 26.3 and make sure Java 25 or newer is available.
2. Install the matching Fabric API build.
3. Download the `requestergolems-1.1.0.jar` file from the project's GitHub Releases page.
4. Place the mod JAR in your Minecraft instance's `mods` folder and launch the Fabric profile.

## V1.1 features

- Give Requester Golems persistent public IDs and custom names.
- Claim unowned legacy Requester Golems.
- Keep golems global or assign them to specific Requester Chests.
- Assign multiple golems to a chest, and assign a golem to multiple chests.
- Manage assignments inside the existing Requester Chest interface.
- Inspect a golem's identity and assignment status.

## Known limitation

ItemStack component-bearing items may not match source chest contents correctly. This is a known limitation and is deferred to a future release.

## License

CC0-1.0. See [LICENSE](LICENSE).
