package dev.visual.fabric.ui;

import dev.visual.fabric.Begleiter;
import dev.visual.fabric.VConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Full gallery with an ordinary Minecraft-button fallback when Compose is unavailable. */
public final class CompanionPicker extends VMouseScreen {
    public static Screen open() {
        if (VConfig.get().composeUi) try {
            return (Screen) Class.forName("dev.visual.fabric.ui.VCosmeticsScreen")
                    .getConstructor(boolean.class).newInstance(true);
        } catch (ReflectiveOperationException | LinkageError ignored) { }
        return new CompanionPicker();
    }
    private CompanionPicker() { super(Text.literal(VText.t("set.companion"))); }
    @Override protected void init() {
        super.init();
        for (int i=0; i<Begleiter.NAMEN.length; i++) {
            final int id=i;
            String name=net.minecraft.client.resource.language.I18n.translate(i==0 ? "vc.val.ownskin" : Begleiter.NAMEN[i]);
            addDrawableChild(new VButton(width/2-208+(i%3)*140, 30+(i/3)*27,136,23,
                    Text.literal(name), () -> { VConfig.get().miniMeArt=id; VConfig.get().miniMe=true; VConfig.save(); close(); }));
        }
        addDrawableChild(new VButton(width/2-60,height-30,120,22,Text.literal(VText.t("ui.done")),this::close));
    }
    @Override public void close() { if(client!=null) client.setScreen(VisualCosmeticsScreen.oeffnen()); }
}
