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

package de.rafael.plugins.creeper.recover.common.listener;

//------------------------------
//
// This class was developed by Rafael K.
// On 31.12.2021 at 12:37
// In the project CreeperRecover
//
//------------------------------

import de.rafael.plugins.creeper.recover.common.CreeperPlugin;
import de.rafael.plugins.creeper.recover.common.classes.Explosion;
import de.rafael.plugins.creeper.recover.common.classes.list.BlockList;
import de.rafael.plugins.creeper.recover.common.integration.WorldGuardIntegration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public class EntityExplodeListener implements Listener {

    @EventHandler
    public void on(EntityExplodeEvent event) {
        if (!CreeperPlugin.instance().configManager().enabled())
            return;

        // ALWAYS remove protected blocks from explosion - they are ALWAYS protected
        // regardless of WorldGuard
        int protectedRemoved = 0;
        var iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            var block = iterator.next();
            if (CreeperPlugin.instance().configManager().isProtectedBlock(block.getType())) {
                iterator.remove();
                protectedRemoved++;
            }
        }

        if (protectedRemoved > 0) {
            CreeperPlugin.instance().configManager().sendDebugMessage(String.format(
                    "Protected %d blocks from explosion destruction (always protected)", protectedRemoved));
        }

        // Check WorldGuard regions for recovery system (after protected blocks are
        // handled)
        if (CreeperPlugin.instance().configManager().worldguardIntegration() &&
                WorldGuardIntegration.isEnabled() &&
                WorldGuardIntegration.isRecoveryDisabled(event.getLocation())) {
            CreeperPlugin.instance().configManager().sendDebugMessage(String.format(
                    "Explosion recovery at %s skipped due to WorldGuard 'creeper-recover-disabled' flag",
                    event.getLocation().toString()));
            return;
        }

        if (CreeperPlugin.instance().configManager().usePlugin(event)) {
            var blocks = new BlockList(event.blockList());

            CreeperPlugin.instance().configManager().sendDebugMessage(String.format(
                    "Explosion detected: Entity=%s, Location=[%s], Blocks=%d",
                    event.getEntityType().name(),
                    event.getLocation().toString(),
                    blocks.blocks().size()));

            // Disable damage by explosion
            event.setYield(100);
            int originalBlockCount = blocks.blocks().size();

            // Remove blacklisted blocks from recovery (they get destroyed but won't be
            // restored)
            blocks.removeIf(
                    block -> CreeperPlugin.instance().configManager().blockBlacklist().contains(block.getType()));
            int filteredBlockCount = blocks.blocks().size();

            if (originalBlockCount != filteredBlockCount) {
                int blacklistedRemoved = originalBlockCount - filteredBlockCount;
                CreeperPlugin.instance().configManager().sendDebugMessage(String.format(
                        "Filtered %d blacklisted blocks, %d blocks remaining for recovery",
                        blacklistedRemoved,
                        filteredBlockCount));
            }

            // Store blocks
            CreeperPlugin.instance().explosionManager().handle(new Explosion(event.getLocation().clone(), blocks));

            CreeperPlugin.instance().configManager().sendDebugMessage(String.format(
                    "Started recovery process for %d blocks",
                    filteredBlockCount));

            // Remove all blocks without collision. To prevent redstone and other blocks
            // from being without support blocks.
            blocks.stream().filter(Block::isPassable).forEach(block -> block.setType(Material.AIR, false));
            // Remove the rest of the blocks
            blocks.stream().filter(block -> !block.isPassable()).forEach(block -> block.setType(Material.AIR, false));
        }
    }

}
