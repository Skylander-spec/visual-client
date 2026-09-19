package dev.visual.visualsmod;

import com.mojang.blaze3d.platform.InputConstants;
import dev.visual.visualsmod.config.VisualsConfig;
import dev.visual.visualsmod.cosmetics.CapeLayer;
import dev.visual.visualsmod.effects.CombatEffects;
import dev.visual.visualsmod.gui.SettingsScreen;
import dev.visual.visualsmod.hud.CpsTracker;
import dev.visual.visualsmod.hud.HudRenderer;
import dev.visual.visualsmod.hud.SidebarRenderer;
import dev.visual.visualsmod.hud.TabRenderer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(VisualsMod.MODID)
public class VisualsMod {
    public static final String MODID = "visualsmod";

    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.visualsmod.settings",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_RSHIFT,
            "key.categories.visualsmod");

    public VisualsMod() {
        VisualsConfig.load();
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onRegisterKeys);
        modBus.addListener(this::onAddLayers);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new HudRenderer());
        MinecraftForge.EVENT_BUS.register(new SidebarRenderer());
        MinecraftForge.EVENT_BUS.register(new TabRenderer());
        MinecraftForge.EVENT_BUS.register(new CombatEffects());
        MinecraftForge.EVENT_BUS.register(new CpsTracker());
    }

    private void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
    }

    private void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new CapeLayer(renderer));
            }
        }
    }

    private boolean titleSet = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!titleSet && mc.getWindow() != null) {
            mc.getWindow().setTitle("Visual Client "
                    + net.minecraft.SharedConstants.getCurrentVersion().getName());
            titleSet = true;
        }
        while (OPEN_SETTINGS.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new SettingsScreen());
            }
        }
    }
}
