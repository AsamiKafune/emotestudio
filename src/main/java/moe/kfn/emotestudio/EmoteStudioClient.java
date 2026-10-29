package moe.kfn.emotestudio;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@EventBusSubscriber(modid = emotestudio.MODID, value = Dist.CLIENT)
public final class EmoteStudioClient {
    private EmoteStudioClient() {}

    @SubscribeEvent
    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("emotestudio")
                .executes(context -> open(null))
                .then(Commands.argument("file", StringArgumentType.greedyString())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(files(), builder))
                        .executes(context -> open(StringArgumentType.getString(context, "file")))));
    }

    private static int open(String file) {
        Minecraft.getInstance().tell(() -> EmoteStudioScreen.open(file));
        return 1;
    }

    static Path folder() { return Minecraft.getInstance().gameDirectory.toPath().resolve("emotes"); }

    static List<String> files() {
        try (Stream<Path> stream = Files.list(folder())) {
            return stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".json"))
                    .map(p -> p.getFileName().toString()).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    static String fileName(String raw) {
        String name = raw == null ? "" : raw.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        if (name.isEmpty() || name.chars().allMatch(c -> c == '.')) return null;
        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".json")) name += ".json";
        Path folder = folder().toAbsolutePath().normalize();
        return folder.resolve(name).normalize().getParent().equals(folder) ? name : null;
    }

    static Emote read(String file) throws IOException {
        String name = fileName(file);
        if (name == null) throw new IOException("bad file name: " + file);
        return Emote.parse(Files.readString(folder().resolve(name)));
    }

    static void write(String file, Emote emote) throws IOException {
        Files.createDirectories(folder());
        Files.writeString(folder().resolve(file), emote.toJson());
    }

    static boolean exists(String file) { return Files.exists(folder().resolve(file)); }
}
