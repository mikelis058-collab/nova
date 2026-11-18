package dev.halo;

import dev.halo.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class HaloClient implements ClientModInitializer {
    public static final String ID = "halo";

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        // Knife skins live in two optional built-in packs that the Knives module switches on/off.
        FabricLoader.getInstance().getModContainer(ID).ifPresent(c -> {
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of(ID, "knives_diamond"), c, ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of(ID, "knives_netherite"), c, ResourcePackActivationType.NORMAL);
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS, Identifier.of(ID, "hud"),
                (ctx, tickCounter) -> Hud.render(ctx));

        ClientTickEvents.END_CLIENT_TICK.register(c -> ModuleManager.tick());

        // Hats are registered by reflection so the build can exclude HatFeature.java if needed.
        try {
            Class.forName("dev.halo.render.HatFeature").getMethod("register").invoke(null);
        } catch (Throwable ignored) {
            System.out.println("[Halo] Hats renderer not loaded.");
        }
    }
}
