# Let's See My Mods

Fabric server/common mod that adds `/mods`.

The command prints installed content mods in chat, similar to Bukkit's
`/plugins`, while hiding Minecraft, Java, Fabric Loader, Fabric API modules,
and common API/library-looking mod ids.

## Targets

- `fabric-1.19.x`: builds against Minecraft `1.19.4`, Java 17
- `fabric-1.20.x`: builds against Minecraft `1.20.6`, Java 21
- `fabric-1.21.x`: builds against Minecraft `1.21.11`, Java 21
- `fabric-26.1.x`: builds against Minecraft `26.1.2`, Java 25

The `26.1.x` target covers Minecraft's newer versioning scheme. If you meant a
literal `1.26.x`, update `fabric-26.1.x/gradle.properties` when that version
exists in Fabric.

## Build

```powershell
.\gradlew.bat :fabric-1.19.x:build
.\gradlew.bat :fabric-1.20.x:build
.\gradlew.bat :fabric-1.21.x:build
.\gradlew.bat :fabric-26.1.x:build
```

The `fabric-1.19.x` build works with JDK 17 or newer. The `fabric-1.20.x` and
`fabric-1.21.x` builds work with JDK 21 or newer. The `fabric-26.1.x` build
must run with JDK 25.

The jars are written to:

- `fabric-1.19.x/build/libs/letsseemymods-fabric-1.19.x-1.0.0.jar`
- `fabric-1.20.x/build/libs/letsseemymods-fabric-1.20.x-1.0.0.jar`
- `fabric-1.21.x/build/libs/letsseemymods-fabric-1.21.x-1.0.0.jar`
- `fabric-26.1.x/build/libs/letsseemymods-fabric-26.1.x-1.0.0.jar`

Install the matching jar together with Fabric API for your Minecraft version.

## Config

On first launch the mod writes:

```text
config/letsseemymods.json
```

Use that file to add hidden mod ids, prefixes, or suffixes if a library/API mod
still appears in `/mods`.
