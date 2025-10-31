# Minecraft 1.19.4 Compatibility Changes

This document outlines the changes made to make CreeperRecover compatible with Minecraft 1.19.4.

## Changes Made

### Version Configuration

- Updated `spigot_version` and `folia_version` in `gradle.properties` from `1.21.4-R0.1-SNAPSHOT` to `1.19.4-R0.1-SNAPSHOT`
- Changed `api-version` in both `plugin.yml` and `paper-plugin.yml` from `1.20` to `1.19`
- Updated plugin version to `1.1.0-mc1.19.4` to indicate compatibility
- **Fixed Java Compatibility**: Set Java source and target compatibility to Java 17 (required for Minecraft 1.19.4)

### API Compatibility Fixes

#### Sign API Changes

- **Issue**: The multi-sided sign API (Side enum, getSide(), etc.) was introduced in Minecraft 1.20
- **Solution**: Modified sign handling to use backward-compatible deprecated methods for 1.19.4:
  - `SignLines` class now uses `sign.setLine()` instead of `sign.getSide().setLine()`
  - `SignStyle` class now uses `sign.setColor()` and `sign.setGlowingText()` instead of side-based methods
  - Added `@SuppressWarnings("deprecation")` to suppress expected deprecation warnings

#### Sign Waxing

- **Issue**: Sign waxing (`isWaxed()`, `setWaxed()`) was added in Minecraft 1.20
- **Solution**: Modified `SignData` to use reflection to call waxing methods if available, silently ignoring if not supported

#### EntityType.TNT

- **Issue**: `EntityType.TNT` constant name changed between versions
- **Solution**: Added fallback logic to try different TNT entity type names:
  1. First tries `EntityType.TNT`
  2. Falls back to `EntityType.PRIMED_TNT`
  3. Final fallback to `EntityType.TNT_MINECART`

#### Configuration

- Updated default entity types to use string "TNT" instead of `EntityType.TNT.name()` to avoid compilation issues

### Java Compatibility Fix

- **Issue**: Plugin was compiled with Java 21 (major version 65) but Minecraft 1.19.4 servers typically run on Java 17
- **Solution**: Configured all Gradle build files to compile with Java 17 compatibility:
  - Set `sourceCompatibility` and `targetCompatibility` to `JavaVersion.VERSION_17`
  - Added `options.release.set(17)` to compile tasks
  - Applied settings to all modules (common, spigot, folia)

## Compatibility Notes

### What Works

- ✅ Block recovery functionality
- ✅ Explosion handling
- ✅ Inventory preservation
- ✅ Basic sign recovery (text content)
- ✅ Sign color and glow effects (using deprecated API)
- ✅ Chest handling
- ✅ All core plugin features

### What's Limited in 1.19.4

- ❌ Sign waxing (not supported in 1.19.4)
- ⚠️ Multi-sided signs (1.19.4 only supports single-sided signs)

### Warnings

- Deprecation warnings are expected when compiling for 1.19.4, as the plugin uses some methods that were deprecated in favor of newer APIs in 1.20+
- These warnings are suppressed where appropriate and don't affect functionality

## Testing Recommendations

When testing this 1.19.4 version:

1. Test basic creeper explosion recovery
2. Verify sign text is preserved and restored
3. Test chest inventory recovery
4. Confirm TNT block handling works correctly
5. Test configuration loading and entity type filtering

## New Features Added

### Debug Logging System

- **Configuration**: `debugEnabled` option in config.json (enabled by default)
- **Command**: `/recover debug` to check debug status
- **Recipients**: Console, operators, and players with `creeperrecover.debug` permission
- **Logging**: Comprehensive debug messages for explosion detection, block filtering, recovery progress, and manual commands
- **Performance**: Minimal impact when disabled, helps with testing and troubleshooting

See `DEBUG_LOGGING_FEATURES.md` for complete documentation.

## Deployment

The built JAR files will be compatible with:

- Spigot 1.19.4
- Paper 1.19.4
- Folia 1.19.4 (Paper fork)

Use the appropriate JAR from the build output:

- `build/libs/creeper-recover-1.1.0-mc1.19.4-spigot.jar` for Spigot/Paper
- `build/libs/creeper-recover-1.1.0-mc1.19.4-folia.jar` for Folia
