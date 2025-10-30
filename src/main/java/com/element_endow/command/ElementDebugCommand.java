package com.element_endow.command;

import com.element_endow.api.ElementSystemAPI;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
                .then(Commands.literal("listcombinations")
                        .executes(ctx -> listCombinations(ctx.getSource()))));

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
                source.sendSuccess(() -> Component.literal("Active combinations: " + activeCombinations), true);
                return Command.SINGLE_SUCCESS;
            } catch (Exception e) {
                source.sendFailure(Component.literal("Error checking combinations: " + e.getMessage()));
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

}