package me.sirreal.carpetdragonutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import me.sirreal.carpetdragonutils.utils.EnderDragon;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

public class DragonCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dragon")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(context -> simulate(context.getSource()))
        );
    }

    private static int simulate(ServerCommandSource source) {
        Vec3d playerPos = source.getPlayer().getCameraPosVec(1.0F);
        Vec3d playerLook = source.getPlayer().getRotationVec(1.0F).multiply(100); // Extend the look vector
        Box box = source.getPlayer().getBoundingBox().expand(playerLook.x, playerLook.y, playerLook.z);

        // Check if there is an Ender Dragon in the player's line of sight
        EnderDragonEntity enderDragon = (EnderDragonEntity) source.getWorld().getEntity(source.getWorld().getEnderDragonFight().getDragonUuid());

        if (enderDragon != null) {
            source.sendMessage(Text.literal("Dragon executed"));
            if (enderDragon.getPhaseManager().getCurrent().getType() != PhaseType.HOLDING_PATTERN || enderDragon.getMovementSpeed() != 0) {
                source.sendFeedback((Supplier<Text>) Text.of("Dragon not frozen in correct phase"), false);
                return 0;
            }
            EnderDragon dragon = new EnderDragon(enderDragon, source.getPlayer());
            for (int i = 0; i < 5; i++) {
                source.sendMessage(Text.literal(String.format("gt: %d", i+1)));
                source.sendMessage(Text.literal(String.format("PosZ: %.16G", dragon.getZ())));
                dragon.tickMovement();
            }
            // Perform actions on the dragon
            source.sendFeedback((Supplier<Text>) Text.of("You are looking at an Ender Dragon."), false);
            return 1;
        } else {
            source.sendFeedback((Supplier<Text>) Text.of("You are not looking at an Ender Dragon."), false);
            return 0;
        }
    }



}
