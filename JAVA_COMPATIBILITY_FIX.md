# Java Compatibility Fix for CreeperRecover 1.19.4

## Problem Description

When trying to load the plugin on a Minecraft 1.19.4 server, you might encounter this error:

```
java.lang.IllegalArgumentException: Unsupported class file major version 65
```

This error means the plugin was compiled with a newer Java version than what your server is running.

## Java Version Mapping

- **Major Version 65** = Java 21
- **Major Version 61** = Java 17
- **Major Version 55** = Java 11

## Root Cause

- The plugin was originally compiled with Java 21
- Minecraft 1.19.4 servers typically run on Java 17
- Java cannot load classes compiled with a newer major version

## Solution

The fix involved updating all Gradle build files to target Java 17 compatibility:

### Changes Made:

1. **Main build.gradle.kts**: Added Java 17 compatibility to all projects
2. **Module build files**: Set Java 17 for common, spigot, and folia modules
3. **Compile tasks**: Added `options.release.set(17)` to ensure Java 17 bytecode

### Configuration Applied:

```kotlin
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    compileJava {
        options.release.set(17)
    }
}
```

## Server Requirements

### For Minecraft 1.19.4:

- **Minimum Java Version**: Java 17
- **Recommended**: OpenJDK 17 or Oracle JDK 17
- **Plugin Compatibility**: Now compiles to Java 17 bytecode

## Verification

After the fix, the plugin generates JAR files with:

- ✅ Java 17 compatibility (major version 61)
- ✅ Works on Java 17+ servers
- ✅ Compatible with Minecraft 1.19.4

## Prevention

To avoid this issue in future builds:

1. Always match Java version to Minecraft version requirements
2. Use `--release` compiler option to ensure bytecode compatibility
3. Test on target server environment before deployment

## If You Still Get Errors

1. **Check your server's Java version**: `java -version`
2. **Ensure Java 17+**: Upgrade if running Java 11 or older
3. **Use the correct JAR**:
   - `creeper-recover-1.1.0-mc1.19.4-spigot.jar` for Spigot/Paper
   - `creeper-recover-1.1.0-mc1.19.4-folia.jar` for Folia

## Additional Notes

- This fix maintains full functionality while ensuring compatibility
- No features were removed or changed
- The plugin still includes all debug logging functionality
- Performance remains the same
