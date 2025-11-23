/*
 * Copyright (c) 2022-2023. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *         notice, this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *     * Neither the name of the developer nor the names of its contributors
 *         may be used to endorse or promote products derived from this software
 *         without specific prior written permission.
 *     * Redistributions in source or binary form must keep the original package
 *         and class name.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package de.rafael.plugins.creeper.recover.common.integration;

//------------------------------
//
// This class was developed by Rafael K.
// On 10/31/2025 at 12:00 PM
// In the project CreeperRecover
//
//------------------------------

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import de.rafael.plugins.creeper.recover.common.CreeperPlugin;

/**
 * WorldGuard integration utility class
 * Handles detection and interaction with WorldGuard plugin
 */
public class WorldGuardIntegration {

  private static boolean worldGuardEnabled = false;
  private static boolean initialized = false;
  private static boolean flagsRegistered = false;
  private static Object creeperRecoverDisabledFlag = null;

  /**
   * Register WorldGuard flags during onLoad phase
   * This MUST be called during onLoad() before the flag registry locks
   */
  public static void registerFlags() {
    if (flagsRegistered)
      return;

    try {
      // Try to register the flag even if we can't detect WorldGuard yet
      // WorldGuard loads its flag registry during onLoad phase
      registerCustomFlag();
      flagsRegistered = true;
    } catch (Exception e) {
      // Check if it's ClassNotFoundException (WorldGuard not installed)
      if (e.getCause() instanceof ClassNotFoundException || e instanceof ClassNotFoundException) {
        Bukkit.getConsoleSender()
            .sendMessage("§7[CreeperRecover] WorldGuard classes not found - Running without region protection");
      } else {
        Bukkit.getConsoleSender().sendMessage(
            "§c[CreeperRecover] Error registering WorldGuard flag: " + e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace();
      }
      flagsRegistered = false;
    }
  }

  /**
   * Initialize WorldGuard integration during onEnable phase
   * Should be called during plugin startup after all plugins are loaded
   */
  public static void initialize() {
    if (initialized)
      return;

    try {
      Plugin worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
      worldGuardEnabled = worldGuardPlugin != null && worldGuardPlugin.isEnabled() && flagsRegistered;

      if (worldGuardEnabled) {
        Bukkit.getConsoleSender().sendMessage("§7[CreeperRecover] WorldGuard detected - Integration enabled!");
      } else if (!flagsRegistered) {
        Bukkit.getConsoleSender()
            .sendMessage("§7[CreeperRecover] WorldGuard flag registration failed - Running without region protection");
      } else {
        Bukkit.getConsoleSender()
            .sendMessage("§7[CreeperRecover] WorldGuard not found - Running without region protection");
      }
    } catch (Exception e) {
      worldGuardEnabled = false;
      Bukkit.getConsoleSender().sendMessage("§c[CreeperRecover] Error checking WorldGuard: " + e.getMessage());
    }

    initialized = true;
  }

  /**
   * Register the custom WorldGuard flag using reflection for safety
   */
  private static void registerCustomFlag() {
    try {
      // Use reflection to avoid compile-time dependencies on WorldGuard classes
      Class<?> flagRegistryClass = Class.forName("com.sk89q.worldguard.WorldGuard");
      Object worldGuardInstance = flagRegistryClass.getMethod("getInstance").invoke(null);

      Object flagRegistry = flagRegistryClass.getMethod("getFlagRegistry").invoke(worldGuardInstance);

      Class<?> stateFlagClass = Class.forName("com.sk89q.worldguard.protection.flags.StateFlag");
      Object stateFlag = stateFlagClass.getConstructor(String.class, boolean.class)
          .newInstance("creeper-recover", false);

      creeperRecoverDisabledFlag = stateFlag;

      // Register the flag
      flagRegistry.getClass().getMethod("register", Class.forName("com.sk89q.worldguard.protection.flags.Flag"))
          .invoke(flagRegistry, stateFlag);

      Bukkit.getConsoleSender()
          .sendMessage("§7[CreeperRecover] WorldGuard flag 'creeper-recover' registered");

    } catch (Exception e) {
      Bukkit.getConsoleSender().sendMessage(
          "§c[CreeperRecover] Failed to register WorldGuard flag: " + e.getClass().getName() + ": " + e.getMessage());
      e.printStackTrace();
      // Don't disable WorldGuard entirely, just flag registration failed
    }
  }

  /**
   * Check if WorldGuard is available and enabled
   * 
   * @return true if WorldGuard integration is available
   */
  public static boolean isEnabled() {
    return worldGuardEnabled;
  }

  /**
   * Check if explosion recovery should be skipped at the given location
   * Logic:
   * - creeper-recover: allow → Always recover (overrides TNT)
   * - creeper-recover: deny → Never recover
   * - creeper-recover: unset → Check TNT flag (if TNT allow in non-global region,
   * skip recovery)
   * 
   * @param location The location to check
   * @param plugin   The plugin instance for config access
   * @return true if recovery should be skipped, false if recovery should happen
   */
  public static boolean shouldSkipRecovery(Location location, CreeperPlugin plugin) {
    if (!worldGuardEnabled || creeperRecoverDisabledFlag == null) {
      return false; // No WorldGuard or flag registration failed = no restriction
    }

    try {
      // Get WorldGuard platform
      Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
      Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
      Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuardInstance);

      // Get region container
      Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

      // Get region manager - try both APIs
      Object regionManager = null;
      
      try {
        // Try newer API (7.0.9+) - BukkitAdapter.adapt(World)
        Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter");
        Object world = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class).invoke(null, location.getWorld());
        regionManager = regionContainer.getClass()
            .getMethod("get", Class.forName("com.sk89q.worldedit.world.World"))
            .invoke(regionContainer, world);
      } catch (Exception e1) {
        // Fallback for older API (7.0.7) - use WorldGuardPlugin directly
        try {
          Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
          Class<?> wgPluginClass = wgPlugin.getClass();
          regionManager = wgPluginClass.getMethod("getRegionManager", org.bukkit.World.class)
              .invoke(wgPlugin, location.getWorld());
        } catch (Exception e2) {
          plugin.configManager().sendDebugMessage("Failed to get region manager: " + e2.getMessage());
          return false;
        }
      }

      if (regionManager == null) {
        return false; // No region manager for this world
      }

      // Convert location to WorldGuard location
      Class<?> vectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
      Object vector = vectorClass.getMethod("at", double.class, double.class, double.class)
          .invoke(null, location.getX(), location.getY(), location.getZ());

      // Get applicable regions
      Object applicableRegions = regionManager.getClass().getMethod("getApplicableRegions", vectorClass)
          .invoke(regionManager, vector);

      // Check creeper-recover flag value
      Object creeperRecoverFlagValue = applicableRegions.getClass()
          .getMethod("queryValue", Object.class, Class.forName("com.sk89q.worldguard.protection.flags.Flag"))
          .invoke(applicableRegions, null, creeperRecoverDisabledFlag);

      // If creeper-recover flag is set, respect it
      if (creeperRecoverFlagValue != null) {
        String stateName = creeperRecoverFlagValue.toString();
        plugin.configManager().sendDebugMessage("creeper-recover flag: " + stateName);
        if ("ALLOW".equals(stateName)) {
          plugin.configManager().sendDebugMessage("Recovery explicitly enabled by creeper-recover flag");
          return false; // Explicitly enabled recovery
        } else if ("DENY".equals(stateName)) {
          plugin.configManager().sendDebugMessage("Recovery explicitly disabled by creeper-recover flag");
          return true; // Explicitly disabled recovery
        }
      }

      // Flag not set - check TNT flag if enabled in config
      plugin.configManager().sendDebugMessage("creeper-recover flag not set, checking TNT flag (config: "
          + plugin.configManager().worldguardTntCheck() + ")");
      if (plugin.configManager().worldguardTntCheck()) {
        boolean tntAllowed = isTntExplicitlyAllowedInternal(location, plugin);
        plugin.configManager().sendDebugMessage("TNT explicitly allowed: " + tntAllowed);
        return tntAllowed;
      }
      return false; // TNT check disabled, allow recovery

    } catch (Exception e) {
      // Log error but don't disable WorldGuard entirely
      Bukkit.getConsoleSender()
          .sendMessage("§c[CreeperRecover] Error checking WorldGuard region: " + e.getClass().getName());
      if (e.getCause() != null) {
        Bukkit.getConsoleSender().sendMessage(
            "§c[CreeperRecover] Caused by: " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage());
      }
      e.printStackTrace();
      return false; // Default to allowing recovery on error
    }
  }

  /**
   * Internal method to check if TNT is explicitly allowed in a non-global region
   * If TNT is explicitly set to ALLOW in a region (not __global__), skip recovery
   * 
   * @param location The location to check
   * @param plugin   The plugin instance for debug logging
   * @return true if TNT is explicitly allowed in a non-global region
   */
  private static boolean isTntExplicitlyAllowedInternal(Location location, CreeperPlugin plugin) {
    if (!worldGuardEnabled) {
      return false; // No WorldGuard = no TNT flag restriction
    }

    try {
      // Get WorldGuard platform
      Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
      Object worldGuardInstance = worldGuardClass.getMethod("getInstance").invoke(null);
      Object platform = worldGuardClass.getMethod("getPlatform").invoke(worldGuardInstance);

      // Get region container
      Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

      // Get region manager - try both APIs
      Object regionManager = null;
      
      try {
        // Try newer API (7.0.9+) - BukkitAdapter.adapt(World)
        Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter");
        Object world = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class).invoke(null, location.getWorld());
        regionManager = regionContainer.getClass()
            .getMethod("get", Class.forName("com.sk89q.worldedit.world.World"))
            .invoke(regionContainer, world);
      } catch (Exception e1) {
        // Fallback for older API (7.0.7) - use WorldGuardPlugin directly
        try {
          Plugin wgPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
          Class<?> wgPluginClass = wgPlugin.getClass();
          regionManager = wgPluginClass.getMethod("getRegionManager", org.bukkit.World.class)
              .invoke(wgPlugin, location.getWorld());
        } catch (Exception e2) {
          plugin.configManager().sendDebugMessage("Failed to get region manager for TNT check: " + e2.getMessage());
          return false;
        }
      }

      if (regionManager == null) {
        return false; // No region manager for this world
      }

      // Convert location to WorldGuard location
      Class<?> vectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
      Object vector = vectorClass.getMethod("at", double.class, double.class, double.class)
          .invoke(null, location.getX(), location.getY(), location.getZ());

      // Get applicable regions
      Object applicableRegions = regionManager.getClass().getMethod("getApplicableRegions", vectorClass)
          .invoke(regionManager, vector);

      // Get the TNT flag from Flags class
      Class<?> flagsClass = Class.forName("com.sk89q.worldguard.protection.flags.Flags");
      Object tntFlag = flagsClass.getField("TNT").get(null);

      // Query the effective TNT flag value (respects region hierarchy and __global__)
      Object tntFlagValue = applicableRegions.getClass()
          .getMethod("queryValue", Object.class, Class.forName("com.sk89q.worldguard.protection.flags.Flag"))
          .invoke(applicableRegions, null, tntFlag);

      plugin.configManager().sendDebugMessage("TNT flag effective value: " + tntFlagValue);

      // Check if any non-global region explicitly overrides TNT to ALLOW
      // We need to check individual regions to see if a non-global region sets it
      Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
      Object regionsIterable = applicableRegions.getClass().getMethod("getRegions").invoke(applicableRegions);

      boolean hasNonGlobalRegion = false;
      boolean nonGlobalHasTntAllow = false;

      for (Object region : (Iterable<?>) regionsIterable) {
        String regionId = (String) protectedRegionClass.getMethod("getId").invoke(region);
        
        if ("__global__".equals(regionId)) {
          continue; // Skip global region
        }

        hasNonGlobalRegion = true;
        
        // Check if this specific region has TNT flag set (not inherited)
        Object flagValue = protectedRegionClass.getMethod("getFlag",
            Class.forName("com.sk89q.worldguard.protection.flags.Flag"))
            .invoke(region, tntFlag);

        plugin.configManager().sendDebugMessage("Region '" + regionId + "' TNT flag: " + flagValue);

        // If this non-global region explicitly sets TNT to ALLOW
        if (flagValue != null && "ALLOW".equals(flagValue.toString())) {
          plugin.configManager().sendDebugMessage("TNT explicitly allowed in non-global region: " + regionId);
          nonGlobalHasTntAllow = true;
          break;
        }
      }

      // If there's a non-global region with TNT explicitly set to ALLOW, skip recovery
      if (hasNonGlobalRegion && nonGlobalHasTntAllow) {
        return true;
      }

      return false; // No non-global region with explicit TNT ALLOW

    } catch (Exception e) {
      // Log error but don't disable WorldGuard entirely
      Bukkit.getConsoleSender()
          .sendMessage("§c[CreeperRecover] Error checking WorldGuard TNT flag: " + e.getMessage());
      return false; // Default to allowing recovery on error
    }
  }

}