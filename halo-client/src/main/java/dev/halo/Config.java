package dev.halo;

import com.google.gson.*;
import dev.halo.module.Module;
import dev.halo.module.ModuleManager;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private Config() {}
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("halo.json"); }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            for (Module m : ModuleManager.ALL) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("enabled")) { m.enabled = o.get("enabled").getAsBoolean(); m.anim = m.enabled ? 1f : 0f; }
                if (o.has("key")) m.key = o.get("key").getAsInt();
                JsonObject s = o.has("settings") ? o.getAsJsonObject("settings") : new JsonObject();
                for (Module.Setting st : m.allSettings()) {
                    try { if (s.has(st.name)) st.load(s.get(st.name)); } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("[Halo] Could not read config: " + e);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (Module m : ModuleManager.ALL) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.enabled);
            o.addProperty("key", m.key);
            JsonObject s = new JsonObject();
            for (Module.Setting st : m.allSettings()) s.add(st.name, st.save());
            o.add("settings", s);
            root.add(m.name, o);
        }
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            System.err.println("[Halo] Could not save config: " + e);
        }
    }
}
