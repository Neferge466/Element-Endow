package com.element_endow.command;

import com.element_endow.api.ElementSystemAPI;
import com.element_endow.data.ElementDataService;
import com.element_endow.event.EntitySpawnHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

public class ElementDebugCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("elementdebug")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(ctx -> reloadData(ctx.getSource())))
                .then(Commands.literal("checkcombinations")
                        .executes(ctx -> checkCombinations(ctx.getSource())))
                .then(Commands.literal("forcecheck")
                        .executes(ctx -> forceCheck(ctx.getSource())))
                .then(Commands.literal("listcombinations")
                        .executes(ctx -> listCombinations(ctx.getSource())))
                .then(Commands.literal("datastats")
                        .executes(ctx -> showDataStats(ctx.getSource())))
                .then(Commands.literal("clearbindings")
                        .executes(ctx -> clearBindings(ctx.getSource())))
                .then(Commands.literal("systeminfo")
                        .executes(ctx -> systemInfo(ctx.getSource())));
    }

    private static int reloadData(CommandSourceStack source) {
        try {
            ElementSystemAPI.reloadAllData();
            source.sendSuccess(() -> Component.literal("Element system data reloaded"), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to reload: " + e.getMessage()));
            return 0;
        }
    }

    private static int checkCombinations(CommandSourceStack source) {
        if (source.getEntity() instanceof Player player) {
            try {
                var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
                combinationSystem.checkAndApplyCombinations(player);

                var activeCombinations = combinationSystem.getActiveCombinations(player);
                source.sendSuccess(() -> Component.literal("Active combinations: " + activeCombinations.size()), true);
                for (String combo : activeCombinations) {
                    source.sendSuccess(() -> Component.literal(" - " + combo), false);
                }
                return Command.SINGLE_SUCCESS;
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error checking combinations: " + e.getMessage()));
                return 0;
            }
        }
        source.sendFailure(Component.literal("Command must be executed by a player"));
        return 0;
    }

    private static int forceCheck(CommandSourceStack source) {
        if (source.getEntity() instanceof Player player) {
            try {
                var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
                // 使缓存失效以强制重新检查
                if (combinationSystem instanceof com.element_endow.core.ElementCombinationSystem impl) {
                    impl.invalidateEntityCache(player);
                }
                combinationSystem.checkAndApplyCombinations(player);

                source.sendSuccess(() -> Component.literal("Force checked combinations for player"), true);
                return Command.SINGLE_SUCCESS;
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error force checking: " + e.getMessage()));
                return 0;
            }
        }
        source.sendFailure(Component.literal("Command must be executed by a player"));
        return 0;
    }

    private static int listCombinations(CommandSourceStack source) {
        try {
            var combinationSystem = ElementSystemAPI.getElementSystem().getCombinationSystem();
            var combinations = combinationSystem.getCombinationLoader().getCombinations();

            source.sendSuccess(() -> Component.literal("Loaded combinations: " + combinations.size()), true);
            for (String comboId : combinations.keySet()) {
                source.sendSuccess(() -> Component.literal(" - " + comboId), false);
            }
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error listing combinations: " + e.getMessage()));
            return 0;
        }
    }

    private static int showDataStats(CommandSourceStack source) {
        try {
            ElementDataService dataService = ElementSystemAPI.getElementSystem().getDataService();
            if (dataService == null) {
                source.sendFailure(Component.literal("DataService not available"));
                return 0;
            }

            var stats = dataService.getDataStats();
            source.sendSuccess(() -> Component.literal("Data Service Statistics:"), true);
            source.sendSuccess(() -> Component.literal("Total data entries: " + stats.getTotalDataCount()), false);
            source.sendSuccess(() -> Component.literal("Total data sources: " + stats.getTotalSourceCount()), false);

            for (var entry : stats.dataCounts.entrySet()) {
                source.sendSuccess(() -> Component.literal(" - " + entry.getKey() + ": " + entry.getValue() + " entries"), false);
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error getting data stats: " + e.getMessage()));
            return 0;
        }
    }

    private static int clearBindings(CommandSourceStack source) {
        try {
            EntitySpawnHandler.clearAllConditionalBindings();
            source.sendSuccess(() -> Component.literal("Cleared all conditional bindings"), true);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error clearing bindings: " + e.getMessage()));
            return 0;
        }
    }

    private static int systemInfo(CommandSourceStack source) {
        try {
            var elementSystem = ElementSystemAPI.getElementSystem();

            source.sendSuccess(() -> Component.literal("Element System Information:"), true);
            source.sendSuccess(() -> Component.literal("Registered elements: " + elementSystem.getRegisteredElements().size()), false);
            source.sendSuccess(() -> Component.literal("Enabled elements: " + elementSystem.getEnabledElements().size()), false);

            if (elementSystem instanceof com.element_endow.core.ElementSystemImpl impl) {
                source.sendSuccess(() -> Component.literal("Disabled elements: " + impl.getDisabledElementCount()), false);
                source.sendSuccess(() -> Component.literal("DataService available: " + (impl.getDataService() != null)), false);
            }

            // 显示缓存统计
            var combinationSystem = elementSystem.getCombinationSystem();
            if (combinationSystem instanceof com.element_endow.core.ElementCombinationSystem combImpl) {
                //这可添加获取缓存统计的方法
                source.sendSuccess(() -> Component.literal("Combination system initialized with DataService"), false);
            }

            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error getting system info: " + e.getMessage()));
            return 0;
        }
    }
}