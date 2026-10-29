package moe.kfn.emotestudio;

import com.google.gson.*;
import dev.kosmx.playerAnim.core.util.Easing;

import java.util.*;

public final class Emote {
    public static final List<String> PARTS = List.of("head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg", "rightItem", "leftItem", "body");
    public static final String DEFAULT_EASE = "LINEAR";
    public static final String CUSTOM = "CUSTOM";
    static final String CURVES = "mirrorcoreCurves";

    public enum Group {
        ROTATION("Rotation", "pitch", "yaw", "roll"), POSITION("Position", "x", "y", "z"),
        SCALE("Scale", "scaleX", "scaleY", "scaleZ"), BEND("Bend", "bend", "axis");
        public final String title;
        public final List<String> channels;
        Group(String title, String... channels) { this.title = title; this.channels = List.of(channels); }
    }

    public static List<Group> groups(String part) {
        return part.equals("head") || part.endsWith("Item") ? List.of(Group.ROTATION, Group.POSITION, Group.SCALE) : List.of(Group.values());
    }
    static boolean angle(String channel) {
        return switch (channel) { case "pitch", "yaw", "roll", "bend", "axis" -> true; default -> false; };
    }
    public static float restValue(String part, String channel) {
        return channel.equals("axis") && part.endsWith("Arm") ? 180 : defaultValue(part, channel);
    }
    public static float defaultValue(String part, String channel) {
        if (channel.startsWith("scale")) return 1;
        float[] p = switch (part) {
            case "rightArm" -> new float[]{-5, 2, 0};
            case "leftArm" -> new float[]{5, 2, 0};
            case "rightLeg" -> new float[]{-1.9F, 12, .1F};
            case "leftLeg" -> new float[]{1.9F, 12, .1F};
            default -> new float[]{0, 0, 0};
        };
        return switch (channel) { case "x" -> p[0]; case "y" -> p[1]; case "z" -> p[2]; default -> 0; };
    }

    public static final class Key {
        public int tick;
        public float value;
        public String ease;
        public int turn;
        public Float arg;
        public float inT = -2, inV, outT = 2, outV;
        public Key(int tick, float value, String ease, int turn, Float arg) { this.tick = tick; this.value = value; this.ease = ease; this.turn = turn; this.arg = arg; }
        public boolean custom() { return CUSTOM.equals(ease); }
    }

    public String name = "untitled", description = "", author = "";
    public int begin, end = 20, stop = 20, returnTick;
    public boolean loop, easeBefore;
    final JsonObject extra = new JsonObject(), emoteExtra = new JsonObject();
    final Map<String, JsonElement> texts = new HashMap<>();
    final Map<String, Map<String, List<Key>>> keys = new LinkedHashMap<>();

