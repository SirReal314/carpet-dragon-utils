package me.sirreal.carpetdragonutils;

import carpet.settings.Rule;

import static carpet.api.settings.RuleCategory.COMMAND;
import static carpet.api.settings.RuleCategory.SURVIVAL;

public class CarpetDragonUtilsSettings {
    @Rule(
            desc = "Simulates 5gt and gives location needed for stationary dragon fireball",
            extra = {
                    "Fix",
                    "this",
                    "later"
            },
            category = {COMMAND, SURVIVAL}
    )
    public static boolean dragonFireballUtils = false;
}
