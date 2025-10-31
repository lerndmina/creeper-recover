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

package de.rafael.plugins.creeper.recover.common.command;

//------------------------------
//
// This class was developed by Rafael K.
// On 2/22/2022 at 12:13 AM
// In the project CreeperRecover
//
//------------------------------

import de.rafael.plugins.creeper.recover.common.CreeperPlugin;
import de.rafael.plugins.creeper.recover.common.manager.MessageManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecoverCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            String[] args) {
        MessageManager messageManager = CreeperPlugin.instance().messageManager();
        if (!sender.hasPermission("creeper.recover.command")) {
            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                    + messageManager.getMessage(MessageManager.Message.NO_PERMISSION));
            return false;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("fix")) {
            try {
                CreeperPlugin.scheduler().runAsync(() -> {
                    if (args[1].equalsIgnoreCase("all")) {
                        CreeperPlugin.instance().configManager().sendDebugMessage(
                                "Manual recovery command executed: recovering ALL blocks by " + sender.getName());
                        int recovered = CreeperPlugin.instance().explosionManager().recoverBlocks(Integer.MAX_VALUE);
                        sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                                + messageManager.getMessage(MessageManager.Message.BLOCKS_RECOVERED, recovered));
                    } else {
                        int amount = Integer.parseInt(args[1]);
                        CreeperPlugin.instance().configManager().sendDebugMessage(
                                "Manual recovery command executed: recovering " + amount + " blocks by "
                                        + sender.getName());
                        int recovered = CreeperPlugin.instance().explosionManager().recoverBlocks(amount);
                        sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                                + messageManager.getMessage(MessageManager.Message.BLOCKS_RECOVERED, recovered));
                    }
                });
            } catch (NumberFormatException exception) {
                sender.sendMessage(
                        messageManager.getMessage(MessageManager.Message.PREFIX) + "§c" + exception.getMessage());
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            CreeperPlugin.instance().configManager().load();
            CreeperPlugin.instance().messageManager().load();
            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                    + messageManager.getMessage(MessageManager.Message.RELOADED));
        } else if (args.length == 1 && args[0].equalsIgnoreCase("stats")) {
            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                    + messageManager.getMessage(MessageManager.Message.STATS_TITLE));
            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                    + messageManager.getMessage(MessageManager.Message.STATS_LINE_BLOCKS,
                            CreeperPlugin.instance().pluginStats().blocksRecovered()));
            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                    + messageManager.getMessage(MessageManager.Message.STATS_LINE_EXPLOSIONS,
                            CreeperPlugin.instance().pluginStats().explosionsRecovered()));
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("protected")) {
            if (sender.hasPermission("creeper.recover.admin")) {
                handleProtectedCommand(sender, args);
            } else {
                sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                        + messageManager.getMessage(MessageManager.Message.NO_PERMISSION));
            }
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("debug")) {
            if (sender.hasPermission("creeper.recover.debug")) {
                boolean currentStatus = CreeperPlugin.instance().configManager().debugEnabled();

                if (args.length == 1) {
                    // Show current status
                    sender.sendMessage(
                            messageManager.getMessage(MessageManager.Message.PREFIX) + "§7Debug mode is currently §"
                                    + (currentStatus ? "aENABLED" : "cDISABLED") + "§7.");
                } else if (args.length == 2) {
                    if (args[1].equalsIgnoreCase("enable")) {
                        if (!currentStatus) {
                            CreeperPlugin.instance().configManager().setDebugEnabled(true);
                            CreeperPlugin.instance().configManager().saveConfig();
                            sender.sendMessage(
                                    messageManager.getMessage(MessageManager.Message.PREFIX) + "§aDebug mode enabled!");
                        } else {
                            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                                    + "§7Debug mode is already enabled.");
                        }
                    } else if (args[1].equalsIgnoreCase("disable")) {
                        if (currentStatus) {
                            CreeperPlugin.instance().configManager().setDebugEnabled(false);
                            CreeperPlugin.instance().configManager().saveConfig();
                            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                                    + "§cDebug mode disabled!");
                        } else {
                            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                                    + "§7Debug mode is already disabled.");
                        }
                    } else {
                        sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                                + "§cUsage: /recover debug [enable|disable]");
                    }
                } else {
                    sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                            + "§cUsage: /recover debug [enable|disable]");
                }
            } else {
                sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX)
                        + messageManager.getMessage(MessageManager.Message.NO_PERMISSION));
            }
        } else {
            showHelp(sender);
        }
        return false;
    }

    public void showHelp(@NotNull CommandSender sender) {
        MessageManager messageManager = CreeperPlugin.instance().messageManager();
        sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                messageManager.getMessage(MessageManager.Message.HELP_LINE_1));
        sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                messageManager.getMessage(MessageManager.Message.HELP_LINE_2));
        sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                messageManager.getMessage(MessageManager.Message.HELP_LINE_3));
        if (sender.hasPermission("creeper.recover.debug")) {
            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                    messageManager.getMessage(MessageManager.Message.HELP_LINE_4));
        }
        if (sender.hasPermission("creeper.recover.admin")) {
            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                    "§7/recover protected <list|add|remove> [material] - Manage protected blocks");
        }
    }

    public void handleProtectedCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        MessageManager messageManager = CreeperPlugin.instance().messageManager();

        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("list"))) {
            // List protected blocks
            var protectedBlocks = CreeperPlugin.instance().configManager().getProtectedBlocks();
            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                    "§7Protected blocks (" + protectedBlocks.size() + "):");
            if (protectedBlocks.isEmpty()) {
                sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) + "§7  None");
            } else {
                for (Material material : protectedBlocks) {
                    sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                            "§7  - §a" + material.name());
                }
            }
        } else if (args.length == 3 && args[1].equalsIgnoreCase("add")) {
            // Add protected block
            try {
                Material material = Material.valueOf(args[2].toUpperCase());
                if (CreeperPlugin.instance().configManager().addProtectedBlock(material)) {
                    // Handle special case: if PLAYER_HEAD is added, also add PLAYER_WALL_HEAD
                    if (material == Material.PLAYER_HEAD) {
                        CreeperPlugin.instance().configManager().addProtectedBlock(Material.PLAYER_WALL_HEAD);
                    }
                    CreeperPlugin.instance().configManager().saveConfig();
                    sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                            "§aAdded §b" + material.name() + "§a to protected blocks list!");
                    if (material == Material.PLAYER_HEAD) {
                        sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                                "§7Also added PLAYER_WALL_HEAD automatically.");
                    }
                } else {
                    sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                            "§c" + material.name() + " is already in the protected blocks list!");
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                        "§cInvalid material: " + args[2]);
            }
        } else if (args.length == 3 && args[1].equalsIgnoreCase("remove")) {
            // Remove protected block
            try {
                Material material = Material.valueOf(args[2].toUpperCase());
                if (CreeperPlugin.instance().configManager().removeProtectedBlock(material)) {
                    // Handle special case: if PLAYER_HEAD is removed, also remove PLAYER_WALL_HEAD
                    if (material == Material.PLAYER_HEAD) {
                        CreeperPlugin.instance().configManager().removeProtectedBlock(Material.PLAYER_WALL_HEAD);
                    }
                    CreeperPlugin.instance().configManager().saveConfig();
                    sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                            "§cRemoved §b" + material.name() + "§c from protected blocks list!");
                    if (material == Material.PLAYER_HEAD) {
                        sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                                "§7Also removed PLAYER_WALL_HEAD automatically.");
                    }
                } else {
                    sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                            "§c" + material.name() + " is not in the protected blocks list!");
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                        "§cInvalid material: " + args[2]);
            }
        } else {
            sender.sendMessage(messageManager.getMessage(MessageManager.Message.PREFIX) +
                    "§cUsage: /recover protected <list|add|remove> [material]");
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("fix", "reload", "stats"));
            if (sender.hasPermission("creeper.recover.debug")) {
                completions.add("debug");
            }
            if (sender.hasPermission("creeper.recover.admin")) {
                completions.add("protected");
            }
            return completions;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("fix")) {
            return Arrays.asList("all", "10", "100", "1000", "10000");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("debug")
                && sender.hasPermission("creeper.recover.debug")) {
            return Arrays.asList("enable", "disable");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("protected")
                && sender.hasPermission("creeper.recover.admin")) {
            return Arrays.asList("list", "add", "remove");
        } else if (args.length == 3 && args[0].equalsIgnoreCase("protected")
                && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))
                && sender.hasPermission("creeper.recover.admin")) {
            // Return some common block types for tab completion
            return Arrays.asList("PLAYER_HEAD", "ITEM_FRAME", "PAINTING", "ARMOR_STAND", "SIGN", "WALL_SIGN");
        } else {
            return new ArrayList<>();
        }
    }

}