    public static Emote parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("emote")) throw new IllegalArgumentException("not an Emotecraft emote (no \"emote\" block)");
        Emote e = new Emote();
        int version = root.has("version") ? root.get("version").getAsInt() : 1;
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            switch (entry.getKey()) {
                case "emote", "version" -> {}
                case "name", "description", "author" -> {
                    String text = text(entry.getValue());
                    if (!entry.getValue().isJsonPrimitive()) e.texts.put(entry.getKey(), entry.getValue());
                    switch (entry.getKey()) { case "name" -> e.name = text; case "description" -> e.description = text; default -> e.author = text; }
                }
                default -> e.extra.add(entry.getKey(), entry.getValue());
            }
        }
        JsonObject node = root.getAsJsonObject("emote");
        boolean degrees = !node.has("degrees") || node.get("degrees").getAsBoolean();
        for (Map.Entry<String, JsonElement> entry : node.entrySet()) {
            JsonElement v = entry.getValue();
            switch (entry.getKey()) {
                case "beginTick" -> e.begin = v.getAsInt();
                case "endTick" -> e.end = v.getAsInt();
                case "stopTick" -> e.stop = v.getAsInt();
                case "isLoop" -> e.loop = v.getAsBoolean();
                case "returnTick" -> e.returnTick = v.getAsInt();
                case "easeBeforeKeyframe" -> e.easeBefore = v.getAsBoolean();
                case "degrees", "moves" -> {}
                default -> e.emoteExtra.add(entry.getKey(), v);
            }
        }
        if (!node.has("stopTick")) e.stop = e.end;
        for (JsonElement m : node.getAsJsonArray("moves")) {
            JsonObject move = m.getAsJsonObject();
            int tick = move.get("tick").getAsInt();
            String ease = move.has("easing") ? move.get("easing").getAsString() : DEFAULT_EASE;
            int turn = move.has("turn") ? move.get("turn").getAsInt() : 0;
            Float arg = move.has("easingArg") ? move.get("easingArg").getAsFloat() : null;
            for (Map.Entry<String, JsonElement> p : move.entrySet()) {
                if (!p.getValue().isJsonObject()) continue;
                String part = version < 3 && p.getKey().equals("torso") ? "body" : p.getKey();
                for (Map.Entry<String, JsonElement> c : p.getValue().getAsJsonObject().entrySet()) {
                    float value = c.getValue().getAsFloat();
                    if (!degrees && angle(c.getKey())) value = (float) Math.toDegrees(value);
                    e.put(part, c.getKey(), new Key(tick, value, ease, turn, arg));
                }
            }
        }
        e.readCurves();
        e.clamp();
        return e;
    }
    private void readCurves() {
        JsonElement curves = emoteExtra.remove(CURVES);
        if (curves == null || !curves.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> part : curves.getAsJsonObject().entrySet()) {
            if (!part.getValue().isJsonObject()) continue;
            for (Map.Entry<String, JsonElement> channel : part.getValue().getAsJsonObject().entrySet()) {
                if (!channel.getValue().isJsonArray()) continue;
                for (JsonElement row : channel.getValue().getAsJsonArray()) {
                    JsonArray r = row.getAsJsonArray();
                    Key a = key(part.getKey(), channel.getKey(), r.get(0).getAsInt());
                    int next = r.get(1).getAsInt();
                    Key b = next < 0 ? null : key(part.getKey(), channel.getKey(), next);
                    if (a == null || next >= 0 && b == null) continue;
                    a.ease = CUSTOM; a.outT = r.get(2).getAsFloat(); a.outV = r.get(3).getAsFloat();
                    if (b == null) continue;
                    b.inT = r.get(4).getAsFloat(); b.inV = r.get(5).getAsFloat();
                    keys.get(part.getKey()).get(channel.getKey()).removeIf(k -> k.tick > a.tick && k.tick < b.tick);
                }
            }
        }
    }
    static String text(JsonElement e) { return e.isJsonPrimitive() ? e.getAsString() : e.toString(); }

    public String toJson() {
        clamp();
        JsonObject root = new JsonObject();
        root.addProperty("version", 3);
        root.add("name", textJson("name", name));
        root.add("description", textJson("description", description));
        if (!author.isEmpty() || texts.containsKey("author")) root.add("author", textJson("author", author));
        extra.entrySet().forEach(x -> root.add(x.getKey(), x.getValue()));
        root.addProperty("editor", "emotestudio");
        JsonObject node = new JsonObject();
        node.addProperty("beginTick", begin);
        node.addProperty("endTick", end);
        node.addProperty("stopTick", stop);
        node.addProperty("isLoop", loop);
        node.addProperty("returnTick", returnTick);
        node.addProperty("degrees", true);
        if (easeBefore) node.addProperty("easeBeforeKeyframe", true);
        emoteExtra.entrySet().forEach(x -> node.add(x.getKey(), x.getValue()));
        Map<String, JsonObject> moves = new TreeMap<>();
        JsonObject curves = new JsonObject();
        keys.forEach((part, channels) -> channels.forEach((channel, list) -> {
            List<Key> out = new ArrayList<>();
            JsonArray rows = new JsonArray();
            for (int i = 0; i < list.size(); i++) {
                Key a = list.get(i);
                if (!a.custom()) { out.add(a); continue; }
                Key b = i + 1 < list.size() ? list.get(i + 1) : null;
                JsonArray row = new JsonArray();
                for (float v : new float[]{a.tick, b == null ? -1 : b.tick, a.outT, a.outV, b == null ? 0 : b.inT, b == null ? 0 : b.inV}) row.add(v);
                rows.add(row);
                out.add(new Key(a.tick, a.value, DEFAULT_EASE, a.turn, null));
                if (b != null) for (int t = a.tick + 1; t < b.tick; t++)
                    out.add(new Key(t, (float) bezier(a, b, t), DEFAULT_EASE, 0, null));
            }
            if (!rows.isEmpty()) {
                if (!curves.has(part)) curves.add(part, new JsonObject());
                curves.getAsJsonObject(part).add(channel, rows);
            }
            for (Key k : out) {
                JsonObject move = moves.computeIfAbsent(String.format(Locale.ROOT, "%08d|%s|%d|%s", k.tick, k.ease, k.turn, k.arg), id -> {
                    JsonObject o = new JsonObject();
                    o.addProperty("tick", k.tick);
                    o.addProperty("easing", k.ease);
                    if (k.turn != 0) o.addProperty("turn", k.turn);
                    if (k.arg != null) o.addProperty("easingArg", k.arg);
                    return o;
                });
                if (!move.has(part)) move.add(part, new JsonObject());
                move.getAsJsonObject(part).addProperty(channel, k.value);
            }
        }));
        if (!curves.isEmpty()) node.add(CURVES, curves);
        JsonArray array = new JsonArray();
        moves.values().forEach(array::add);
        node.add("moves", array);
        root.add("emote", node);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }
    JsonElement textJson(String field, String text) {
        JsonElement original = texts.get(field);
        return original != null && text(original).equals(text) ? original : new JsonPrimitive(text);
    }
    void clamp() {
        end = Math.max(1, end);
        begin = Math.clamp(begin, 0, end);
        stop = Math.max(stop, end);
        returnTick = Math.clamp(returnTick, 0, end);
    }
    public int length() { return Math.max(end, stop); }

    public Emote copyAsNew() {
        Emote copy = parse(toJson());
        copy.extra.remove("uuid");
        return copy;
    }


    public List<Key> keys(String part, String channel) { return keys.getOrDefault(part, Map.of()).getOrDefault(channel, List.of()); }
    public boolean animated(String part) { return keys.containsKey(part); }
    Key key(String part, String channel, int tick) {
        for (Key k : keys(part, channel)) if (k.tick == tick) return k;
        return null;
    }
    void put(String part, String channel, Key key) {
        List<Key> list = keys.computeIfAbsent(part, p -> new LinkedHashMap<>()).computeIfAbsent(channel, c -> new ArrayList<>());
        list.removeIf(k -> k.tick == key.tick);
        list.add(key);
        list.sort(Comparator.comparingInt(k -> k.tick));
    }
    void remove(String part, String channel, int tick) {
        Map<String, List<Key>> channels = keys.get(part);
        if (channels == null) return;
        List<Key> list = channels.get(channel);
        if (list != null && list.removeIf(k -> k.tick == tick) && list.isEmpty()) channels.remove(channel);
        if (channels.isEmpty()) keys.remove(part);
    }

    public SortedSet<Integer> ticks(String part, Group group) {
        SortedSet<Integer> ticks = new TreeSet<>();
        for (String c : group.channels) for (Key k : keys(part, c)) ticks.add(k.tick);
        return ticks;
    }
    public boolean has(String part, Group group, int tick) {
        for (String c : group.channels) if (key(part, c, tick) != null) return true;
        return false;
    }
    public void keyGroup(String part, Group group, int tick, float[] values) {
        String ease = ease(part, group, tick);
        for (int i = 0; i < group.channels.size(); i++) {
            String c = group.channels.get(i);
            Key k = key(part, c, tick);
            if (k != null) { k.value = values[i]; k.turn = 0; }
            else {
                float v = keys(part, c).isEmpty() && values[i] == defaultValue(part, c) ? restValue(part, c) : values[i];
                anchor(part, c, tick); put(part, c, new Key(tick, v, ease, 0, null));
            }
        }
    }
    public float[] values(String part, Group group, double time) {
        float[] v = new float[group.channels.size()];
        for (int i = 0; i < v.length; i++) v[i] = value(part, group.channels.get(i), time);
        return v;
    }
    public void set(String part, Group group, String channel, int tick, float value) {
        float[] v = values(part, group, tick);
        v[group.channels.indexOf(channel)] = value;
        keyGroup(part, group, tick, v);
    }
    public void retime(String part, String channel, Key k) {
        List<Key> list = keys.getOrDefault(part, Map.of()).get(channel);
        if (list == null) return;
        list.removeIf(o -> o != k && o.tick == k.tick);
        list.sort(Comparator.comparingInt(o -> o.tick));
    }
    public Key keyChannel(String part, String channel, int tick) {
        Key k = key(part, channel, tick);
        if (k == null) {
            float value = keys(part, channel).isEmpty() ? restValue(part, channel) : value(part, channel, tick);
            anchor(part, channel, tick);
            put(part, channel, k = new Key(tick, value, DEFAULT_EASE, 0, null));
        }
        return k;
    }
    private void anchor(String part, String channel, int tick) {
        List<Key> list = keys(part, channel);
        if (tick > begin && (list.isEmpty() || list.getFirst().tick > tick))
            put(part, channel, new Key(begin, restValue(part, channel), DEFAULT_EASE, 0, null));
    }
    public void removeKey(String part, String channel, int tick) { remove(part, channel, tick); }
    public void removeGroup(String part, Group group, int tick) { for (String c : group.channels) remove(part, c, tick); }
    public void moveGroup(String part, Group group, int from, int to) {
        if (from == to) return;
        for (String c : group.channels) {
            Key k = key(part, c, from);
            if (k == null) continue;
            remove(part, c, from);
            k.tick = to;
            put(part, c, k);
        }
    }
    public String ease(String part, Group group, int tick) {
        for (String c : group.channels) { Key k = key(part, c, tick); if (k != null) return k.ease; }
        return DEFAULT_EASE;
    }
    public void setEase(String part, Group group, int tick, String ease) {
        for (String c : group.channels) { Key k = key(part, c, tick); if (k != null) k.ease = ease; }
    }


    private record Frame(int tick, float value, String ease, Float arg) {}

    public float value(String part, String channel, double time) {
        List<Key> list = keys(part, channel);
        float def = defaultValue(part, channel);
        if (list.isEmpty()) return def;
        int tick = (int) Math.floor(time);
        int pos = -1;
        for (int i = 0; i < list.size(); i++) if (list.get(i).tick <= tick) pos = i;
        Frame before, after;
        if (pos == -1) before = new Frame(tick < begin ? 0 : tick < end ? begin : end, def, "INOUTSINE", null);
        else {
            Key k = list.get(pos);
            before = !loop && tick >= end && pos == list.size() - 1 && k.tick < end
                    ? new Frame(end, k.value, k.ease, k.arg) : new Frame(k.tick, value(channel, k), k.ease, k.arg);
        }
        if (pos + 1 < list.size()) { Key k = list.get(pos + 1); after = new Frame(k.tick, value(channel, k), k.ease, k.arg); }
        else if (loop) { Key k = list.getLast(); after = new Frame(end + 1, value(channel, k), k.ease, k.arg); }
        else if (tick < end) { Key k = list.getLast(); after = new Frame(end, value(channel, k), k.ease, k.arg); }
        else after = new Frame(stop, def, "INOUTSINE", null);
        if (pos >= 0 && pos + 1 < list.size() && list.get(pos).custom()) return (float) bezier(list.get(pos), list.get(pos + 1), time);
        int tb = before.tick, ta = after.tick;
        if (tb >= ta) { if (tick < tb) tb -= end - returnTick + 1; else ta += end - returnTick + 1; }
        if (tb == ta) return before.value;
        float f = (float) ((time - tb) / (ta - tb));
        Frame from = easeBefore ? after : before;
        return before.value + (after.value - before.value) * Easing.easeFromString(from.ease).invoke(f, from.arg);
    }
    static double bezier(Key a, Key b, double t) {
        double span = b.tick - a.tick;
        double x1 = a.tick + Math.clamp(a.outT, 0, span), x2 = b.tick + Math.clamp(b.inT, -span, 0);
        double y1 = a.value + a.outV, y2 = b.value + b.inV;
        double lo = 0, hi = 1, u = .5;
        for (int i = 0; i < 32; i++) {
            u = (lo + hi) / 2;
            if (cubic(a.tick, x1, x2, b.tick, u) < t) lo = u; else hi = u;
        }
        return cubic(a.value, y1, y2, b.value, u);
    }
    private static double cubic(double p0, double p1, double p2, double p3, double u) {
        double r = 1 - u;
        return r * r * r * p0 + 3 * r * r * u * p1 + 3 * r * u * u * p2 + u * u * u * p3;
    }
    private static float value(String channel, Key k) { return k.turn != 0 && angle(channel) ? k.value + k.turn * 360F : k.value; }

}
