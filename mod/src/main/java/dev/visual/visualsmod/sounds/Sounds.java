package dev.visual.visualsmod.sounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Spielt Sounds aus dem Visual Pack (Namespace "visualpack") ab.
 * Die Sounds sind im Resource Pack definiert — keine Registry nötig,
 * der SoundManager löst über sounds.json auf.
 */
public final class Sounds {
    public static final ResourceLocation HIT = new ResourceLocation("visualpack", "hit");
    public static final ResourceLocation KILL = new ResourceLocation("visualpack", "kill");
    public static final ResourceLocation UI_CLICK = new ResourceLocation("visualpack", "ui_click");

    private Sounds() {
    }

    public static void playUi(ResourceLocation id, float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(id), pitch));
    }
}
