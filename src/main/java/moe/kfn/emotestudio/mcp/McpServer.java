package moe.kfn.emotestudio.mcp;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import moe.kfn.emotestudio.EmoteStudioScreen;
import moe.kfn.emotestudio.emotestudio;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public final class McpServer {
    public static final int PORT = Integer.getInteger("emotestudio.mcp.port", 23367);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final String INSTRUCTIONS = """
            Emote Studio: in-game editor for Emotecraft emotes (Minecraft player animations). The editor opens itself when a tool needs it.
            Time is in ticks (20 = 1 s). Parts: head, torso, rightArm, leftArm, rightLeg, leftLeg, rightItem, leftItem, body (whole player).
            Groups -> channels: rotation (pitch, yaw, roll; degrees), position (x, y, z; pixels, +x = left, +y = down, +z = back),
            scale (scaleX, scaleY, scaleZ; 1 = normal), bend (bend, axis; degrees; not on head or items).
            Values are absolute (rest pose: rightArm x=-5 y=2, leftArm x=5 y=2, rightLeg x=-1.9 y=12, leftLeg x=1.9 y=12, everything else 0, scale 1).
            A scene holds up to 3 characters; character 0 (P1) is the main file, 1 and 2 are saved as <file>_p2.json / _p3.json.
            Keyframe tools edit the character being edited (see get_state.editing_character; switch with edit_character).
            Edits are undoable and stay unsaved until `save`. Start with get_state; check your work with render_preview.""";
    private static final JsonArray TOOLS = new JsonArray();

    static {
        tool("list_emotes", "List emote JSON files in the emotes folder with their display names.");
        tool("get_state", "Editor state: file, dirty flag, playhead tick, emote settings, animated parts, characters (look + stand position), camera, available skins.");
        tool("open_emote", "Open an emote file from the emotes folder, or start a new blank emote when file is omitted.",
                "file:string:File name in the emotes folder, e.g. wave.json", "discard:boolean:Throw away unsaved changes");
        tool("save", "Save all characters. Without file it overwrites the current file; with a new file it saves as a new emote (new uuid).",
                "file:string:Target file name (Save As)", "overwrite:boolean:Allow replacing an existing other file");
        tool("get_emote_json", "Full Emotecraft JSON of one character (default: the one being edited).", "character:integer:0, 1 or 2");
        tool("set_emote_json", "Replace one character's emote with Emotecraft JSON (bulk edit). Its timing then applies to every character.",
                "json!:string:Emotecraft emote JSON", "character:integer:0, 1 or 2 (default: the one being edited)");
        tool("set_emote_settings", "Change emote info and timing (shared by all characters). Text fields accept & colour codes.",
                "name:string:Display name", "description:string:Description", "author:string:Author",
                "begin_tick:integer:Begin tick", "end_tick:integer:End tick", "stop_tick:integer:Stop tick (fade out ends)",
                "loop:boolean:Loop the emote", "return_tick:integer:Tick a loop returns to", "ease_before:boolean:Ease before keyframe",
                "hold:boolean:Hold the last frame (end = last keyframe, loop to end)");
        tool("get_keyframes", "Raw keyframes per part and channel (tick, value, ease, custom-curve handles).",
                "part:string:Only this part", "channel:string:Only this channel");
        tool("get_pose", "Evaluated (interpolated) channel values of every part at a tick.",
                "tick:number:Tick (default: playhead)", "part:string:Only this part");
        tool("set_keyframe", "Add or update a keyframe of one group (rotation / position / scale / bend). Channels not given keep their current interpolated value.",
                "part!:string:Part", "group!:string:rotation, position, scale or bend", "tick:integer:Tick (default: playhead)",
                "values:object:Channel values, e.g. {\"pitch\": -90, \"roll\": 10}", "ease:string:Ease into the next key, e.g. LINEAR, INOUTSINE, OUTBACK, CONSTANT, CUSTOM");
        tool("edit_channel_key", "Graph-editor edit of a single channel key (created if missing): value, ease, full turns, custom bezier handles, retime.",
                "part!:string:Part", "channel!:string:Channel, e.g. pitch", "tick:integer:Key tick (default: playhead)", "value:number:Value",
                "ease:string:Ease (CUSTOM = bezier curve to the next key)", "turn:integer:Extra full 360 turns",
                "out_t:number:Outgoing handle ticks (CUSTOM)", "out_v:number:Outgoing handle value offset (CUSTOM)",
                "in_t:number:Incoming handle ticks, negative", "in_v:number:Incoming handle value offset", "new_tick:integer:Move the key to this tick");
        tool("remove_keyframe", "Delete keys at a tick: one channel, one group, or (neither given) the whole part.",
                "part!:string:Part", "tick!:integer:Tick", "group:string:Only this group", "channel:string:Only this channel");
        tool("move_keyframes", "Move keys from one tick to another (overwrites keys already at the target).",
                "from_tick!:integer:Source tick", "to_tick!:integer:Target tick", "part:string:Only this part (default all)", "group:string:Only this group");
        tool("copy_keyframes", "Copy every key between from_tick and to_tick so the first lands on at_tick, optionally onto another part (mirror an arm etc.).",
                "from_tick!:integer:Range start", "to_tick:integer:Range end (default from_tick)", "at_tick!:integer:Paste start tick",
                "part:string:Only this part (default all)", "to_part:string:Paste onto this part instead (needs part)");
        tool("key_whole_part", "Key every group of a part at a tick with its current pose (K).", "part!:string:Part", "tick:integer:Tick (default: playhead)");
        tool("set_time", "Stop playback and move the playhead.", "tick!:number:Tick");
        tool("play", "Start or stop preview playback.", "playing:boolean:Default true", "speed:number:0.25 to 2", "loop:boolean:Loop playback in the preview");
        tool("select_part", "Select a part in the UI (inspector + timeline), optionally open the graph editor on a channel.",
                "part:string:Part, or none for emote settings", "bend:boolean:Select the lower (bent) half", "graph:boolean:Show the graph editor", "channel:string:Graph channel");
        tool("undo", "Undo edits.", "steps:integer:Default 1");
        tool("redo", "Redo edits.", "steps:integer:Default 1");
        tool("add_character", "Add a character to the scene (max 3), blank or imported from an emote file. It becomes the one being edited.",
                "file:string:Emote file to import");
        tool("remove_character", "Remove character 1 or 2 from the scene.", "character!:integer:1 or 2");
        tool("edit_character", "Choose which character the keyframe tools edit.", "character!:integer:0, 1 or 2");
        tool("set_character", "Character preview look (not saved) and stand position (saved).",
                "character:integer:0, 1 or 2 (default: the one being edited)", "hidden:boolean:Hide in preview", "skin:string:player, a default skin (steve, alex...) or a PNG in emotes/skins",
                "slim:boolean:Slim arms", "right_hand:string:SWORD, PAPER, BLOCK or NONE", "left_hand:string:SWORD, PAPER, BLOCK or NONE",
                "x:number:Blocks", "y:number:Blocks", "z:number:Blocks", "yaw:number:Facing degrees",
                "preset:string:carry_on or piggyback (place character 1/2 on character 0)");
        tool("set_camera", "Preview camera.", "reset:boolean:Reset view", "yaw:number:Camera yaw", "pitch:number:Camera pitch", "zoom:number:Zoom (default 5)",
                "frame:boolean:Show the picture frame", "focus:integer:Character to focus in pictures, -1 = all");
        tool("take_picture", "Save the emote picture (PNG next to the emote file) from the current camera.");
        tool("render_preview", "Render the preview from the current camera and return it as an image.", "tick:number:Move the playhead here first");
    }

    private static void tool(String name, String description, String... params) {
        JsonObject props = new JsonObject();
        JsonArray required = new JsonArray();
        for (String p : params) {
            String[] s = p.split(":", 3);
            String key = s[0].replace("!", "");
            if (s[0].endsWith("!")) required.add(key);
            JsonObject prop = new JsonObject();
            prop.addProperty("type", s[1]);
            prop.addProperty("description", s[2]);
            props.add(key, prop);
        }
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", props);
        if (!required.isEmpty()) schema.add("required", required);
        JsonObject tool = new JsonObject();
        tool.addProperty("name", name);
        tool.addProperty("description", description);
        tool.add("inputSchema", schema);
        TOOLS.add(tool);
    }

    private static HttpServer http;

    public static synchronized boolean running() { return http != null; }

    public static synchronized String start() {
        if (http != null) return null;
        String[] error = new String[1];
        Thread t = new Thread(() -> {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), PORT), 0);
                server.createContext("/mcp", McpServer::handle);
                server.start();
                http = server;
                emotestudio.LOGGER.info("Emote Studio MCP server on http://127.0.0.1:{}/mcp", PORT);
            } catch (IOException e) {
                error[0] = e.toString();
                emotestudio.LOGGER.warn("Emote Studio MCP server could not start on port {}: {}", PORT, e.toString());
            }
        }, "emotestudio-mcp");
        t.setDaemon(true);
        t.start();
        try { t.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return error[0];
    }

    public static synchronized void stop() {
        if (http == null) return;
        http.stop(0);
        http = null;
        emotestudio.LOGGER.info("Emote Studio MCP server stopped");
    }

    private static void handle(HttpExchange ex) throws IOException {
        try (ex) {
            String origin = ex.getRequestHeaders().getFirst("Origin");
            if (origin != null && !origin.matches("https?://(localhost|127\\.0\\.0\\.1|\\[::1])(:\\d+)?")) { ex.sendResponseHeaders(403, -1); return; }
            if (!ex.getRequestMethod().equals("POST")) { ex.getResponseHeaders().add("Allow", "POST"); ex.sendResponseHeaders(405, -1); return; }
            JsonObject msg;
            try { msg = JsonParser.parseString(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject(); }
            catch (RuntimeException e) { send(ex, error(JsonNull.INSTANCE, -32700, "Parse error")); return; }
            JsonElement id = msg.get("id");
            if (id == null) { ex.sendResponseHeaders(202, -1); return; }
            String method = msg.has("method") ? msg.get("method").getAsString() : "";
            JsonObject params = msg.get("params") instanceof JsonObject o ? o : new JsonObject();
            JsonObject result = new JsonObject();
            switch (method) {
                case "initialize" -> {
                    result.addProperty("protocolVersion", params.has("protocolVersion") ? params.get("protocolVersion").getAsString() : "2025-06-18");
                    JsonObject caps = new JsonObject();
                    caps.add("tools", new JsonObject());
                    result.add("capabilities", caps);
                    JsonObject info = new JsonObject();
                    info.addProperty("name", "emotestudio");
                    info.addProperty("version", "1.0.0");
                    result.add("serverInfo", info);
                    result.addProperty("instructions", INSTRUCTIONS);
                }
                case "ping" -> {}
                case "tools/list" -> result.add("tools", TOOLS);
                case "tools/call" -> result = call(params.get("name").getAsString(), params.get("arguments") instanceof JsonObject o ? o : new JsonObject());
                default -> { send(ex, error(id, -32601, "Method not found: " + method)); return; }
            }
            JsonObject out = new JsonObject();
            out.addProperty("jsonrpc", "2.0");
            out.add("id", id);
            out.add("result", result);
            send(ex, out);
        }
    }

    private static JsonObject call(String name, JsonObject args) {
        JsonObject content = new JsonObject();
        boolean failed = false;
        try {
            Object out = Minecraft.getInstance().submit(() -> {
                try { return EmoteStudioScreen.mcp(name, args); }
                catch (IOException e) { throw new java.io.UncheckedIOException(e); }
            }).get(30, TimeUnit.SECONDS);
            if (out instanceof byte[] png) {
                content.addProperty("type", "image");
                content.addProperty("data", Base64.getEncoder().encodeToString(png));
                content.addProperty("mimeType", "image/png");
            } else {
                content.addProperty("type", "text");
                content.addProperty("text", out instanceof JsonElement json ? GSON.toJson(json) : String.valueOf(out));
            }
        } catch (Exception e) {
            Throwable cause = e instanceof java.util.concurrent.ExecutionException && e.getCause() != null ? e.getCause() : e;
            if (cause instanceof java.io.UncheckedIOException u) cause = u.getCause();
            content.addProperty("type", "text");
            content.addProperty("text", cause.getClass().getSimpleName() + ": " + cause.getMessage());
            failed = true;
        }
        JsonArray list = new JsonArray();
        list.add(content);
        JsonObject result = new JsonObject();
        result.add("content", list);
        result.addProperty("isError", failed);
        return result;
    }

    private static JsonObject error(JsonElement id, int code, String message) {
        JsonObject err = new JsonObject();
        err.addProperty("code", code);
        err.addProperty("message", message);
        JsonObject out = new JsonObject();
        out.addProperty("jsonrpc", "2.0");
        out.add("id", id);
        out.add("error", err);
        return out;
    }

    private static void send(HttpExchange ex, JsonObject body) throws IOException {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
    }
}
