package moe.kfn.emotestudio;

import moe.kfn.emotestudio.mcp.McpServer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
                .then(Commands.literal("mcp")
                        .executes(context -> {
                            context.getSource().sendSystemMessage(Component.literal("MCP server is " + (McpServer.running() ? "on: " + url() : "off")));
                            return 1;
                        })
                        .then(Commands.literal("enable").executes(context -> {
                            String error = McpServer.start();
                            context.getSource().sendSystemMessage(error == null ? Component.literal("MCP server on: " + url())
                                    : Component.literal("MCP server failed: " + error).withStyle(ChatFormatting.RED));
                            return error == null ? 1 : 0;
                        }))
                        .then(Commands.literal("disable").executes(context -> {
                            McpServer.stop();
                            context.getSource().sendSystemMessage(Component.literal("MCP server off"));
                            return 1;
                        }))));
    }

    private static String url() { return "http://127.0.0.1:" + McpServer.PORT + "/mcp"; }

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
