package dev.radworks.diagnostics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.radworks.RadWorks;
import dev.radworks.gameplay.RadiationGameplayService;
import dev.radworks.radiation.ExposureBreakdown;
import dev.radworks.radiation.ExposureEngine;
import dev.radworks.radiation.RadiationRules;
import dev.radworks.radiation.RadiationRulesLoader;
import dev.radworks.radiation.effects.EffectStrategyService;
import dev.radworks.radiation.shielding.ShieldingDiagnostics;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

public final class DiagnosticsService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withLocale(Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    private DiagnosticsService() {
    }

    public static String versionText() {
        return String.join(System.lineSeparator(),
                RadWorks.MOD_NAME + " " + modVersion(RadWorks.MOD_ID),
                "Minecraft: " + minecraftVersion(),
                "NeoForge: " + modVersion("neoforge"),
                "Java: " + System.getProperty("java.version", "UNKNOWN"),
                "Integrations: create=" + loadedState("create") + ", aeronautics=" + loadedState("aeronautics"),
                "Rules: " + rulesText());
    }

    public static Path writeDump(CommandSourceStack source) throws IOException {
        return PerformanceStats.timeValue("dump", () -> {
            try {
                return writeDumpTimed(source);
            } catch (IOException exception) {
                throw new DumpWriteException(exception);
            }
        });
    }

    private static Path writeDumpTimed(CommandSourceStack source) throws IOException {
        Instant createdAt = Instant.now();
        ServerPlayer player = source.getEntity() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        ServerLevel level = player != null ? player.serverLevel() : source.getLevel();
        String suffix = player == null ? "server" : sanitize(player.getGameProfile().getName());

        Path dumpDir = Path.of("radworks_dumps");
        Files.createDirectories(dumpDir);

        Path dumpPath = uniqueDumpPath(dumpDir, createdAt, suffix);
        Files.writeString(dumpPath, GSON.toJson(createDump(source, level, player, createdAt)), StandardCharsets.UTF_8);
        return dumpPath.toAbsolutePath().normalize();
    }

    private static JsonObject createDump(CommandSourceStack source, ServerLevel level, ServerPlayer player, Instant createdAt) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("createdAt", createdAt.toString());
        root.add("mod", modInfo());
        root.add("world", worldInfo(source, level));
        root.add("player", player == null ? JsonNull.INSTANCE : playerInfo(player));
        root.add("rules", rulesInfo());
        root.add("shielding", shieldingInfo());
        root.add("effectStrategy", effectStrategyInfo());
        root.add("gameplay", gameplayInfo());
        root.add("debug", DiagnosticsState.toJson());
        root.add("integrations", integrationsInfo());
        root.add("performance", performanceInfo());
        root.add("lastExposureSnapshot", lastExposureSnapshotInfo());
        root.add("sourceScanSummary", SourceScanSummary.lastToJson());
        root.add("handlerDiagnostics", HandlerDiagnostics.lastToJson());
        root.add("recentWarnings", WarningBuffer.toJson());
        return root;
    }

    private static JsonObject modInfo() {
        JsonObject mod = new JsonObject();
        mod.addProperty("id", RadWorks.MOD_ID);
        mod.addProperty("version", modVersion(RadWorks.MOD_ID));
        mod.addProperty("minecraftVersion", minecraftVersion());
        mod.addProperty("neoforgeVersion", modVersion("neoforge"));
        mod.addProperty("javaVersion", System.getProperty("java.version", "UNKNOWN"));
        return mod;
    }

    private static JsonObject worldInfo(CommandSourceStack source, ServerLevel level) {
        JsonObject world = new JsonObject();
        ResourceLocation dimension = level.dimension().location();
        world.addProperty("dimension", dimension.toString());
        world.addProperty("serverType", source.getServer().isDedicatedServer() ? "dedicated" : "integrated");
        world.addProperty("gameTime", level.getGameTime());
        return world;
    }

    private static JsonObject playerInfo(ServerPlayer player) {
        JsonObject playerInfo = new JsonObject();
        playerInfo.addProperty("name", player.getGameProfile().getName());
        playerInfo.addProperty("uuid", player.getUUID().toString());

        JsonObject position = new JsonObject();
        position.addProperty("x", player.getX());
        position.addProperty("y", player.getY());
        position.addProperty("z", player.getZ());
        playerInfo.add("position", position);

        return playerInfo;
    }

    private static JsonObject rulesInfo() {
        return RadiationRulesLoader.currentRules().toJson();
    }

    private static JsonObject shieldingInfo() {
        return ShieldingDiagnostics.toJson();
    }

    private static JsonObject effectStrategyInfo() {
        return EffectStrategyService.strategy().toJson();
    }

    private static JsonObject gameplayInfo() {
        return RadiationGameplayService.toJson();
    }

    private static JsonObject integrationsInfo() {
        JsonObject integrations = new JsonObject();
        integrations.add("create", integrationInfo("create", "Phase 0: not implemented"));
        integrations.add("aeronautics", integrationInfo("aeronautics", "Phase 0: not implemented"));
        return integrations;
    }

    private static JsonObject integrationInfo(String modId, String note) {
        JsonObject integration = new JsonObject();
        integration.addProperty("loaded", ModList.get().isLoaded(modId));
        integration.addProperty("enabled", false);

        JsonArray notes = new JsonArray();
        notes.add(note);
        integration.add("notes", notes);
        return integration;
    }

    private static JsonObject performanceInfo() {
        return PerformanceStats.toJson();
    }

    private static com.google.gson.JsonElement lastExposureSnapshotInfo() {
        ExposureBreakdown snapshot = ExposureEngine.lastExposureSnapshot();
        if (snapshot == null) {
            return JsonNull.INSTANCE;
        }
        return snapshot.toJson(20, RadiationRulesLoader.currentRules().checksum());
    }

    private static Path uniqueDumpPath(Path dumpDir, Instant createdAt, String suffix) {
        String baseName = "radworks-dump-" + FILE_TIMESTAMP.format(createdAt) + "-" + suffix;
        Path candidate = dumpDir.resolve(baseName + ".json");
        int counter = 2;
        while (Files.exists(candidate)) {
            candidate = dumpDir.resolve(baseName + "-" + counter + ".json");
            counter++;
        }
        return candidate;
    }

    private static String loadedState(String modId) {
        return ModList.get().isLoaded(modId) ? "loaded_disabled" : "absent";
    }

    private static String rulesText() {
        RadiationRules rules = RadiationRulesLoader.currentRules();
        if (!rules.loaded()) {
            return "not loaded";
        }
        return "loaded, checksum=" + rules.shortChecksum() + ", mode=" + RadiationRules.VALIDATION_MODE;
    }

    private static String modVersion(String modId) {
        return ModList.get()
                .getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("UNKNOWN");
    }

    private static String minecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static final class DumpWriteException extends RuntimeException {
        private DumpWriteException(IOException cause) {
            super(cause);
        }
    }
}
