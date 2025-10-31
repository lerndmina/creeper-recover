# Debug Logging Features - CreeperRecover 1.19.4

This document describes the debug logging functionality added to CreeperRecover for testing and troubleshooting purposes.

## Configuration

Debug logging is controlled by a new configuration option in `config.json`:

```json
{
  "configVersion": 2,
  "plugin": {
    "enabled": true,
    "bStats": true,
    "ignoreUpdates": false,
    "debugEnabled": true
  },
  "recover": {
    // ... other settings
  }
}
```

### Configuration Options

- **`debugEnabled`**: `true` to enable debug messages, `false` to disable
- **Default**: `true` (enabled by default as requested)
- **Location**: Under the `plugin` section in `config.json`

## Debug Message Recipients

When debug logging is enabled, debug messages are sent to:

1. **Console** - All debug messages appear in the server console
2. **Operators** - Players with OP status receive debug messages in chat
3. **Permission holders** - Players with `creeperrecover.debug` permission receive debug messages

## Debug Message Format

Debug messages follow this format:

```
➜ CreeperRecover ● [DEBUG] <message>
```

## What Gets Logged

### 1. Explosion Detection

When an explosion occurs:

```
[DEBUG] Explosion detected: Entity=CREEPER, Location=[world,100,64,200], Blocks=15
```

### 2. Block Filtering

When blocks are filtered by the blacklist:

```
[DEBUG] Filtered 3 blacklisted blocks, 12 blocks remaining
```

### 3. Recovery Process Start

When recovery begins:

```
[DEBUG] Started recovery process for 12 blocks
```

### 4. Individual Block Recovery

For each block that gets recovered:

```
[DEBUG] Recovering block: STONE at world,100,64,200
```

### 5. Batch Recovery Progress

When multiple blocks are recovered in a batch:

```
[DEBUG] Recovered 5 blocks from explosion at world,100,64,200
```

### 6. Recovery Completion

When an explosion's recovery is finished:

```
[DEBUG] Explosion at world,100,64,200 completed recovery
```

### 7. Manual Recovery Commands

When players use `/recover fix` commands:

```
[DEBUG] Manual recovery command executed: recovering 10 blocks by PlayerName
[DEBUG] Manual recovery command executed: recovering ALL blocks by PlayerName
```

## Commands

### New Debug Command

```
/recover debug
```

- **Permission**: `creeperrecover.debug`
- **Function**: Shows current debug status (enabled/disabled)
- **Output**: `Debug mode is currently ENABLED/DISABLED. Use config file to change.`

### Updated Help

The `/recover` help now includes the debug command (only visible to users with `creeperrecover.debug` permission):

```
/recover fix [blocks/all]
/recover reload
/recover stats
/recover debug
```

## Permissions

### New Permission

- **`creeperrecover.debug`**: Required to see debug messages and use `/recover debug` command

### Existing Permissions

- **`creeper.recover.command`**: Required for all `/recover` commands (existing)

## Use Cases

### Server Testing

Enable debug logging to:

- Verify explosions are being detected properly
- Check if the right number of blocks are being recovered
- Monitor recovery timing and performance
- Ensure blacklisted blocks are being filtered correctly

### Troubleshooting

Debug messages help identify:

- Why certain explosions aren't being recovered
- If block recovery is working as expected
- Performance issues with large explosions
- Configuration problems

### Development

Debug logging provides insight into:

- Plugin workflow and timing
- Block processing efficiency
- Recovery queue management

## Performance Notes

- Debug messages are only processed when `debugEnabled` is `true`
- Messages are sent asynchronously where possible
- Minimal performance impact when disabled
- When enabled, there may be slight chat spam during large explosions

## Example Debug Session

Here's what a typical debug session looks like:

```
➜ CreeperRecover ● [DEBUG] Explosion detected: Entity=CREEPER, Location=[world,156,63,242], Blocks=8
➜ CreeperRecover ● [DEBUG] Started recovery process for 8 blocks
➜ CreeperRecover ● [DEBUG] Recovering block: GRASS_BLOCK at world,156,63,242
➜ CreeperRecover ● [DEBUG] Recovering block: DIRT at world,157,63,242
➜ CreeperRecover ● [DEBUG] Recovering block: STONE at world,156,62,242
➜ CreeperRecover ● [DEBUG] Recovered 3 blocks from explosion at world,156,63,242
➜ CreeperRecover ● [DEBUG] Recovering block: DIRT at world,158,63,242
➜ CreeperRecover ● [DEBUG] Recovered 1 blocks from explosion at world,156,63,242
➜ CreeperRecover ● [DEBUG] Explosion at world,156,63,242 completed recovery
```

## Enabling/Disabling Debug Mode

1. **Edit Configuration**: Modify `plugins/CreeperRecover/config.json`
2. **Change debugEnabled**: Set to `true` or `false`
3. **Reload**: Use `/recover reload` to apply changes
4. **Check Status**: Use `/recover debug` to verify current state

## Server Administration Tips

### For Testing

- Enable debug logging temporarily during testing
- Use `/recover debug` to check status
- Monitor console for detailed recovery information

### For Production

- Disable debug logging in production environments
- Only enable when troubleshooting specific issues
- Consider performance impact on busy servers

This debug system provides comprehensive insight into CreeperRecover's operation while maintaining performance when disabled.
