package dev.visual.branding;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Setzt Fenstertitel ("Visual Client &lt;version&gt;") und Fenster-Icon (V-Logo)
 * des Minecraft-Fensters. Nutzt nur stabile LWJGL/GLFW- und Fabric-Loader-APIs,
 * daher versionsunabhängig (keine gemappten Minecraft-Aufrufe).
 */
public class VisualBranding implements ClientModInitializer {
    private static final int[] SIZES = {16, 32, 48};
    private String title = null;
    private boolean iconDone = false;
    // Manche Setups (Launcher hat den Fokus, Fenstermanager wie Seelen UI)
    // starten das Minecraft-Fenster MINIMIERT — es lädt fertig, taucht aber nie
    // auf, was wie "startet nicht" wirkt. Die ersten Ticks holen es daher aktiv
    // nach vorne. Danach nicht mehr, damit der Spieler selbst minimieren kann.
    private int foregroundTicks = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            long handle = GLFW.glfwGetCurrentContext();
            if (handle == 0L) return; // Fenster/Context noch nicht bereit
            if (foregroundTicks < 60) {
                foregroundTicks++;
                try {
                    if (GLFW.glfwGetWindowAttrib(handle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE) {
                        // Fenster ist minimiert gestartet -> nach vorne holen
                        GLFW.glfwRestoreWindow(handle);
                        GLFW.glfwShowWindow(handle);
                        GLFW.glfwFocusWindow(handle);
                    } else {
                        // schon sichtbar -> nicht weiter eingreifen (Fokus nicht klauen)
                        foregroundTicks = 60;
                    }
                } catch (Throwable ignored) {
                    foregroundTicks = 60;
                }
            }
            if (title == null) {
                String mc = FabricLoader.getInstance()
                        .getModContainer("minecraft")
                        .map(c -> c.getMetadata().getVersion().getFriendlyString())
                        .orElse("");
                title = "Visual Client " + mc;
            }
            // Titel jeden Tick setzen — Minecraft überschreibt ihn sonst selbst
            try {
                GLFW.glfwSetWindowTitle(handle, title);
            } catch (Throwable ignored) {
            }
            if (!iconDone) {
                iconDone = true;
                setIcon(handle);
            }
        });
    }

    private void setIcon(long handle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer icons = GLFWImage.malloc(SIZES.length, stack);
            ByteBuffer[] loaded = new ByteBuffer[SIZES.length];
            int n = 0;
            for (int size : SIZES) {
                ByteBuffer png = readResource("/assets/visualbranding/icon_" + size + ".png");
                if (png == null) continue;
                int[] w = new int[1];
                int[] h = new int[1];
                int[] ch = new int[1];
                ByteBuffer img = STBImage.stbi_load_from_memory(png, w, h, ch, 4);
                MemoryUtil.memFree(png);
                if (img == null) continue;
                icons.position(n).width(w[0]).height(h[0]).pixels(img);
                loaded[n] = img;
                n++;
            }
            if (n > 0) {
                icons.position(0).limit(n);
                GLFW.glfwSetWindowIcon(handle, icons);
            }
            for (ByteBuffer b : loaded) {
                if (b != null) STBImage.stbi_image_free(b);
            }
        } catch (Throwable ignored) {
        }
    }

    private ByteBuffer readResource(String path) {
        try (InputStream in = VisualBranding.class.getResourceAsStream(path)) {
            if (in == null) return null;
            byte[] bytes = in.readAllBytes();
            ByteBuffer buf = MemoryUtil.memAlloc(bytes.length);
            buf.put(bytes).flip();
            return buf;
        } catch (Exception e) {
            return null;
        }
    }
}
