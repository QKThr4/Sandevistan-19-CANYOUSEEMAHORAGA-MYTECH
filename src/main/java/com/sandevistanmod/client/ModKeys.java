package com.sandevistanmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ModKeys {
    private ModKeys() {}

    public static final String CATEGORY = "key.categories.sandevistan";

    public static final KeyMapping SCREEN = key("key.sandevistan.screen", GLFW.GLFW_KEY_J);
    public static final KeyMapping TOGGLE = key("key.sandevistan.toggle", GLFW.GLFW_KEY_V);

    /** Acao do olho implantado (Zoom: segurar; Tatico/Visao: liga e desliga; Laser: atira). */
    public static final KeyMapping EYE = key("key.sandevistan.eye", GLFW.GLFW_KEY_G);
    /** Scanner de Minerios. */
    public static final KeyMapping XRAY = key("key.sandevistan.xray", GLFW.GLFW_KEY_H);

    /** Segurar e rolar o mouse troca a habilidade selecionada. */
    public static final KeyMapping SELECT = key("key.sandevistan.select", GLFW.GLFW_KEY_LEFT_ALT);
    /** Usa a habilidade selecionada. */
    public static final KeyMapping USE = key("key.sandevistan.use", GLFW.GLFW_KEY_R);

    private static KeyMapping key(String name, int code) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, code, CATEGORY);
    }
}
