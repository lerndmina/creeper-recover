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

/**
 * WorldGuard integration utility class
 * Handles detection and interaction with WorldGuard plugin
 */
public class WorldGuardIntegration {

  private static boolean worldGuardEnabled = false;
  private static boolean initialized = false;
  private static Object creeperRecoverDisabledFlag = null;

  /**
   * Initialize WorldGuard integration
   * Should be called during plugin startup
   */
  public static void initialize() {
    if (initialized)
      return;

    try {
      Plugin worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
      worldGuardEnabled = worldGuardPlugin != null && worldGuardPlugin.isEnabled();

      if (worldGuardEnabled) {
        registerCustomFlag();
        Bukkit.getConsoleSender().sendMessage("§7[CreeperRecover] WorldGuard detected - Integration enabled!");
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
          .sendMessage("§7[CreeperRecover] Custom WorldGuard flag 'creeper-recover' registered successfully!");

    } catch (Exception e) {
      Bukkit.getConsoleSender().sendMessage("§c[CreeperRecover] Failed to register WorldGuard flag: " + e.getMessage());
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
   * @return true if recovery should be skipped, false if recovery should happen
   */
  public static boolean shouldSkipRecovery(Location location) {
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

      // Convert Bukkit world to WorldGuard world
      Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter");
      Object world = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class).invoke(null, location.getWorld());

      // Get region manager for this world
      Object regionManager = regionContainer.getClass()
          .getMethod("get", Class.forName("com.sk89q.worldedit.world.World"))
          .invoke(regionContainer, world);

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
        if ("ALLOW".equals(stateName)) {
          return false; // Explicitly enabled recovery
        } else if ("DENY".equals(stateName)) {
          return true; // Explicitly disabled recovery
        }
      }

      // Flag not set - check TNT flag
      return isTntExplicitlyAllowedInternal(location);

    } catch (Exception e) {
      // Log error but don't disable WorldGuard entirely
      Bukkit.getConsoleSender().sendMessage("§c[CreeperRecover] Error checking WorldGuard region: " + e.getMessage());
      return false; // Default to allowing recovery on error
    }
  }

  /**
   * Internal method to check if TNT is explicitly allowed in a non-global region
   * If TNT is explicitly set to ALLOW in a region (not __global__), skip recovery
   * 
   * @param location The location to check
   * @return true if TNT is explicitly allowed in a non-global region
   */
  private static boolean isTntExplicitlyAllowedInternal(Location location) {
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

      // Convert Bukkit world to WorldGuard world
      Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldguard.bukkit.BukkitAdapter");
      Object world = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class).invoke(null,
          location.getWorld());

      // Get region manager for this world
      Object regionManager = regionContainer.getClass()
          .getMethod("get", Class.forName("com.sk89q.worldedit.world.World"))
          .invoke(regionContainer, world);

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

      // Check if any non-global region explicitly sets TNT to ALLOW
      Class<?> protectedRegionClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");

      // Get regions from applicableRegions
      Object regionsIterable = applicableRegions.getClass().getMethod("getRegions").invoke(applicableRegions);

      for (Object region : (Iterable<?>) regionsIterable) {
        // Skip __global__ region
        String regionId = (String) protectedRegionClass.getMethod("getId").invoke(region);
        if ("__global__".equals(regionId)) {
          continue;
        }

        // Check if this region has TNT flag set
        Object flagValue = protectedRegionClass.getMethod("getFlag",
            Class.forName("com.sk89q.worldguard.protection.flags.Flag"))
            .invoke(region, tntFlag);

        // If TNT flag is explicitly set to ALLOW in this region
        if (flagValue != null && "ALLOW".equals(flagValue.toString())) {
          return true; // TNT is explicitly allowed in a non-global region
        }
      }

      return false; // TNT not explicitly allowed in any non-global region

    } catch (Exception e) {
      // Log error but don't disable WorldGuard entirely
      Bukkit.getConsoleSender()
          .sendMessage("§c[CreeperRecover] Error checking WorldGuard TNT flag: " + e.getMessage());
      return false; // Default to allowing recovery on error
    }
  }

}