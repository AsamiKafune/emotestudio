package moe.kfn.emotestudio;

import com.lowdragmc.lowdraglib2.configurator.IConfigurable;
import com.lowdragmc.lowdraglib2.configurator.annotation.ConfigNumber;
import com.lowdragmc.lowdraglib2.configurator.ui.*;
import com.lowdragmc.lowdraglib2.editor.keymap.EditorAction;
import com.lowdragmc.lowdraglib2.gui.ui.event.CommandEvents;
import com.lowdragmc.lowdraglib2.editor.keymap.EditorActions;
import com.lowdragmc.lowdraglib2.editor.keymap.KeyChord;
import com.lowdragmc.lowdraglib2.editor.keymap.KeymapCategories;
import com.lowdragmc.lowdraglib2.editor.ui.Editor;
import com.lowdragmc.lowdraglib2.editor.ui.EditorWindow;
import com.lowdragmc.lowdraglib2.editor.ui.View;
import com.lowdragmc.lowdraglib2.editor.ui.ViewContainer;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.SceneEditor;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.ISceneRendering;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.SceneObject;
import com.lowdragmc.lowdraglib2.math.Ray;
import com.lowdragmc.lowdraglib2.editor.ui.sceneeditor.sceneobject.utils.TransformGizmo;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.*;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.utils.virtuallevel.TrackedDummyWorld;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.core.util.Easing;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyPosition;
import moe.kfn.emotestudio.Emote.Group;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.joml.Matrix4fStack;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class EmoteStudioScreen extends ModularUIScreen {
    private static final int LABEL_WIDTH = 170, ROW_HEIGHT = 18, ARENA = 8, GRAPH_HEIGHT = 170;
    private static final Map<String, Integer> CHANNEL_COLORS = Map.ofEntries(
            Map.entry("pitch", 0xFFE05555), Map.entry("yaw", 0xFF6BD06B), Map.entry("roll", 0xFF5A8CFF),
            Map.entry("x", 0xFFE05555), Map.entry("y", 0xFF6BD06B), Map.entry("z", 0xFF5A8CFF),
            Map.entry("scaleX", 0xFFE05555), Map.entry("scaleY", 0xFF6BD06B), Map.entry("scaleZ", 0xFF5A8CFF),
            Map.entry("bend", 0xFFE0A040), Map.entry("axis", 0xFF40D0D0));
    private static final Map<Group, Integer> GROUP_COLORS = Map.of(Group.ROTATION, 0xFFD8D8D8, Group.POSITION, 0xFF8CC0FF,
            Group.SCALE, 0xFFFFC870, Group.BEND, 0xFF9BE08C);
    private final Screen parent;
    private final StudioEditor editor;

    public static void open(String file) { open(file, true); }
    static void open(String file, boolean dialog) {
        Minecraft mc = Minecraft.getInstance();
        Emote emote = null;
        String error = null, name = file == null ? null : EmoteStudioClient.fileName(file);
        if (file != null) {
            try { emote = EmoteStudioClient.read(file); }
            catch (IOException | RuntimeException e) { error = "Can't open " + file + ": " + e.getMessage(); }
        }
        Emote start = emote == null ? blank() : emote;
        String startFile = emote == null ? null : name;
        Screen parent = mc.screen instanceof EmoteStudioScreen open ? open.parent : mc.screen;
        StudioEditor[] ref = new StudioEditor[1];
        EditorWindow window = EditorWindow.openDefault(() -> ref[0] = new StudioEditor(start, startFile));
        if (ref[0] == null) window = EditorWindow.openDefault(() -> ref[0] = new StudioEditor(start, startFile));
        ModularUI ui = ModularUI.of(UI.of(window, StylesheetManager.ORE), mc.player);
        mc.setScreen(new EmoteStudioScreen(ui, ref[0], parent));
        if (error != null) ref[0].notice(error);
        else if (file == null && dialog) ref[0].openDialog();
    }

    public static Object mcp(String tool, JsonObject a) throws IOException {
        if (tool.equals("list_emotes")) {
            JsonArray list = new JsonArray();
            for (String f : EmoteStudioClient.files()) {
                JsonObject o = new JsonObject();
                o.addProperty("file", f);
                try { Emote e = EmoteStudioClient.read(f); o.addProperty("name", e.name); o.addProperty("description", e.description); }
                catch (IOException | RuntimeException e) { o.addProperty("error", String.valueOf(e.getMessage())); }
                list.add(o);
            }
            return list;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof EmoteStudioScreen)) open(null, false);
        return ((EmoteStudioScreen) mc.screen).editor.mcp(tool, a);
    }
    static boolean has(JsonObject a, String k) { return a.get(k) != null && !a.get(k).isJsonNull(); }
    static JsonElement need(JsonObject a, String k) {
        if (!has(a, k)) throw new IllegalArgumentException("missing " + k);
        return a.get(k);
    }
    static String str(JsonObject a, String k) { return has(a, k) ? a.get(k).getAsString() : null; }
    static int num(JsonObject a, String k) { return need(a, k).getAsInt(); }
    static boolean flag(JsonObject a, String k) { return has(a, k) && a.get(k).getAsBoolean(); }

    static Emote blank() {
        Emote e = new Emote();
        e.name = "new_emote"; e.end = e.stop = 40;
        return e;
    }

    private EmoteStudioScreen(ModularUI ui, StudioEditor editor, Screen parent) {
        super(ui, Component.literal("Emote Studio"));
        this.parent = parent; this.editor = editor;
        modularUI.ui.rootElement.addEventListener(UIEvents.KEY_DOWN, event -> {
            if (event.target instanceof TextField || event.target instanceof TextArea) return;
            if (event.isCtrlDown()) return;
            switch (event.keyCode) {
                case GLFW.GLFW_KEY_SPACE -> editor.setPlaying(!editor.playing);
                case GLFW.GLFW_KEY_DELETE -> editor.deletePicked();
                case GLFW.GLFW_KEY_K -> editor.keyPart();
                case GLFW.GLFW_KEY_G -> editor.toggleGraph();
                case GLFW.GLFW_KEY_LEFT -> editor.step(-1);
                case GLFW.GLFW_KEY_RIGHT -> editor.step(1);
                case GLFW.GLFW_KEY_R -> { editor.preview.setTransformGizmoMode(TransformGizmo.Mode.ROTATE); editor.preview.placeHandle(); }
                case GLFW.GLFW_KEY_V -> { editor.preview.setTransformGizmoMode(TransformGizmo.Mode.TRANSLATE); editor.preview.placeHandle(); }
                case GLFW.GLFW_KEY_S -> { editor.preview.setTransformGizmoMode(TransformGizmo.Mode.SCALE); editor.preview.placeHandle(); }
                default -> { return; }
            }
            event.stopPropagation();
        });
    }

    @Override public void onClose() { editor.confirmDiscard(() -> Minecraft.getInstance().setScreen(parent)); }
    @Override public boolean isPauseScreen() { return false; }

    enum Held {
        SWORD("Sword", Items.WOODEN_SWORD), PAPER("Paper", Items.PAPER), BLOCK("Block", Items.GRASS_BLOCK), NONE("None (hidden)", Items.AIR);
        final String title;
        final ItemStack stack;
        Held(String title, net.minecraft.world.item.Item item) { this.title = title; this.stack = new ItemStack(item); }
        static Held of(String title) { for (Held h : values()) if (h.title.equals(title)) return h; return SWORD; }
    }
    static final List<String> DEFAULT_SKINS = List.of("steve", "alex", "ari", "efe", "kai", "makena", "noor", "sunny", "zuri");
    static final String PLAYER_SKIN = "Player (you)";
    static final class Look {
        String skin;
        boolean slim, hidden;
        Held right = Held.NONE, left = Held.NONE;
        static Look initial(int i) {
            Look l = new Look();
            if (i == 0) {
                Minecraft mc = Minecraft.getInstance();
                l.slim = mc.player != null && mc.player.getSkin().model() == PlayerSkin.Model.SLIM;
            } else { l.skin = i == 1 ? "steve" : "alex"; l.slim = i != 1; }
            return l;
        }
    }

    private static final class StudioEditor extends Editor {
        Emote emote;
        final List<Emote> cast = new ArrayList<>();
        int actor;
        String file;
        final TimelineView timeline;
        final Preview preview;
        String part;
        boolean bendSelected;
        record Dot(String part, Group group, int tick) {}
        final Set<Dot> picked = new HashSet<>();
        final Set<String> expanded = new HashSet<>();
        record Copied(String part, Group group, int offset, float[] values, String ease) {}
        record KeyLane(UIElement lane, String part, Group group) {}
        final List<KeyLane> keyLanes = new ArrayList<>();
        UIElement marquee;
        boolean graphOpen, graphShown;
        String channel = "pitch";
        Emote.Key selectedKey;
        Integer linkTick;
        final List<Copied> clipboard = new ArrayList<>();
        double time;
        boolean playing, loop = true;
        float speed = 1, scale = 8;
        UIElement rows, playhead;
        UIElement rulerRow;
        ScrollerView scroller;
        int rowCount, inspectedTick = -1;
        int dragDelta;
        final Deque<String> undo = new ArrayDeque<>(), redo = new ArrayDeque<>();
        String committed, saved;
        long lastCommit;

        StudioEditor(Emote emote, String file) {
            this.emote = emote; this.file = file;
            cast.add(emote); cast.addAll(partners(emote));
            committed = saved = snapshot();
            for (String p : Emote.PARTS) if (emote.animated(p)) expanded.add(p);
            if (leftWindow.getParentWindow() != null) leftWindow.getParentWindow().removeSplitWindow(leftWindow);
            if (historyView.getViewContainer() != null) historyView.removeSelf();
            timeline = new TimelineView(this);
            PreviewView previewView = new PreviewView(this);
            preview = previewView.preview;
            rootWindow.splitStyle(style -> style.percentage(77));
            if (bottomWindow.getParentWindow() != null) bottomWindow.getParentWindow().splitStyle(style -> style.percentage(55));
            ViewContainer oldResources = resourceView.getViewContainer();
            if (oldResources != null) oldResources.removeView(resourceView);
            viewFallbacks.remove(resourceView);
            ViewContainer previewContainer = centerWindow.getLeftTop();
            previewContainer.addView(previewView); previewContainer.selectView(previewView);
            ViewContainer timelineContainer = bottomWindow.getLeftBottom();
            timelineContainer.addView(timeline); timelineContainer.selectView(timeline);
            ViewContainer inspectorContainer = rightWindow.getRightTop();
            if (inspectorView.getViewContainer() != inspectorContainer) {
                if (inspectorView.getViewContainer() != null) inspectorView.removeSelf();
                inspectorContainer.addView(inspectorView);
            }
            inspectorContainer.selectView(inspectorView);
            rebuild(); inspect(); preview.refresh();
        }


        boolean dirty() { return !committed.equals(saved); }
        void notice(String text) {
            if (getModularUI() != null) Dialog.showNotification(text, 2.5f).show(getModularUI());
        }
        @Override protected void initMenus() {
            menuContainer.addChildren(new com.lowdragmc.lowdraglib2.editor.ui.menu.MenuTab(this) {
                        @Override protected com.lowdragmc.lowdraglib2.gui.util.TreeBuilder.Menu createDefaultMenu() {
                            return com.lowdragmc.lowdraglib2.gui.util.TreeBuilder.Menu.start()
                                    .leaf(Component.literal("New"), () -> confirmDiscard(() -> load(blank(), null)))
                                    .leaf(Component.literal("Open…"), () -> confirmDiscard(StudioEditor.this::openDialog))
                                    .leaf(Component.literal("Save  (Ctrl+S)"), StudioEditor.this::save)
                                    .leaf(Component.literal("Save As…"), StudioEditor.this::saveAsDialog);
                        }
                        @Override protected Component getComponent() { return Component.literal("File"); }
                    }.createMenuTab(),
                    new com.lowdragmc.lowdraglib2.editor.ui.menu.ViewMenu(this) {
                        @Override protected com.lowdragmc.lowdraglib2.gui.util.TreeBuilder.Menu createMenu() { return createDefaultMenu(); }
                    }.createMenuTab());
        }
        @Override protected void initKeymap() {
            super.initKeymap();
            keymap.registerAll(
                    EditorAction.builder(EditorActions.UNDO).category(KeymapCategories.EDIT).defaultChord(KeyChord.ctrl(GLFW.GLFW_KEY_Z)).onAction(() -> undo()).build(),
                    EditorAction.builder(EditorActions.REDO).category(KeymapCategories.EDIT).defaultChord(KeyChord.ctrl(GLFW.GLFW_KEY_Y))
                            .defaultAlternative(KeyChord.ctrlShift(GLFW.GLFW_KEY_Z)).onAction(() -> redo()).build(),
                    EditorAction.builder(EditorActions.SAVE).category(KeymapCategories.EDIT).defaultChord(KeyChord.ctrl(GLFW.GLFW_KEY_S)).onAction(() -> save()).build(),
                    EditorAction.builder(EditorActions.COPY).category(KeymapCategories.EDIT).defaultChord(KeyChord.ctrl(GLFW.GLFW_KEY_C))
                            .onAction(context -> typing() ? command(CommandEvents.COPY) : copyKeys()).build(),
                    EditorAction.builder(EditorActions.PASTE).category(KeymapCategories.EDIT).defaultChord(KeyChord.ctrl(GLFW.GLFW_KEY_V))
                            .onAction(context -> typing() ? command(CommandEvents.PASTE) : pasteKeys()).build());
        }
        boolean typing() { return getFocusedElement() instanceof TextField || getFocusedElement() instanceof TextArea;
        }
        @Override protected Editor createNewEditorInstance() { return new StudioEditor(emote, file); }
        @Override public Component getTitle() { return Component.literal("EMOTE · " + (file == null ? "unsaved" : file) + (dirty() ? " *" : "")); }

        void load(Emote next, String nextFile) {
            emote = next; file = nextFile;
            cast.clear(); cast.add(next); cast.addAll(partners(next)); actor = 0;
            committed = saved = snapshot();
            undo.clear(); redo.clear();
            part = null; picked.clear(); clipboard.clear(); selectedKey = null;
            expanded.clear();
            for (String p : Emote.PARTS) if (emote.animated(p)) expanded.add(p);
            playing = false; time = 0;
            rebuild(); inspect(); preview.refresh();
        }
        void save() {
            if (file == null) { saveAsDialog(); return; }
            write(file);
        }
        boolean write(String name) {
            try {
                Emote main = cast.getFirst();
                JsonArray links = new JsonArray();
                for (int i = 1; i < cast.size(); i++) links.add(partnerFile(name, i));
                if (links.isEmpty()) main.extra.remove(PARTNERS); else main.extra.add(PARTNERS, links);
                EmoteStudioClient.write(name, main);
                for (int i = 1; i < cast.size(); i++) EmoteStudioClient.write(partnerFile(name, i), cast.get(i));
                file = name; saved = committed = snapshot();
                notice("Saved emotes/" + name);
                return true;
            } catch (IOException e) {
                notice("Save failed: " + e.getMessage());
                return false;
            }
        }
        void saveAsDialog() {
            Dialog dialog = new Dialog().setAutoClose(false).setClickOutsideClose(true);
            dialog.setTitle("Save As  (emotes folder)");
            dialog.width(TaffyDimension.length(360));
            TextField name = new TextField().setAnyString();
            name.setText(file != null ? file.replaceFirst("(?i)\\.json$", "") + "_copy" : emote.name, false);
            name.layout(l -> l.widthPercent(100));
            dialog.addContent(name);
            dialog.addButton(new Button().setText("Save").setOnClick(e -> {
                String target = EmoteStudioClient.fileName(name.getText());
                if (target == null) { notice("Bad file name"); return; }
                Runnable doSave = () -> {
                    dialog.close();
                    if (!target.equals(file)) { cast.replaceAll(Emote::copyAsNew); emote = cast.get(actor); }
                    write(target);
                };
                if (EmoteStudioClient.exists(target) && !target.equals(file)) confirm("Replace emotes/" + target + "?", "Replace", doSave);
                else doSave.run();
            }));
            dialog.addButton(new Button().setText("Cancel").setOnClick(e -> dialog.close()));
            dialog.show(getModularUI());
        }
        void openDialog() { openDialog(false); }
        void openDialog(boolean asActor) {
            Dialog dialog = new Dialog().setAutoClose(false).setClickOutsideClose(true);
            List<String> files = EmoteStudioClient.files();
            Map<String, Emote> infos = new HashMap<>();
            for (String f : files) {
                try { infos.put(f, EmoteStudioClient.read(f)); } catch (IOException | RuntimeException ignored) {}
            }
            dialog.setTitle((asActor ? "Import Character" : "Open Emote") + "  (" + files.size() + " in emotes folder)");
            var window = Minecraft.getInstance().getWindow();
            int listHeight = Math.clamp(window.getGuiScaledHeight() - 130, 60, 260);
            dialog.width(TaffyDimension.length(Math.min(440, window.getGuiScaledWidth() - 40)));
            TextField search = new TextField().setAnyString();
            search.layout(l -> l.widthPercent(100));
            ScrollerView list = new ScrollerView();
            list.layout(l -> l.widthPercent(100).height(listHeight));
            Runnable fill = () -> {
                list.viewContainer.clearAllChildren();
                String query = search.getText().trim().toLowerCase(Locale.ROOT);
                for (String f : files) {
                    Emote info = infos.get(f);
                    String display = info == null ? "" : info.name;
                    if (!query.isEmpty() && !f.toLowerCase(Locale.ROOT).contains(query) && !display.toLowerCase(Locale.ROOT).contains(query)) continue;
                    UIElement row = new UIElement().layout(l -> l.widthPercent(100).height(20).flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(2));
                    Label label = new Label(); label.setText(f);
                    if (info != null) {
                        List<Component> tip = new ArrayList<>(List.of(Component.literal(display)));
                        if (!info.description.isBlank())
                            for (String line : info.description.split("\n")) tip.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.GRAY));
                        label.style(s -> s.tooltips(tip.toArray(Component[]::new)));
                    }
                    label.layout(l -> l.flex(1).heightPercent(100));
                    label.textStyle(s -> s.adaptiveWidth(false).textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER));
                    Button open = new Button().setText(asActor ? "Import" : "Open").setOnClick(e -> {
                        try { Emote next = EmoteStudioClient.read(f); dialog.close(); if (asActor) addActor(next.copyAsNew()); else load(next, f); }
                        catch (IOException | RuntimeException ex) { notice("Can't open " + f + ": " + ex.getMessage()); }
                    });
                    open.layout(l -> l.width(44).height(18));
                    row.addChildren(label, open);
                    list.addScrollViewChild(row);
                }
            };
            search.setTextResponder(text -> fill.run());
            fill.run();
            dialog.addContent(search); dialog.addContent(list);
            dialog.addButton(new Button().setText("Open Folder").setOnClick(e -> {
                try { java.nio.file.Files.createDirectories(EmoteStudioClient.folder()); } catch (IOException ignored) {}
                net.minecraft.Util.getPlatform().openFile(EmoteStudioClient.folder().toFile());
            }));
            dialog.addButton(new Button().setText("New Emote").setOnClick(e -> { dialog.close(); load(blank(), null); }));
            dialog.addButton(new Button().setText("Close").setOnClick(e -> dialog.close()));
            dialog.show(getModularUI());
        }
        void confirm(String question, String yes, Runnable action) {
            Dialog dialog = new Dialog().setAutoClose(false);
            dialog.setTitle(question);
            dialog.width(TaffyDimension.length(300));
            dialog.addButton((Button) new Button().setText(yes).setOnClick(e -> { dialog.close(); action.run(); }).addClass(STOP));
            dialog.addButton(new Button().setText("Cancel").setOnClick(e -> dialog.close()));
            dialog.show(getModularUI());
        }
        void confirmDiscard(Runnable then) {
            if (!dirty()) { then.run(); return; }
            Dialog dialog = new Dialog().setAutoClose(false);
            dialog.setTitle("Unsaved changes in " + (file == null ? "this emote" : file));
            dialog.width(TaffyDimension.length(320));
            dialog.addButton(new Button().setText("Save").setOnClick(e -> {
                dialog.close();
                if (file == null) saveAsDialog(); else if (write(file)) then.run();
            }));
            dialog.addButton((Button) new Button().setText("Discard").setOnClick(e -> { dialog.close(); then.run(); }).addClass(STOP));
            dialog.addButton(new Button().setText("Cancel").setOnClick(e -> dialog.close()));
            dialog.show(getModularUI());
        }


        int tick() { return (int) Math.floor(time); }
        int timelineTicks() { return Math.max(60, emote.length() + 40); }
        void setTime(double value) {
            time = Math.clamp(value, 0, timelineTicks());
            preview.refresh(); timeline.refreshCursor();
            if (!playing && tick() != inspectedTick) inspect();
        }
        void scrubTo(double ticks) { setTime(Math.round(ticks)); }
        void step(int ticks) { playing = false; scrubTo(time + ticks); }
        void setPlaying(boolean value) {
            playing = value;
            if (playing && time >= emote.length()) setTime(0);
            if (!playing) setTime(Math.round(time));
            timeline.refreshCursor();
        }
        void advance(double ticks) {
            double next = time + ticks, end = emote.loop ? emote.end + 1 : emote.length();
            if (holds() && next >= emote.end) { playing = false; setTime(emote.end); return; }
            if (next < end) { setTime(next); return; }
            if (loop || emote.loop) setTime(emote.loop ? emote.returnTick : 0);
            else { playing = false; setTime(end); }
        }


        void changed() { commit(); rebuild(); preview.refresh(); }
        void commit() {
            shareTiming();
            String now = snapshot();
            if (now.equals(committed)) return;
            long ms = System.currentTimeMillis();
            if (undo.isEmpty() || ms - lastCommit > 400) {
                undo.push(committed);
                while (undo.size() > 100) undo.removeLast();
            }
            lastCommit = ms; committed = now; redo.clear();
        }
        void undo() { step(undo, redo); }
        void redo() { step(redo, undo); }
        private void step(Deque<String> from, Deque<String> to) {
            if (from.isEmpty()) return;
            to.push(committed);
            committed = from.pop();
            lastCommit = 0;
            cast.clear();
            for (String json : committed.split(CUT)) cast.add(Emote.parse(json));
            actor = Math.min(actor, cast.size() - 1);
            emote = cast.get(actor);
            picked.clear(); selectedKey = null;
            rebuild(); inspect(); preview.refresh();
        }


        final List<Look> looks = new ArrayList<>();
        Look look(int i) {
            while (looks.size() <= i) looks.add(Look.initial(looks.size()));
            return looks.get(i);
        }
        final Map<String, ResourceLocation> customSkins = new HashMap<>();
        static final ResourceLocation STEVE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");
        ResourceLocation skinTexture(Look l) {
            if (l.skin == null) {
                Minecraft mc = Minecraft.getInstance();
                return mc.player == null ? STEVE : mc.player.getSkin().texture();
            }
            if (DEFAULT_SKINS.contains(l.skin)) return ResourceLocation.withDefaultNamespace("textures/entity/player/" + (l.slim ? "slim/" : "wide/") + l.skin + ".png");
            ResourceLocation id = customSkin(l.skin);
            return id == null ? STEVE : id;
        }
        static Path skinFolder() { return EmoteStudioClient.folder().resolve("skins"); }
        static List<String> skinFiles() {
            try (var stream = Files.list(skinFolder())) {
                return stream.map(f -> f.getFileName().toString()).filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".png")).sorted(String.CASE_INSENSITIVE_ORDER).toList();
            } catch (IOException e) {
                return List.of();
            }
        }
        ResourceLocation customSkin(String name) {
            if (customSkins.containsKey(name)) return customSkins.get(name);
            ResourceLocation id = null;
            try (var in = Files.newInputStream(skinFolder().resolve(name))) {
                NativeImage image = NativeImage.read(in);
                if (image.getWidth() == image.getHeight() && image.getWidth() >= 64 && image.getWidth() % 64 == 0) {
                    id = ResourceLocation.fromNamespaceAndPath(emotestudio.MODID, "emotestudio/skin_" + customSkins.size());
                    Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
                } else { image.close(); notice("Skin " + name + " must be square, 64 / 128 / 256... px"); }
            } catch (IOException | RuntimeException e) { notice("Can't read skin " + name + ": " + e.getMessage()); }
            customSkins.put(name, id);
            return id;
        }
        static String skinName(Look l) {
            if (l.skin == null) return PLAYER_SKIN;
            return DEFAULT_SKINS.contains(l.skin) ? Character.toUpperCase(l.skin.charAt(0)) + l.skin.substring(1) : l.skin;
        }
        static String skinKey(String name) {
            if (name.equals(PLAYER_SKIN)) return null;
            return name.toLowerCase(Locale.ROOT).endsWith(".png") ? name : name.toLowerCase(Locale.ROOT);
        }
        String characterFile() { return file == null ? null : actor == 0 ? file : partnerFile(file, actor); }
        static String pictureFile(String json) { return json.replaceFirst("(?i)\\.json$", "") + ".png"; }
        List<String> pictureFiles() {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < cast.size(); i++)
                if (preview.focus < 0 || preview.focus == i) list.add(pictureFile(i == 0 ? file : partnerFile(file, i)));
            return list;
        }
        void renameFile(String raw) {
            String target = EmoteStudioClient.fileName(raw);
            if (target == null || target.equals(file)) return;
            if (file != null) { cast.replaceAll(Emote::copyAsNew); emote = cast.get(actor); }
            file = target; saved = "";
        }
        void takePicture() {
            if (file == null) { notice("Save the emote first (the picture is named after its file)"); return; }
            List<String> pngs = pictureFiles();
            try {
                Path first = EmoteStudioClient.folder().resolve(pngs.getFirst());
                preview.capture(first);
                for (String png : pngs.subList(1, pngs.size()))
                    Files.copy(first, EmoteStudioClient.folder().resolve(png), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                notice("Picture saved: emotes/" + String.join(", ", pngs));
            } catch (IOException | RuntimeException e) {
                notice("Picture failed: " + e.getMessage());
            }
        }


        static final String PARTNERS = "mirrorcorePartners", STAGE = "mirrorcoreStage", CUT = "\n\u0001\n";
        String snapshot() { return String.join(CUT, cast.stream().map(Emote::toJson).toList()); }
        static String partnerFile(String main, int i) { return main.replaceFirst("(?i)\\.json$", "") + "_p" + (i + 1) + ".json"; }
        static List<Emote> partners(Emote main) {
            List<Emote> list = new ArrayList<>();
            if (main.extra.get(PARTNERS) instanceof JsonArray links)
                for (JsonElement link : links) {
                    try { list.add(EmoteStudioClient.read(link.getAsString())); } catch (IOException | RuntimeException ignored) {}
                }
            return list;
        }
        static float[] stage(Emote e) {
            if (!(e.extra.get(STAGE) instanceof JsonArray a) || a.size() < 4) return new float[4];
            return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), a.get(3).getAsFloat()};
        }
        static final Vector3f CARRY_ON = new Vector3f(0, .4F, .6F);
        static final Vector3f PIGGYBACK = new Vector3f(0, 1.8F * .2F, -.6F);
        void ridePreset(int i, Vector3f offset) {
            float[] carrier = stage(cast.getFirst());
            Vector3f at = new Vector3f(offset).rotateY((float) Math.toRadians(carrier[3]));
            setStage(cast.get(i), new float[]{carrier[0] + at.x, carrier[1] + at.y, carrier[2] + at.z, carrier[3]});
            changed(); inspect();
        }
        static void setStage(Emote e, float[] st) {
            JsonArray a = new JsonArray();
            for (float v : st) a.add(v);
            e.extra.add(STAGE, a);
        }
        void shareTiming() {
            for (Emote o : cast) if (o != emote) {
                o.begin = emote.begin; o.end = emote.end; o.stop = emote.stop;
                o.loop = emote.loop; o.returnTick = emote.returnTick; o.easeBefore = emote.easeBefore;
            }
        }
        void editActor(int i) {
            if (i == actor || i < 0 || i >= cast.size()) return;
            actor = i; emote = cast.get(i);
            picked.clear(); selectedKey = null; linkTick = null;
            rebuild(); inspect(); preview.refresh();
        }
        void addActor() {
            Emote e = new Emote();
            e.name = cast.getFirst().name + "_p" + (cast.size() + 1);
            addActor(e);
        }
        static final int MAX_CAST = 3;
        void addActor(Emote e) {
            if (cast.size() >= MAX_CAST) { notice("At most " + MAX_CAST + " characters"); return; }
            e.extra.remove(PARTNERS);
            if (!(e.extra.get(STAGE) instanceof JsonArray)) setStage(e, new float[]{cast.size(), 0, 0, 0});
            cast.add(e);
            shareTiming();
            actor = cast.size() - 1; emote = e;
            picked.clear(); selectedKey = null; linkTick = null;
            changed(); inspect();
        }
        void removeActor(int i) {
            if (i <= 0 || i >= cast.size()) return;
            confirm("Remove P" + (i + 1) + " (" + cast.get(i).name + ") from the scene?", "Remove", () -> dropActor(i));
        }
        void dropActor(int i) {
            cast.remove(i);
            if (i < looks.size()) looks.remove(i);
            preview.focus = -1;
            actor = 0; emote = cast.getFirst();
            picked.clear(); selectedKey = null; linkTick = null;
            changed(); inspect();
        }


        void selectPart(String p) { selectPart(p, false); }
        void selectPart(String p, boolean bend) {
            part = p; bendSelected = bend && p != null && Emote.groups(p).contains(Group.BEND); linkTick = null;
            if (p != null) expanded.add(p);
            rebuild(); inspect(); preview.refresh();
        }
        void addKey(String p, Group g) {
            emote.keyGroup(p, g, tick(), emote.values(p, g, tick()));
            part = p; expanded.add(p);
            picked.clear(); picked.add(new Dot(p, g, tick()));
            changed(); inspect();
        }
        void keyPart() {
            if (part == null) return;
            picked.clear();
            for (Group g : Emote.groups(part)) { emote.keyGroup(part, g, tick(), emote.values(part, g, tick())); picked.add(new Dot(part, g, tick())); }
            expanded.add(part);
            changed(); inspect();
        }
        void deletePicked() {
            if (picked.isEmpty()) return;
            for (Dot d : picked) emote.removeGroup(d.part, d.group, d.tick);
            picked.clear();
            changed(); inspect();
        }
        void movePicked(int delta) {
            if (delta == 0 || picked.isEmpty()) return;
            List<Dot> order = new ArrayList<>(picked);
            order.sort(Comparator.comparingInt(d -> delta > 0 ? -d.tick : d.tick));
            picked.clear();
            for (Dot d : order) { emote.moveGroup(d.part, d.group, d.tick, d.tick + delta); picked.add(new Dot(d.part, d.group, d.tick + delta)); }
            changed(); inspect();
        }
        boolean copyKeys() {
            if (picked.isEmpty()) return false;
            int first = picked.stream().mapToInt(Dot::tick).min().orElse(0);
            clipboard.clear();
            for (Dot d : picked) clipboard.add(new Copied(d.part, d.group, d.tick - first, emote.values(d.part, d.group, d.tick), emote.ease(d.part, d.group, d.tick)));
            notice("Copied " + clipboard.size() + " keyframe(s)");
            return true;
        }
        boolean pasteKeys() {
            if (clipboard.isEmpty()) return false;
            boolean onePart = clipboard.stream().map(Copied::part).distinct().count() == 1;
            picked.clear();
            for (Copied c : clipboard) {
                String p = onePart && part != null ? part : c.part;
                if (!Emote.groups(p).contains(c.group)) continue;
                int at = tick() + c.offset;
                emote.keyGroup(p, c.group, at, c.values);
                emote.setEase(p, c.group, at, c.ease);
                picked.add(new Dot(p, c.group, at));
                expanded.add(p);
            }
            changed(); inspect();
            return true;
        }


        void rebuild() {
            if (rows == null) return;
            rows.clearAllChildren();
            rowCount = 0; keyLanes.clear(); marquee = null;
            graphShown = graphOpen && part != null;
            float width = LABEL_WIDTH + timelineTicks() * scale;
            rows.layout(l -> l.positionType(TaffyPosition.RELATIVE).width(width).flexDirection(FlexDirection.COLUMN).gapAll(1));
            ruler();
            for (String p : Emote.PARTS) {
                partRow(p);
                if (expanded.contains(p)) for (Group g : Emote.groups(p)) groupRow(p, g);
                if (graphShown && p.equals(part)) graph();
            }
            playhead = new UIElement().setAllowHitTest(false).style(s -> s.backgroundTexture(fill(0xFF4A90E0)));
            rows.addChild(playhead);
            timeline.refreshCursor();
            timeline.slideRuler();
        }

        int rulerStep() {
            for (int step = 1; ; step *= 10)
                for (int m : new int[]{1, 2, 5}) if (step * m * scale >= 40) return step * m;
        }

        UIElement[] row(int nameColor) {
            rowCount++;
            UIElement row = new UIElement().layout(l -> l.width(LABEL_WIDTH + timelineTicks() * scale).height(ROW_HEIGHT)
                    .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER));
            UIElement name = new UIElement().layout(l -> l.width(LABEL_WIDTH).heightPercent(100).flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER))
                    .style(s -> s.backgroundTexture(fill(nameColor)));
            UIElement lane = new UIElement().layout(l -> l.positionType(TaffyPosition.RELATIVE).width(timelineTicks() * scale).heightPercent(100));
            row.addChildren(name, lane);
            rows.addChild(row);
            return new UIElement[]{name, lane};
        }
        void grid(GuiGraphics g, float x, float y, float w, float h, int color) {
            DrawerHelper.drawSolidRect(g, x, y, w, h, color);
            float end = (emote.loop ? emote.end : emote.length()) * scale;
            if (end < w) DrawerHelper.drawSolidRect(g, x + end, y, w - end, h, 0xFF121212);
            int step = rulerStep();
            for (int t = 0; t <= timelineTicks(); t += step) DrawerHelper.drawSolidRect(g, x + t * scale, y, 1, h, 0xFF2C2C2C);
        }
        static Label text(String value, int color) {
            Label label = new Label(); label.setText(value);
            label.layout(l -> l.flex(1).heightPercent(100).paddingLeft(4));
            label.textStyle(s -> s.adaptiveWidth(false).textAlignHorizontal(Horizontal.LEFT).textAlignVertical(Vertical.CENTER).textColor(color));
            return label;
        }
        void scrubbable(UIElement lane, Runnable onDown) {
            lane.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                if (e.button != 0) return;
                playing = false;
                if (onDown != null) onDown.run();
                scrubTo((e.x - lane.getPositionX()) / scale);
                lane.startDrag(null, null);
            });
            lane.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> scrubTo((e.x - lane.getPositionX()) / scale));
        }

        void ruler() {
            UIElement[] cells = row(0xFF303030);
            rulerRow = cells[0].getParent();
            rows.removeChild(rulerRow); rowCount--;
            timeline.rulerBox.clearAllChildren();
            timeline.rulerBox.addChild(rulerRow.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).top(0)));
            cells[0].layout(l -> l.gapAll(1).paddingLeft(2));
            for (int i = 0; i < cast.size(); i++) {
                int at = i;
                Button tab = small(i == actor ? "[P" + (i + 1) + "]" : "P" + (i + 1), () -> editActor(at));
                tab.layout(l -> l.width(24).height(14));
                cells[0].addChild(tab);
            }
            if (cast.size() < MAX_CAST) {
                Button add = small("+", this::addActor);
                add.layout(l -> l.width(14).height(14));
                cells[0].addChild(add);
            }
            cells[0].addChild(text(emote.name, 0xFFBBBBBB));
            UIElement lane = cells[1];
            int step = rulerStep();
            lane.style(s -> s.backgroundTexture((g, mx, my, x, y, w, h, pt) -> {
                grid(g, x, y, w, h, 0xFF262626);
                DrawerHelper.drawSolidRect(g, x + emote.stop * scale, y, 1, h, 0xFF888888);
                if (emote.loop) DrawerHelper.drawSolidRect(g, x + emote.returnTick * scale, y, 2, h, 0xFF4CAF50);
                DrawerHelper.drawSolidRect(g, x + (float) time * scale, y, 2, h, 0xFF4A90E0);
            }));
            for (int t = 0; t <= timelineTicks(); t += step) {
                int at = t; Label label = new Label(); label.setText(String.format(Locale.ROOT, "%.2f", t / 20.0));
                label.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(at * scale + 2).top(1).width(40).height(16));
                label.setAllowHitTest(false);
                lane.addChild(label);
            }
            TreeSet<Integer> keyed = new TreeSet<>();
            emote.keys.values().forEach(channels -> channels.values().forEach(list -> list.forEach(k -> keyed.add(k.tick))));
            int[] drag = {-1, -1};
            UIElement marks = new UIElement().layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(ROW_HEIGHT - 8).widthPercent(100).height(8));
            marks.style(s -> s.backgroundTexture((g, mx, my, x, y, w, h, pt) -> {
                for (int t : keyed) if (t != drag[0]) diamond(g, x + t * scale, y + h / 2, 0xFFD8D8D8);
                if (drag[0] >= 0) diamond(g, x + drag[1] * scale, y + h / 2, 0xFFFFD040);
            }));
            marks.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                if (e.button != 0) return;
                float at = (e.x - lane.getPositionX()) / scale;
                Integer hit = keyed.stream().filter(t -> Math.abs(t - at) * scale <= 4).min(Comparator.comparingDouble(t -> Math.abs(t - at))).orElse(null);
                if (hit == null) return;
                drag[0] = drag[1] = hit;
                marks.startDrag(null, null);
                e.stopPropagation();
            });
            marks.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
                if (drag[0] < 0) return;
                drag[1] = Math.max(0, Math.round((e.x - lane.getPositionX()) / scale));
                e.stopPropagation();
            });
            marks.addEventListener(UIEvents.DRAG_END, e -> {
                if (drag[0] < 0) return;
                int from = drag[0];
                drag[0] = -1;
                if (from == drag[1]) pickTick(from);
                else moveTick(from, drag[1]);
            });
            lane.addChild(marks);
            UIElement handle = new UIElement().style(s -> s.backgroundTexture(fill(0xFF4A90E0)));
            Runnable place = () -> handle.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(emote.end * scale - 2).top(0).width(4).heightPercent(100));
            place.run();
            handle.style(s -> s.tooltips("End tick: drag to change the emote's length (grey line = stop tick, green = loop return)"));
            handle.addEventListener(UIEvents.MOUSE_DOWN, e -> { if (e.button == 0) { handle.startDrag(null, null); e.stopPropagation(); } });
            handle.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
                int end = Math.max(1, Math.round((e.x - lane.getPositionX()) / scale));
                boolean hold = holds();
                emote.stop += end - emote.end;
                emote.end = end;
                if (hold) emote.returnTick = end;
                place.run(); timeline.refreshCursor();
            });
            handle.addEventListener(UIEvents.DRAG_END, e -> { changed(); inspect(); });
            lane.addChild(handle);
            scrubbable(lane, null);
        }

        Integer nearest(Collection<Integer> ticks, float at) {
            return ticks.stream().filter(t -> Math.abs(t - at) * scale <= 4).min(Comparator.comparingDouble(t -> Math.abs(t - at))).orElse(null);
        }
        void linkPart(String p, int t) {
            part = p; bendSelected = false; linkTick = t;
            picked.clear();
            for (Group g : Emote.groups(p)) if (emote.has(p, g, t)) picked.add(new Dot(p, g, t));
            List<String> channels = Emote.groups(p).stream().flatMap(g -> g.channels.stream()).toList();
            if (!channels.contains(channel) || emote.key(p, channel, t) == null)
                channel = channels.stream().filter(c -> emote.key(p, c, t) != null).findFirst().orElse(channels.getFirst());
            selectedKey = emote.key(p, channel, t);
            playing = false; time = t;
            rebuild(); inspect(); preview.refresh();
        }
        boolean linked(Emote.Key k) { return linkTick != null && k != null && k.tick == linkTick; }
        void syncLinked(String p, String ch, Emote.Key src) {
            if (!linked(src)) return;
            List<Emote.Key> own = emote.keys(p, ch);
            int at = own.indexOf(src);
            Emote.Key sNext = at + 1 < own.size() ? own.get(at + 1) : null, sPrev = at > 0 ? own.get(at - 1) : null;
            for (Group g : Emote.groups(p)) for (String c : g.channels) {
                Emote.Key k = c.equals(ch) ? null : emote.key(p, c, src.tick);
                if (k == null) continue;
                List<Emote.Key> keys = emote.keys(p, c);
                int i = keys.indexOf(k);
                Emote.Key next = i + 1 < keys.size() ? keys.get(i + 1) : null, prev = i > 0 ? keys.get(i - 1) : null;
                k.ease = src.ease; k.arg = src.arg;
                if (next != null && sNext != null) {
                    k.outT = rise(src.outT, sNext.tick - src.tick, next.tick - k.tick);
                    k.outV = rise(src.outV, sNext.value - src.value, next.value - k.value);
                    next.inT = rise(sNext.inT, sNext.tick - src.tick, next.tick - k.tick);
                    next.inV = rise(sNext.inV, sNext.value - src.value, next.value - k.value);
                }
                if (prev != null && sPrev != null) {
                    k.inT = rise(src.inT, src.tick - sPrev.tick, k.tick - prev.tick);
                    k.inV = rise(src.inV, src.value - sPrev.value, k.value - prev.value);
                }
            }
        }
        static float rise(float v, float from, float to) { return Math.abs(from) < 1e-4F ? 0 : v * to / from; }
        void moveTick(int from, int to) {
            if (from == to) return;
            for (String p : Emote.PARTS) for (Group g : Emote.groups(p)) if (emote.has(p, g, from)) emote.moveGroup(p, g, from, to);
            commit();
            pickTick(to);
        }
        void pickTick(int t) {
            picked.clear(); selectedKey = null; linkTick = null;
            for (String p : Emote.PARTS) for (Group g : Emote.groups(p)) if (emote.has(p, g, t)) picked.add(new Dot(p, g, t));
            playing = false; time = t;
            rebuild(); inspect(); preview.refresh();
        }
        static void diamond(GuiGraphics g, float cx, float cy, int color) {
            for (int i = -3; i <= 3; i++) { int w = 7 - 2 * Math.abs(i); DrawerHelper.drawSolidRect(g, cx - w / 2F, cy + i, w, 1, color); }
        }

        void partRow(String p) {
            boolean open = expanded.contains(p), current = p.equals(part);
            UIElement[] cells = row(current ? 0xFF3B5A80 : 0xFF2A2A2A);
            cells[0].addChild(text((open ? "▾ " : "▸ ") + p, emote.animated(p) ? 0xFFFFFFFF : 0xFF8A8A8A));
            cells[0].addEventListener(UIEvents.MOUSE_DOWN, e -> {
                if (e.button != 0) return;
                boolean fold = open && (current || e.x < cells[0].getPositionX() + 14);
                selectPart(p);
                if (fold) { expanded.remove(p); rebuild(); }
            });
            cells[1].style(s -> s.backgroundTexture((g, mx, my, x, y, w, h, pt) -> grid(g, x, y, w, h, current ? 0xFF22262C : 0xFF202020)));
            TreeSet<Integer> keyed = new TreeSet<>();
            for (Group group : Emote.groups(p)) keyed.addAll(emote.ticks(p, group));
            int[] drag = {-1, -1};
            UIElement marks = new UIElement().layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).widthPercent(100).heightPercent(100));
            marks.style(s -> s.backgroundTexture((g, mx, my, x, y, w, h, pt) -> {
                for (int t : keyed) if (t != drag[0])
                    diamond(g, x + t * scale, y + h / 2, current && linkTick != null && linkTick == t ? 0xFFFFD040 : 0xFFAAAAAA);
                if (drag[0] >= 0) diamond(g, x + drag[1] * scale, y + h / 2, 0xFFFFD040);
            }));
            marks.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                if (e.button != 0) return;
                Integer hit = nearest(keyed, (e.x - cells[1].getPositionX()) / scale);
                if (hit == null) return;
                drag[0] = drag[1] = hit;
                marks.startDrag(null, null);
                e.stopPropagation();
            });
            marks.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
                if (drag[0] < 0) return;
                drag[1] = Math.max(0, Math.round((e.x - cells[1].getPositionX()) / scale));
                e.stopPropagation();
            });
            marks.addEventListener(UIEvents.DRAG_END, e -> {
                if (drag[0] < 0) return;
                int from = drag[0], to = drag[1];
                drag[0] = -1;
                for (Group group : Emote.groups(p)) if (emote.has(p, group, from)) emote.moveGroup(p, group, from, to);
                if (from != to) commit();
                linkPart(p, to);
            });
            cells[1].addChild(marks);
            scrubbable(cells[1], () -> { if (!p.equals(part)) selectPart(p); });
        }

        void groupRow(String p, Group group) {
            boolean current = p.equals(part);
            UIElement[] cells = row(current ? 0xFF2E3F55 : 0xFF232323);
            UIElement swatch = new UIElement().layout(l -> l.width(3).height(10).marginLeft(16)).style(s -> s.backgroundTexture(fill(GROUP_COLORS.get(group))));
            Button plus = new Button().setText("+").setOnClick(e -> addKey(p, group));
            plus.layout(l -> l.width(16).height(15).marginRight(6));
            plus.style(s -> s.tooltips("Add a " + group.title + " keyframe at the playhead"));
            cells[0].addChildren(swatch, text(group.title, 0xFFCCCCCC), plus);
            cells[0].addEventListener(UIEvents.MOUSE_DOWN, e -> { if (e.button == 0 && !p.equals(part)) selectPart(p); });
            UIElement lane = cells[1];
            int color = GROUP_COLORS.get(group);
            lane.style(s -> s.backgroundTexture((g, mx, my, x, y, w, h, pt) -> {
                grid(g, x, y, w, h, current ? 0xFF1E2228 : 0xFF1A1A1A);
                for (int t : emote.ticks(p, group)) {
                    boolean sel = picked.contains(new Dot(p, group, t));
                    dot(g, x + (t + (sel ? dragDelta : 0)) * scale, y + h / 2, sel ? 0xFFFFFFFF : color, sel);
                }
            }));
            keyLanes.add(new KeyLane(lane, p, group));
            int[] mode = {0}, grab = {0};
            float[] box = new float[4];
            lane.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                int t = Math.round((e.x - lane.getPositionX()) / scale);
                boolean hit = emote.has(p, group, t) && Math.abs(e.x - (lane.getPositionX() + t * scale)) <= 5;
                Dot d = new Dot(p, group, t);
                if (e.button == 1) {
                    if (!hit) return;
                    if (!picked.contains(d)) { picked.clear(); picked.add(d); }
                    deletePicked();
                    return;
                }
                if (e.button != 0) return;
                playing = false;
                if (!p.equals(part)) part = p;
                if (hit) {
                    if (e.isShiftDown()) { if (!picked.remove(d)) picked.add(d); }
                    else if (!picked.contains(d)) { picked.clear(); picked.add(d); }
                    mode[0] = 1; grab[0] = t; dragDelta = 0;
                    setTime(t);
                    inspect();
                } else {
                    if (!e.isShiftDown()) picked.clear();
                    mode[0] = 0;
                    box[0] = box[2] = e.x; box[1] = box[3] = e.y;
                    scrubTo((e.x - lane.getPositionX()) / scale);
                }
                lane.startDrag(null, null);
            });
            lane.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
                if (mode[0] == 0) {
                    box[2] = e.x; box[3] = e.y;
                    if (marquee == null) {
                        if (Math.abs(box[2] - box[0]) + Math.abs(box[3] - box[1]) < 4) return;
                        marquee = new UIElement().setAllowHitTest(false).style(st -> st.backgroundTexture((g, mx, my, x, y, w, h, pt) -> {
                            DrawerHelper.drawSolidRect(g, x, y, w, h, 0x334A90E0);
                            DrawerHelper.drawSolidRect(g, x, y, w, 1, 0xFF4A90E0); DrawerHelper.drawSolidRect(g, x, y + h - 1, w, 1, 0xFF4A90E0);
                            DrawerHelper.drawSolidRect(g, x, y, 1, h, 0xFF4A90E0); DrawerHelper.drawSolidRect(g, x + w - 1, y, 1, h, 0xFF4A90E0);
                        }));
                        rows.addChild(marquee);
                    }
                    marquee.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
                            .left(Math.min(box[0], box[2]) - rows.getPositionX()).top(Math.min(box[1], box[3]) - rows.getPositionY())
                            .width(Math.abs(box[2] - box[0])).height(Math.abs(box[3] - box[1])));
                    return;
                }
                int first = picked.stream().mapToInt(Dot::tick).min().orElse(0);
                dragDelta = Math.max(-first, Math.round((e.x - lane.getPositionX()) / scale) - grab[0]);
                setTime(grab[0] + dragDelta);
            });
            lane.addEventListener(UIEvents.DRAG_END, e -> {
                if (mode[0] == 0) {
                    if (marquee != null) {
                        rows.removeChild(marquee); marquee = null;
                        selectBox(Math.min(box[0], box[2]), Math.min(box[1], box[3]), Math.max(box[0], box[2]), Math.max(box[1], box[3]));
                    }
                    rebuild(); inspect();
                    return;
                }
                int delta = dragDelta;
                dragDelta = 0;
                if (delta != 0) movePicked(delta); else rebuild();
            });
        }


        void toggleGraph() {
            graphOpen = !graphOpen;
            if (graphOpen && part == null) notice("Pick a part to see its graph");
            rebuild();
        }
        static String channelName(String ch) {
            return switch (ch) {
                case "pitch" -> "Pitch"; case "yaw" -> "Yaw"; case "roll" -> "Roll"; case "bend" -> "Bend"; case "axis" -> "Axis";
                case "scaleX" -> "Scale X"; case "scaleY" -> "Scale Y"; case "scaleZ" -> "Scale Z";
                default -> ch.toUpperCase(Locale.ROOT);
            };
        }
        void addGraphKey() {
            selectedKey = emote.keyChannel(part, channel, tick());
            picked.clear();
            changed(); inspect();
        }
        void deleteGraphKey() {
            if (selectedKey == null) return;
            emote.removeKey(part, channel, selectedKey.tick);
            selectedKey = null;
            changed(); inspect();
        }

        void graph() {
            String p = part;
            Set<Group> pickedGroups = new HashSet<>();
            for (Dot d : picked) if (d.part.equals(p)) pickedGroups.add(d.group);
            List<String> channels = pickedGroups.size() == 1 ? pickedGroups.iterator().next().channels
                    : Emote.groups(p).stream().flatMap(g -> g.channels.stream()).toList();
            if (!channels.contains(channel)) channel = channels.getFirst();
            String ch = channel;
            List<Emote.Key> keys = emote.keys(p, ch);
            if (selectedKey != null && !keys.contains(selectedKey)) selectedKey = null;
            int ticks = timelineTicks();
            double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
            for (int t = 0; t <= ticks; t++) { double v = emote.value(p, ch, t); lo = Math.min(lo, v); hi = Math.max(hi, v); }
            for (Emote.Key k : keys) {
                lo = Math.min(lo, k.value); hi = Math.max(hi, k.value);
                if (k == selectedKey) { lo = Math.min(lo, k.value + Math.min(k.inV, k.outV)); hi = Math.max(hi, k.value + Math.max(k.inV, k.outV)); }
            }
            double pad = Math.max(1, (hi - lo) * .15), min = lo - pad, max = hi + pad;
            java.util.function.DoubleUnaryOperator y = v -> 4 + (max - v) / (max - min) * (GRAPH_HEIGHT - 8);

            UIElement row = new UIElement().layout(l -> l.width(LABEL_WIDTH + ticks * scale).height(GRAPH_HEIGHT).flexDirection(FlexDirection.ROW));
            UIElement side = new UIElement().layout(l -> l.width(LABEL_WIDTH).heightPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2).paddingAll(2))
                    .style(st -> st.backgroundTexture(fill(0xFF262626)));
            side.addChildren(label("GRAPH · " + p + " · " + channelName(ch)), label(selectedKey == null ? "no key selected"
                    : String.format(Locale.ROOT, "key  t%d  =  %.2f", selectedKey.tick, selectedKey.value)));
            for (int i = 0; i < channels.size(); i += 3) {
                UIElement picks = new UIElement().layout(l -> l.widthPercent(100).height(15).flexDirection(FlexDirection.ROW).gapAll(1));
                for (String c : channels.subList(i, Math.min(i + 3, channels.size()))) {
                    String name = channelName(c) + (emote.keys(p, c).isEmpty() ? "" : "◆");
                    Button pick = new Button().setText(ch.equals(c) ? "[" + name + "]" : name).setOnClick(e -> { channel = c; selectedKey = null; rebuild(); });
                    pick.layout(l -> l.flex(1).height(14));
                    picks.addChild(pick);
                }
                side.addChild(picks);
            }
            UIElement actions = new UIElement().layout(l -> l.widthPercent(100).height(16).flexDirection(FlexDirection.ROW).gapAll(1));
            actions.addChildren(small("+KEY", this::addGraphKey), small("DEL", this::deleteGraphKey), small("CLEAR", () -> {
                for (int t : keys.stream().map(k -> k.tick).toList()) emote.removeKey(p, ch, t);
                selectedKey = null; changed(); inspect();
            }));
            side.addChild(actions);
            if (linked(selectedKey)) side.addChild(label("◆ editing all " + p + " keys @t" + linkTick));
            if (selectedKey != null) {
                Emote.Key k = selectedKey;
                Selector<String> ease = new Selector<>();
                ease.setCandidates(EASES).setSelected(easeName(k.ease), false).setOnValueChanged(v -> setKeyEase(keys, k, v));
                ease.layout(l -> l.widthPercent(100).height(16));
                side.addChild(ease);
            } else side.addChild(label("Ease: select a key"));
            side.addChild(label("drag ● · right-click deletes"));

            UIElement lane = new UIElement().layout(l -> l.positionType(TaffyPosition.RELATIVE).width(ticks * scale).heightPercent(100));
            lane.style(st -> st.backgroundTexture((g, mx, my, x, y0, w, h, pt) -> {
                grid(g, x, y0, w, h, 0xFF151515);
                for (int i = 0; i <= 4; i++) DrawerHelper.drawSolidRect(g, x, y0 + 4 + i * (h - 8) / 4F, w, 1, 0xFF2A2A2A);
                if (min < 0 && max > 0) DrawerHelper.drawSolidRect(g, x, y0 + (float) y.applyAsDouble(0), w, 1, 0xFF505050);
                Group group = Arrays.stream(Group.values()).filter(gr -> gr.channels.contains(ch)).findFirst().orElse(Group.ROTATION);
                for (String c : group.channels) {
                    boolean current = c.equals(ch);
                    if (!current && emote.keys(p, c).isEmpty()) continue;
                    int color = CHANNEL_COLORS.getOrDefault(c, 0xFFFFFFFF);
                    if (!current) color = 0x55000000 | color & 0xFFFFFF;
                    float previous = Float.NaN;
                    for (int px = 0; px <= w; px += 2) {
                        float py = (float) Math.clamp(y.applyAsDouble(emote.value(p, c, px / scale)), 0, h - 1);
                        float top = Float.isNaN(previous) ? py : Math.min(previous, py), bottom = Float.isNaN(previous) ? py : Math.max(previous, py);
                        DrawerHelper.drawSolidRect(g, x + px, y0 + top, current ? 2 : 1, bottom - top + (current ? 2 : 1), color);
                        previous = py;
                    }
                }
            }));
            for (double v : new double[]{max, min}) {
                Label label = new Label(); label.setText(String.format(Locale.ROOT, "%.1f", v));
                float top = v == max ? 2 : GRAPH_HEIGHT - 14;
                label.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(3).top(top).width(60).height(12));
                label.textStyle(st -> st.adaptiveWidth(false).textAlignHorizontal(Horizontal.LEFT));
                label.setAllowHitTest(false);
                lane.addChild(label);
            }
            scrubbable(lane, null);
            int color = CHANNEL_COLORS.getOrDefault(ch, 0xFFFFFFFF);
            double range = range(ch);
            for (Emote.Key k : keys) {
                UIElement point = new UIElement();
                Runnable place = () -> point.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
                        .left(k.tick * scale - 4).top((float) y.applyAsDouble(k.value) - 4).width(9).height(9));
                place.run();
                point.style(st -> st.backgroundTexture((g, mx, my, x, y0, w, h, pt) -> dot(g, x + w / 2F, y0 + h / 2F, k == selectedKey ? 0xFFFFFFFF : color, k == selectedKey)));
                point.addEventListener(UIEvents.MOUSE_DOWN, e -> {
                    e.stopPropagation();
                    selectedKey = k; linkTick = null;
                    if (e.button == 1) { deleteGraphKey(); return; }
                    if (e.button != 0) return;
                    playing = false;
                    setTime(k.tick);
                    point.startDrag(null, null);
                });
                point.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
                    k.tick = Math.clamp(Math.round((e.x - lane.getPositionX()) / scale), 0, ticks);
                    double v = max - (e.y - lane.getPositionY() - 4) / (GRAPH_HEIGHT - 8) * (max - min);
                    k.value = (float) Math.clamp(Math.round(v * 100) / 100.0, -range, range);
                    k.turn = 0;
                    place.run(); setTime(k.tick);
                });
                point.addEventListener(UIEvents.DRAG_END, e -> { emote.retime(p, ch, k); changed(); inspect(); });
                lane.addChild(point);
            }
            int index = selectedKey == null ? -1 : keys.indexOf(selectedKey);
            if (index >= 0) {
                Emote.Key k = selectedKey;
                if (k.custom() && index + 1 < keys.size()) handle(lane, k, true, keys.get(index + 1).tick - k.tick, y, min, max);
                if (index > 0 && keys.get(index - 1).custom()) handle(lane, k, false, k.tick - keys.get(index - 1).tick, y, min, max);
            }
            row.addChildren(side, lane);
            rows.addChild(row);
        }
        static final List<String> EASES = java.util.stream.Stream.concat(java.util.stream.Stream.of(Emote.CUSTOM),
                Arrays.stream(Ease.values()).map(Ease::name)).toList();
        static String easeName(String ease) { return Emote.CUSTOM.equals(ease) ? Emote.CUSTOM : Easing.easeFromString(ease).name(); }
        void setKeyEase(List<Emote.Key> keys, Emote.Key k, String ease) {
            easeKey(keys, k, ease);
            syncLinked(part, channel, k);
            changed();
        }
        static void easeKey(List<Emote.Key> keys, Emote.Key k, String ease) {
            if (ease.equals(Emote.CUSTOM) && !k.custom()) {
                int index = keys.indexOf(k);
                if (index >= 0 && index + 1 < keys.size()) {
                    Emote.Key next = keys.get(index + 1);
                    float third = (next.tick - k.tick) / 3F;
                    k.outT = third; k.outV = 0; next.inT = -third; next.inV = 0;
                }
            }
            k.ease = ease;
        }

        void handle(UIElement lane, Emote.Key k, boolean out, float span, java.util.function.DoubleUnaryOperator y, double min, double max) {
            UIElement dot = new UIElement();
            java.util.function.DoubleSupplier ht = () -> out ? Math.clamp(k.outT, 0, span) : Math.clamp(k.inT, -span, 0);
            java.util.function.DoubleSupplier hv = () -> out ? k.outV : k.inV;
            Runnable place = () -> dot.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
                    .left((float) (k.tick + ht.getAsDouble()) * scale - 3).top((float) y.applyAsDouble(k.value + hv.getAsDouble()) - 3).width(7).height(7));
            place.run();
            dot.style(st -> st.backgroundTexture((g, mx, my, x, y0, w, h, pt) -> {
                DrawerHelper.drawSolidRect(g, x, y0, w, h, 0xFFFFFFFF);
                DrawerHelper.drawSolidRect(g, x + 1, y0 + 1, w - 2, h - 2, 0xFF202020);
            }));
            UIElement guide = new UIElement().setAllowHitTest(false)
                    .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).widthPercent(100).heightPercent(100));
            guide.style(st -> st.backgroundTexture((g, mx, my, x, y0, w, h, pt) -> {
                float x1 = x + k.tick * scale, y1 = y0 + (float) y.applyAsDouble(k.value);
                float x2 = x + (float) (k.tick + ht.getAsDouble()) * scale, y2 = y0 + (float) y.applyAsDouble(k.value + hv.getAsDouble());
                for (int i = 0; i <= 24; i++) DrawerHelper.drawSolidRect(g, x1 + (x2 - x1) * i / 24F, y1 + (y2 - y1) * i / 24F, 1, 1, 0xAAFFFFFF);
            }));
            dot.addEventListener(UIEvents.MOUSE_DOWN, e -> { e.stopPropagation(); if (e.button == 0) dot.startDrag(null, null); });
            dot.addEventListener(UIEvents.DRAG_SOURCE_UPDATE, e -> {
                float t = (e.x - lane.getPositionX()) / scale - k.tick;
                float v = (float) (max - (e.y - lane.getPositionY() - 4) / (GRAPH_HEIGHT - 8) * (max - min)) - k.value;
                if (out) { k.outT = Math.clamp(t, 0, span); k.outV = v; } else { k.inT = Math.clamp(t, -span, 0); k.inV = v; }
                syncLinked(part, channel, k);
                place.run(); preview.refresh();
            });
            dot.addEventListener(UIEvents.DRAG_END, e -> changed());
            lane.addChildren(guide, dot);
        }

        static Label label(String text) {
            Label label = new Label(); label.setText(text);
            label.layout(l -> l.widthPercent(100).height(12));
            label.textStyle(st -> st.adaptiveWidth(false).textAlignHorizontal(Horizontal.LEFT));
            return label;
        }
        static Button small(String text, Runnable action) {
            Button button = new Button().setText(text).setOnClick(e -> action.run());
            button.layout(l -> l.flex(1).height(16));
            return button;
        }

        void selectBox(float x0, float y0, float x1, float y1) {
            for (KeyLane k : keyLanes) {
                float top = k.lane.getPositionY();
                if (top + ROW_HEIGHT < y0 || top > y1) continue;
                for (int t : emote.ticks(k.part, k.group)) {
                    float x = k.lane.getPositionX() + t * scale;
                    if (x >= x0 - 3 && x <= x1 + 3) picked.add(new Dot(k.part, k.group, t));
                }
            }
        }

        static void dot(GuiGraphics g, float cx, float cy, int color, boolean picked) {
            int border = picked ? 0xFF4A90E0 : 0xFF0E0E0E;
            DrawerHelper.drawSolidRect(g, cx - 3, cy - 5, 6, 10, border);
            DrawerHelper.drawSolidRect(g, cx - 5, cy - 3, 10, 6, border);
            DrawerHelper.drawSolidRect(g, cx - 4, cy - 4, 8, 8, border);
            DrawerHelper.drawSolidRect(g, cx - 2, cy - 4, 4, 8, color);
            DrawerHelper.drawSolidRect(g, cx - 4, cy - 2, 8, 4, color);
            DrawerHelper.drawSolidRect(g, cx - 3, cy - 3, 6, 6, color);
        }


        void inspect() {
            inspectedTick = tick();
            IConfigurable config = IConfigurable.create(part == null ? this::buildEmote : this::buildPart);
            inspectorView.inspect(config, ignored -> changed(), () -> {}, this::changed);
        }
        static ConfiguratorGroup section(ConfiguratorGroup parent, String name) {
            ConfiguratorGroup group = new ConfiguratorGroup(name);
            group.setCanCollapse(true); group.setCollapse(false);
            parent.addConfigurator(group);
            return group;
        }

        final Map<String, Boolean> folded = new HashMap<>();
        ConfiguratorGroup category(ConfiguratorGroup parent, String key, String name, boolean closed) {
            boolean fold = folded.getOrDefault(key, closed);
            ConfiguratorGroup group = new ConfiguratorGroup(name) {
                @Override public ConfiguratorGroup setCollapse(boolean collapse) { folded.put(key, collapse); return super.setCollapse(collapse); }
            };
            group.setCanCollapse(true);
            group.setCollapse(fold);
            parent.addConfigurator(group);
            return group;
        }

        static String amp(String text) { return text.replace('§', '&'); }
        static String section(String text) { return text.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1"); }
        boolean holds() { return emote.loop && emote.returnTick == emote.end; }
        void setHold(boolean v) {
            if (v) {
                int last = emote.keys.values().stream().flatMap(c -> c.values().stream()).flatMap(List::stream).mapToInt(k -> k.tick).max().orElse(emote.end);
                emote.stop += Math.max(1, last) - emote.end;
                emote.end = Math.max(1, last);
            }
            emote.loop = v; emote.returnTick = v ? emote.end : 0; changed(); inspect();
        }
        void buildEmote(ConfiguratorGroup group) {
            String who = "P" + (actor + 1);
            boolean duet = cast.size() > 1;
            ConfiguratorGroup info = category(group, "emote", duet ? "Emote · " + who + (actor == 0 ? " (main file)" : "") : "Emote", false);
            String bare = file == null ? "" : file.replaceFirst("(?i)\\.json$", "");
            if (actor == 0) info.addConfigurator(new StringConfigurator("File Name", () -> file == null ? "" : file.replaceFirst("(?i)\\.json$", ""), this::renameFile, bare, true));
            else info.addConfigurator(new Configurator("File: " + (file == null ? "<file>_p" + (actor + 1) + ".json (after saving)" : characterFile())));
            info.addConfigurator(text("Display Name", () -> emote.name, v -> emote.name = v));
            info.addConfigurator(text("Description (&)", () -> amp(emote.description), v -> emote.description = section(v)));
            info.addConfigurator(text("Author (&)", () -> amp(emote.author), v -> emote.author = section(v)));

            ConfiguratorGroup timing = category(info, "timing", "Timing (ticks, 20 = 1 s)" + (duet ? " · all characters" : ""), false);
            timing.addConfigurator(integer("Begin Tick", 0, 72000, () -> emote.begin, v -> emote.begin = v));
            timing.addConfigurator(integer("End Tick", 1, 72000, () -> emote.end, v -> { boolean hold = holds(); emote.end = v; if (hold) emote.returnTick = v; }));
            timing.addConfigurator(integer("Stop Tick (fade out)", 1, 72000, () -> emote.stop, v -> emote.stop = v));
            timing.addConfigurator(new BooleanConfigurator("Hold Last Frame (export)", this::holds, this::setHold, holds(), true));
            timing.addConfigurator(new BooleanConfigurator("Loop", () -> emote.loop, v -> { emote.loop = v; changed(); }, emote.loop, true));
            timing.addConfigurator(integer("Loop Return Tick", 0, 72000, () -> emote.returnTick, v -> emote.returnTick = v));
            timing.addConfigurator(new BooleanConfigurator("Ease Before Keyframe", () -> emote.easeBefore, v -> { emote.easeBefore = v; changed(); }, emote.easeBefore, true));

            ConfiguratorGroup files = category(info, "file", "File · " + (file == null ? "not saved yet" : "emotes/" + file), true);
            files.addConfigurator(button("New Emote", () -> confirmDiscard(() -> load(blank(), null))));
            files.addConfigurator(button("Open…", () -> confirmDiscard(this::openDialog)));
            files.addConfigurator(button(file == null ? "Save…  (Ctrl+S)" : "Save (overwrite)  (Ctrl+S)", this::save, GO));
            files.addConfigurator(button("Save As New File…", this::saveAsDialog));

            ConfiguratorGroup scene = category(group, "characters", "Characters · editing keys of " + who + " (" + cast.size() + " / " + MAX_CAST + ")", false);
            if (cast.size() < MAX_CAST) {
                scene.addConfigurator(button("Add Character", this::addActor, GO));
                scene.addConfigurator(button("Import Character (emote file)…", () -> openDialog(true), GO));
            }
            scene.addConfigurator(button("Open Skins Folder (square PNG, 64 px or HD)", () -> {
                try { Files.createDirectories(skinFolder()); } catch (IOException ignored) {}
                net.minecraft.Util.getPlatform().openFile(skinFolder().toFile());
            }));
            ConfiguratorGroup list = category(scene, "cast", "Characters", false);
            for (int i = 0; i < cast.size(); i++) buildCharacter(list, i);

            ConfiguratorGroup camera = category(group, "camera", "Camera · emote picture", true);
            camera.addConfigurator(new BooleanConfigurator("Show Picture Frame", () -> preview.cameraFrame, v -> preview.cameraFrame = v, preview.cameraFrame, true));
            List<String> focus = new ArrayList<>(List.of("All"));
            for (int i = 0; i < cast.size(); i++) focus.add("P" + (i + 1));
            String focused = preview.focus < 0 || preview.focus >= cast.size() ? "All" : "P" + (preview.focus + 1);
            camera.addConfigurator(new SelectorConfigurator<>("Focus (others 30% bright)", () -> preview.focus < 0 ? "All" : "P" + (preview.focus + 1),
                    v -> { preview.focus = v.equals("All") ? -1 : Integer.parseInt(v.substring(1)) - 1; inspect(); }, focused, true, focus, x -> x));
            camera.addConfigurator(button(file == null ? "Take Picture (save the emote first)" : "Take Picture → " + String.join(", ", pictureFiles()),this::takePicture, file == null ? null : GO));
            camera.addConfigurator(button("Reset View", preview::resetView));
        }

        void buildCharacter(ConfiguratorGroup parent, int i) {
            String who = "P" + (i + 1);
            Emote e = cast.get(i);
            Look look = look(i);
            ConfiguratorGroup it = category(parent, "char" + i, who + " · " + e.name + (i == actor ? "  (editing)" : ""), i != actor);
            if (i > 0) it.addConfigurator(button("Remove " + who, () -> removeActor(i), STOP));
            it.addConfigurator(new BooleanConfigurator("Hide", () -> look.hidden, v -> look.hidden = v, look.hidden, true));

            ConfiguratorGroup looks = category(it, "look" + i, "Look (preview only, not saved)", false);
            List<String> skins = new ArrayList<>();
            skins.add(PLAYER_SKIN);
            for (String d : DEFAULT_SKINS) skins.add(Character.toUpperCase(d.charAt(0)) + d.substring(1));
            skins.addAll(skinFiles());
            looks.addConfigurator(new SelectorConfigurator<>("Skin", () -> skinName(look), v -> look.skin = skinKey(v), skinName(look), true, skins, x -> x));
            looks.addConfigurator(new BooleanConfigurator("Slim Arms (3 px)", () -> look.slim, v -> look.slim = v, look.slim, true));
            List<String> held = Arrays.stream(Held.values()).map(h -> h.title).toList();
            looks.addConfigurator(new SelectorConfigurator<>("Right Hand", () -> look.right.title, v -> look.right = Held.of(v), look.right.title, true, held, x -> x));
            looks.addConfigurator(new SelectorConfigurator<>("Left Hand", () -> look.left.title, v -> look.left = Held.of(v), look.left.title, true, held, x -> x));

            ConfiguratorGroup stand = category(it, "stand" + i, "Stand (preview position)", false);
            for (int a = 0; a < 4; a++) {
                int axis = a;
                stand.addConfigurator(number(new String[]{"X (blocks)", "Y (blocks)", "Z (blocks)", "Facing (Yaw °)"}[a], a == 3 ? 360 : 16,
                        () -> stage(e)[axis], v -> { float[] st = stage(e); st[axis] = (float) v; setStage(e, st); }));
            }
            if (i > 0) {
                stand.addConfigurator(new SelectorConfigurator<>("Preset (from P1)", () -> PICK, v -> {
                    if (v.equals(CARRY)) ridePreset(i, CARRY_ON);
                    else if (v.equals(PIGGY)) ridePreset(i, PIGGYBACK);
                }, PICK, true, List.of(PICK, CARRY, PIGGY), x -> x));
            }
        }
        static final String GO = "__confirm-button__", STOP = "__reject-button__";
        static final String PICK = "Choose…", CARRY = "Carry On (held by P1)", PIGGY = "Piggyback (on P1's back)";

        void buildPart(ConfiguratorGroup group) {
            String p = part;
            int t = tick();
            ConfiguratorGroup head = section(group, "Part · " + p + "  @ tick " + t);
            head.addConfigurator(button("Key Whole Part  (K)", this::keyPart));
            head.addConfigurator(button("Emote Settings / Files", () -> selectPart(null)));
            for (Group g : Emote.groups(p)) {
                boolean keyed = emote.has(p, g, t);
                ConfiguratorGroup sec = section(group, g.title + (keyed ? "  ◆ keyframe" : "  (no key here)"));
                for (String ch : g.channels)
                    sec.addConfigurator(number(channelLabel(ch), range(ch), () -> emote.value(p, ch, tick()), v -> emote.set(p, g, ch, tick(), (float) v)));
                if (keyed) {
                    sec.addConfigurator(new SelectorConfigurator<>("Ease", () -> easeName(emote.ease(p, g, tick())),
                            v -> { emote.setEase(p, g, tick(), v); changed(); }, easeName(emote.ease(p, g, t)), true, EASES, s -> s));
                    sec.addConfigurator(button("Remove Keyframe", () -> { emote.removeGroup(p, g, tick()); picked.remove(new Dot(p, g, tick())); changed(); inspect(); }, STOP));
                } else sec.addConfigurator(button("Add Keyframe", () -> addKey(p, g), GO));
            }
        }
        static String channelLabel(String ch) {
            return switch (ch) {
                case "pitch" -> "Pitch (X) °"; case "yaw" -> "Yaw (Y) °"; case "roll" -> "Roll (Z) °";
                case "x" -> "X (px, + = left)"; case "y" -> "Y (px, + = down)"; case "z" -> "Z (px, + = back)";
                case "bend" -> "Bend °"; case "axis" -> "Bend Axis °";
                default -> ch.substring(5);
            };
        }
        static double range(String ch) { return Emote.angle(ch) ? 3600 : ch.startsWith("scale") ? 16 : 512; }

        Configurator text(String label, java.util.function.Supplier<String> get, java.util.function.Consumer<String> set) {
            return new StringConfigurator(label, get, v -> { set.accept(v); changed(); }, get.get(), true);
        }
        Configurator integer(String label, int min, int max, java.util.function.IntSupplier get, java.util.function.IntConsumer set) {
            return new NumberConfigurator(label, get::getAsInt, v -> { set.accept(Math.clamp(v.intValue(), min, max)); changed(); }, get.getAsInt(), true)
                    .setRange(min, max).setWheel(1).setType(ConfigNumber.Type.INTEGER);
        }
        Configurator number(String label, double range, java.util.function.DoubleSupplier get, java.util.function.DoubleConsumer set) {
            return new NumberConfigurator(label, get::getAsDouble, v -> { set.accept(Math.clamp(v.doubleValue(), -range, range)); changed(); }, get.getAsDouble(), true)
                    .setRange(-range, range).setWheel(1).setType(ConfigNumber.Type.DOUBLE);
        }
        Configurator button(String text, Runnable action) { return button(text, action, null); }
        Configurator button(String text, Runnable action, String oreClass) {
            Configurator row = new Configurator("");
            Button button = new Button().setText(text).setOnClick(e -> action.run());
            if (oreClass != null) button.addClass(oreClass);
            button.layout(l -> l.widthPercent(100));
            return row.addInlineChild(button);
        }

        Object mcp(String tool, JsonObject a) throws IOException {
            switch (tool) {
                case "get_state" -> { return state(); }
                case "open_emote" -> {
                    if (dirty() && !flag(a, "discard")) throw new IllegalStateException("Unsaved changes in " + (file == null ? "this emote" : file) + ": save first or pass discard=true");
                    String f = str(a, "file");
                    if (f == null) load(blank(), null); else load(EmoteStudioClient.read(f), EmoteStudioClient.fileName(f));
                }
                case "save" -> {
                    String f = str(a, "file");
                    if (f == null && file == null) throw new IllegalArgumentException("Never saved: pass file");
                    String target = f == null ? file : EmoteStudioClient.fileName(f);
                    if (target == null) throw new IllegalArgumentException("Bad file name: " + f);
                    if (!target.equals(file)) {
                        if (EmoteStudioClient.exists(target) && !flag(a, "overwrite")) throw new IllegalStateException("emotes/" + target + " exists: pass overwrite=true");
                        cast.replaceAll(Emote::copyAsNew); emote = cast.get(actor);
                    }
                    if (!write(target)) throw new IOException("Save failed");
                    return "Saved emotes/" + target;
                }
                case "get_emote_json" -> { return cast.get(character(a)).toJson(); }
                case "set_emote_json" -> {
                    int i = character(a);
                    Emote e = Emote.parse(need(a, "json").getAsString());
                    if (i > 0) e.extra.remove(PARTNERS);
                    cast.set(i, e);
                    actor = i; emote = e; picked.clear(); selectedKey = null; linkTick = null;
                    for (String p : Emote.PARTS) if (e.animated(p)) expanded.add(p);
                    changed(); inspect();
                }
                case "set_emote_settings" -> {
                    if (has(a, "name")) emote.name = str(a, "name");
                    if (has(a, "description")) emote.description = section(str(a, "description"));
                    if (has(a, "author")) emote.author = section(str(a, "author"));
                    if (has(a, "begin_tick")) emote.begin = Math.clamp(num(a, "begin_tick"), 0, 72000);
                    if (has(a, "end_tick")) { boolean hold = holds(); emote.end = Math.clamp(num(a, "end_tick"), 1, 72000); if (hold) emote.returnTick = emote.end; }
                    if (has(a, "stop_tick")) emote.stop = Math.clamp(num(a, "stop_tick"), 1, 72000);
                    if (has(a, "loop")) emote.loop = flag(a, "loop");
                    if (has(a, "return_tick")) emote.returnTick = Math.clamp(num(a, "return_tick"), 0, 72000);
                    if (has(a, "ease_before")) emote.easeBefore = flag(a, "ease_before");
                    if (has(a, "hold")) setHold(flag(a, "hold"));
                    changed(); inspect();
                    return state().get("emote");
                }
                case "get_keyframes" -> {
                    String only = has(a, "part") ? part(a) : null, ch = str(a, "channel");
                    JsonObject out = new JsonObject();
                    emote.keys.forEach((p, channels) -> {
                        if (only != null && !only.equals(p)) return;
                        JsonObject po = new JsonObject();
                        channels.forEach((c, list) -> {
                            if (ch != null && !ch.equals(c)) return;
                            JsonArray rows = new JsonArray();
                            for (int i = 0; i < list.size(); i++) rows.add(keyJson(list.get(i), i > 0 && list.get(i - 1).custom()));
                            po.add(c, rows);
                        });
                        out.add(p, po);
                    });
                    return out;
                }
                case "get_pose" -> {
                    double t = has(a, "tick") ? need(a, "tick").getAsDouble() : time;
                    String only = has(a, "part") ? part(a) : null;
                    JsonObject out = new JsonObject();
                    for (String p : Emote.PARTS) {
                        if (only != null && !only.equals(p)) continue;
                        JsonObject po = new JsonObject();
                        for (Group g : Emote.groups(p)) for (String c : g.channels) po.addProperty(c, Preview.round(emote.value(p, c, t)));
                        out.add(p, po);
                    }
                    return out;
                }
                case "set_keyframe" -> {
                    String p = part(a); Group g = group(p, a); int t = has(a, "tick") ? num(a, "tick") : tick();
                    float[] v = emote.values(p, g, t);
                    if (a.get("values") instanceof JsonObject values) for (Map.Entry<String, JsonElement> e : values.entrySet()) {
                        int i = g.channels.indexOf(e.getKey());
                        if (i < 0) throw new IllegalArgumentException(g.title + " channels are " + g.channels + ", not " + e.getKey());
                        v[i] = e.getValue().getAsFloat();
                    }
                    emote.keyGroup(p, g, t, v);
                    if (has(a, "ease")) { String ease = ease(a); for (String c : g.channels) easeKey(emote.keys(p, c), emote.key(p, c, t), ease); }
                    expanded.add(p); picked.clear(); picked.add(new Dot(p, g, t));
                    changed(); inspect();
                }
                case "edit_channel_key" -> {
                    String p = part(a), c = str(a, "channel");
                    if (Emote.groups(p).stream().noneMatch(g -> g.channels.contains(c))) throw new IllegalArgumentException(p + " has no channel " + c);
                    Emote.Key k = emote.keyChannel(p, c, has(a, "tick") ? num(a, "tick") : tick());
                    if (has(a, "value")) k.value = need(a, "value").getAsFloat();
                    if (has(a, "turn")) k.turn = num(a, "turn");
                    if (has(a, "ease")) easeKey(emote.keys(p, c), k, ease(a));
                    if (has(a, "out_t")) k.outT = need(a, "out_t").getAsFloat();
                    if (has(a, "out_v")) k.outV = need(a, "out_v").getAsFloat();
                    if (has(a, "in_t")) k.inT = need(a, "in_t").getAsFloat();
                    if (has(a, "in_v")) k.inV = need(a, "in_v").getAsFloat();
                    if (has(a, "new_tick")) { k.tick = Math.max(0, num(a, "new_tick")); emote.retime(p, c, k); }
                    expanded.add(p);
                    changed(); inspect();
                    return keyJson(k, true);
                }
                case "remove_keyframe" -> {
                    String p = part(a); int t = num(a, "tick");
                    if (has(a, "channel")) emote.removeKey(p, str(a, "channel"), t);
                    else if (has(a, "group")) emote.removeGroup(p, group(p, a), t);
                    else for (Group g : Emote.groups(p)) emote.removeGroup(p, g, t);
                    picked.clear(); selectedKey = null;
                    changed(); inspect();
                }
                case "move_keyframes" -> {
                    int from = num(a, "from_tick"), to = Math.max(0, num(a, "to_tick"));
                    for (String p : parts(a)) for (Group g : Emote.groups(p))
                        if (!has(a, "group") || g == group(p, a)) emote.moveGroup(p, g, from, to);
                    picked.clear(); selectedKey = null;
                    changed(); inspect();
                }
                case "copy_keyframes" -> {
                    int from = num(a, "from_tick"), to = has(a, "to_tick") ? num(a, "to_tick") : from, at = Math.max(0, num(a, "at_tick"));
                    String target = has(a, "to_part") ? part(a, "to_part") : null;
                    if (target != null && !has(a, "part")) throw new IllegalArgumentException("to_part needs part");
                    List<Copied> copies = new ArrayList<>();
                    for (String p : parts(a)) for (Group g : Emote.groups(p)) for (int t : emote.ticks(p, g))
                        if (t >= from && t <= to) copies.add(new Copied(target == null ? p : target, g, t - from, emote.values(p, g, t), emote.ease(p, g, t)));
                    picked.clear();
                    for (Copied c : copies) {
                        if (!Emote.groups(c.part).contains(c.group)) continue;
                        emote.keyGroup(c.part, c.group, at + c.offset, c.values);
                        emote.setEase(c.part, c.group, at + c.offset, c.ease);
                        picked.add(new Dot(c.part, c.group, at + c.offset)); expanded.add(c.part);
                    }
                    changed(); inspect();
                    return "Copied " + picked.size() + " keyframe(s)";
                }
                case "key_whole_part" -> {
                    String p = part(a); int t = has(a, "tick") ? num(a, "tick") : tick();
                    picked.clear();
                    for (Group g : Emote.groups(p)) { emote.keyGroup(p, g, t, emote.values(p, g, t)); picked.add(new Dot(p, g, t)); }
                    expanded.add(p);
                    changed(); inspect();
                }
                case "set_time" -> { playing = false; setTime(need(a, "tick").getAsDouble()); }
                case "play" -> {
                    if (has(a, "speed")) speed = Math.clamp(need(a, "speed").getAsFloat(), .25F, 2F);
                    if (has(a, "loop")) loop = flag(a, "loop");
                    setPlaying(!has(a, "playing") || flag(a, "playing"));
                }
                case "select_part" -> {
                    String p = str(a, "part");
                    if (has(a, "channel")) channel = str(a, "channel");
                    selectPart(p == null || p.equals("none") ? null : part(a), flag(a, "bend"));
                    if (has(a, "graph") && flag(a, "graph") != graphOpen) toggleGraph();
                }
                case "undo", "redo" -> {
                    for (int i = has(a, "steps") ? num(a, "steps") : 1; i > 0; i--) if (tool.equals("undo")) undo(); else redo();
                }
                case "add_character" -> {
                    if (cast.size() >= MAX_CAST) throw new IllegalStateException("At most " + MAX_CAST + " characters");
                    if (has(a, "file")) addActor(EmoteStudioClient.read(str(a, "file")).copyAsNew()); else addActor();
                    return "Added character " + (cast.size() - 1);
                }
                case "remove_character" -> {
                    int i = character(a);
                    if (i == 0) throw new IllegalArgumentException("Character 0 (P1) is the main file and can't be removed");
                    dropActor(i);
                }
                case "edit_character" -> editActor(character(a));
                case "set_character" -> {
                    int i = character(a);
                    Look l = look(i);
                    if (has(a, "hidden")) l.hidden = flag(a, "hidden");
                    if (has(a, "slim")) l.slim = flag(a, "slim");
                    if (has(a, "skin")) {
                        String s = str(a, "skin");
                        if (s.equalsIgnoreCase("player")) l.skin = null;
                        else if (DEFAULT_SKINS.contains(s.toLowerCase(Locale.ROOT))) l.skin = s.toLowerCase(Locale.ROOT);
                        else if (skinFiles().contains(s)) l.skin = s;
                        else throw new IllegalArgumentException("Unknown skin " + s + "; options: player, " + DEFAULT_SKINS + ", " + skinFiles());
                    }
                    if (has(a, "right_hand")) l.right = Held.valueOf(str(a, "right_hand").toUpperCase(Locale.ROOT));
                    if (has(a, "left_hand")) l.left = Held.valueOf(str(a, "left_hand").toUpperCase(Locale.ROOT));
                    Emote e = cast.get(i);
                    float[] st = stage(e);
                    String[] axes = {"x", "y", "z", "yaw"};
                    for (int k = 0; k < 4; k++) if (has(a, axes[k])) { st[k] = need(a, axes[k]).getAsFloat(); setStage(e, st); }
                    if (has(a, "preset")) {
                        if (i == 0) throw new IllegalArgumentException("Presets place characters 1/2 relative to character 0");
                        ridePreset(i, switch (str(a, "preset")) {
                            case "carry_on" -> CARRY_ON; case "piggyback" -> PIGGYBACK;
                            default -> throw new IllegalArgumentException("preset is carry_on or piggyback");
                        });
                    }
                    changed(); inspect();
                }
                case "set_camera" -> {
                    if (flag(a, "reset")) preview.resetView();
                    if (has(a, "frame")) preview.cameraFrame = flag(a, "frame");
                    if (has(a, "focus")) { int f = num(a, "focus"); preview.focus = f < 0 || f >= cast.size() ? -1 : f; }
                    if (has(a, "yaw") || has(a, "pitch"))
                        preview.scene.setCameraYawAndPitch(has(a, "yaw") ? need(a, "yaw").getAsFloat() : 45, has(a, "pitch") ? need(a, "pitch").getAsFloat() : 15);
                    if (has(a, "zoom")) preview.scene.setZoom(need(a, "zoom").getAsFloat());
                    inspect();
                }
                case "take_picture" -> {
                    if (file == null) throw new IllegalStateException("Save the emote first (the picture is named after its file)");
                    takePicture();
                    return "Picture: emotes/" + String.join(", ", pictureFiles());
                }
                case "render_preview" -> {
                    if (has(a, "tick")) { playing = false; setTime(need(a, "tick").getAsDouble()); }
                    Path tmp = Files.createTempFile("emotestudio", ".png");
                    try { preview.capture(tmp); return Files.readAllBytes(tmp); }
                    finally { Files.deleteIfExists(tmp); }
                }
                default -> throw new IllegalArgumentException("Unknown tool " + tool);
            }
            return "OK · " + getTitle().getString() + " · tick " + tick();
        }
        JsonObject state() {
            JsonObject o = new JsonObject();
            o.addProperty("file", file);
            o.addProperty("dirty", dirty());
            o.addProperty("tick", time);
            o.addProperty("playing", playing);
            o.addProperty("speed", speed);
            o.addProperty("loop_playback", loop);
            o.addProperty("selected_part", part);
            o.addProperty("graph_open", graphOpen);
            o.addProperty("editing_character", actor);
            o.addProperty("can_undo", !undo.isEmpty());
            o.addProperty("can_redo", !redo.isEmpty());
            JsonObject s = new JsonObject();
            s.addProperty("name", emote.name);
            s.addProperty("description", emote.description);
            s.addProperty("author", emote.author);
            s.addProperty("begin_tick", emote.begin);
            s.addProperty("end_tick", emote.end);
            s.addProperty("stop_tick", emote.stop);
            s.addProperty("loop", emote.loop);
            s.addProperty("return_tick", emote.returnTick);
            s.addProperty("ease_before", emote.easeBefore);
            s.addProperty("hold", holds());
            o.add("emote", s);
            JsonArray animated = new JsonArray();
            for (String p : Emote.PARTS) if (emote.animated(p)) animated.add(p);
            o.add("animated_parts", animated);
            JsonArray chars = new JsonArray();
            for (int i = 0; i < cast.size(); i++) {
                Look l = look(i);
                JsonObject c = new JsonObject();
                c.addProperty("character", i);
                c.addProperty("name", cast.get(i).name);
                c.addProperty("file", file == null ? null : i == 0 ? file : partnerFile(file, i));
                c.addProperty("hidden", l.hidden);
                c.addProperty("skin", l.skin == null ? "player" : l.skin);
                c.addProperty("slim", l.slim);
                c.addProperty("right_hand", l.right.name());
                c.addProperty("left_hand", l.left.name());
                JsonArray st = new JsonArray();
                for (float v : stage(cast.get(i))) st.add(v);
                c.add("x_y_z_yaw", st);
                chars.add(c);
            }
            o.add("characters", chars);
            JsonObject cam = new JsonObject();
            cam.addProperty("frame", preview.cameraFrame);
            cam.addProperty("focus", preview.focus);
            o.add("camera", cam);
            JsonArray skins = new JsonArray();
            skins.add("player");
            DEFAULT_SKINS.forEach(skins::add);
            skinFiles().forEach(skins::add);
            o.add("skins", skins);
            return o;
        }
        static JsonObject keyJson(Emote.Key k, boolean in) {
            JsonObject o = new JsonObject();
            o.addProperty("tick", k.tick);
            o.addProperty("value", k.value);
            o.addProperty("ease", easeName(k.ease));
            if (k.turn != 0) o.addProperty("turn", k.turn);
            if (k.arg != null) o.addProperty("ease_arg", k.arg);
            if (k.custom()) { o.addProperty("out_t", k.outT); o.addProperty("out_v", k.outV); }
            if (in) { o.addProperty("in_t", k.inT); o.addProperty("in_v", k.inV); }
            return o;
        }
        int character(JsonObject a) {
            int i = has(a, "character") ? num(a, "character") : actor;
            if (i < 0 || i >= cast.size()) throw new IllegalArgumentException("character is 0.." + (cast.size() - 1));
            return i;
        }
        static String part(JsonObject a) { return part(a, "part"); }
        static String part(JsonObject a, String key) {
            String p = str(a, key);
            if (!Emote.PARTS.contains(p)) throw new IllegalArgumentException(key + " must be one of " + Emote.PARTS);
            return p;
        }
        static List<String> parts(JsonObject a) { return has(a, "part") ? List.of(part(a)) : Emote.PARTS; }
        static Group group(String p, JsonObject a) {
            Group g;
            try { g = Group.valueOf(str(a, "group").toUpperCase(Locale.ROOT)); }
            catch (RuntimeException e) { throw new IllegalArgumentException("group must be rotation, position, scale or bend"); }
            if (!Emote.groups(p).contains(g)) throw new IllegalArgumentException(p + " has no " + g.title + " group");
            return g;
        }
        static String ease(JsonObject a) {
            String e = str(a, "ease").toUpperCase(Locale.ROOT);
            if (!EASES.contains(e)) throw new IllegalArgumentException("ease must be one of " + EASES);
            return e;
        }
    }


    private static final class TimelineView extends View {
        final StudioEditor editor; final Label time;
        long lastFrame = System.nanoTime();
        final UIElement rulerBox = new UIElement();
        TimelineView(StudioEditor editor) {
            super("emote.timeline", new TextTexture("TL")); this.editor = editor;
            setCanRemove(false); setFloatable(false);
            getLayout().widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN).gapAll(2);
            UIElement tools = new UIElement().layout(l -> l.widthPercent(100).height(24).flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).gapAll(3));
            time = new Label(); time.layout(l -> l.width(130));
            IGuiTexture loopOn = Icons.REPLAY.copy().setColor(0xFF1F8F2E), loopOff = Icons.REPLAY.copy().setColor(0xFF3A3A3A);
            Button loop = icon((g, mx, my, x, y, w, h, pt) -> (editor.loop ? loopOn : loopOff).draw(g, mx, my, x, y, w, h, pt), () -> editor.loop = !editor.loop);
            Selector<String> speed = new Selector<>();
            speed.setCandidates(List.of("x0.25", "x0.5", "x1.0", "x1.5", "x2.0")).setSelected("x1.0", false)
                    .setOnValueChanged(v -> editor.speed = Float.parseFloat(v.substring(1)));
            speed.layout(l -> l.width(46).height(18));
            tools.addChildren(
                    icon(glyph(G_START), () -> { editor.playing = false; editor.setTime(0); }), icon(glyph(G_BACK), () -> editor.step(-1)),
                    icon((g, mx, my, x, y, w, h, pt) -> glyph(editor.playing ? G_PAUSE : G_PLAY).draw(g, mx, my, x, y, w, h, pt), () -> editor.setPlaying(!editor.playing)),
                    icon(glyph(G_FORWARD), () -> editor.step(1)), icon(glyph(G_END), () -> { editor.playing = false; editor.setTime(editor.emote.length()); }),
                    loop, speed, time,
                    new UIElement().layout(l -> l.flex(1)),
                    tool("ITEMS", 44, () -> editor.preview.showItems = !editor.preview.showItems), tool("GRAPH", 46, editor::toggleGraph), tool("KEY PART", 60, editor::keyPart), tool("DELETE", 48, editor::deletePicked), tool("COPY", 40, editor::copyKeys), tool("PASTE", 44, editor::pasteKeys),
                    tool("UNDO", 40, editor::undo), tool("REDO", 40, editor::redo),
                    tool("Fit", 28, this::fit), tool("−", 18, () -> zoom(-1)), tool("+", 18, () -> zoom(1)));
            addChild(tools);
            rulerBox.layout(l -> l.widthPercent(100).height(ROW_HEIGHT).flexShrink(0)).setOverflowVisible(false);
            addChild(rulerBox);
            editor.rows = new UIElement();
            ScrollerView scroller = new ScrollerView() {
                @Override protected void onHorizontalScroll(float value) { super.onHorizontalScroll(value); slideRuler(); }
            };
            editor.scroller = scroller;
            scroller.layout(l -> l.widthPercent(100).flex(1));
            scroller.scrollerStyle(s -> s.mode(ScrollerMode.BOTH).horizontalScrollDisplay(ScrollDisplay.AUTO));
            scroller.viewContainer.addChild(editor.rows);
            addChild(scroller);
            scroller.addEventListener(UIEvents.MOUSE_WHEEL, e -> {
                if (e.isCtrlDown()) { zoom(e.deltaY > 0 ? 1 : -1); e.stopPropagation(); }
                else if (e.isAltDown()) { scroller.horizontalScroller.scrollByWheel(e.deltaY); e.stopPropagation(); }
            }, true);
        }
        @Override public void drawBackgroundAdditional(GUIContext context) {
            super.drawBackgroundAdditional(context);
            long now = System.nanoTime();
            if (editor.playing) editor.advance(Math.min(5, (now - lastFrame) / 1e9 * 20 * editor.speed));
            lastFrame = now;
            if (editor.rulerRow != null && editor.rows != null) {
                float off = editor.rows.getPositionX() - editor.rulerRow.getPositionX();
                if (Math.abs(off) > .5F) { rulerFix += off; slideRuler(); }
            }

        }
        Button tool(String text, int width, Runnable action) {
            Button button = new Button().setText(text).setOnClick(e -> action.run());
            button.layout(l -> l.width(width).height(20));
            return button;
        }
        Button icon(IGuiTexture icon, Runnable action) {
            Button button = new Button().noText().addPreIcon(icon).setOnClick(e -> action.run());
            button.layout(l -> l.width(18).height(18).paddingAll(1));
            return button;
        }
        void fit() {
            float room = editor.scroller.getContentWidth() - LABEL_WIDTH - 12;
            editor.scale = Math.clamp(room / Math.max(1, editor.emote.length()), 2F, 40F);
            editor.rebuild();
        }
        float rulerFix;
        void slideRuler() {
            ScrollerView s = editor.scroller;
            if (editor.rulerRow == null || s == null) return;
            Float value = s.horizontalScroller.getValue();
            float left = rulerFix - (value == null ? 0 : value) * Math.max(0, s.getContainerWidth() - s.viewPort.getContentWidth());
            editor.rulerRow.layout(l -> l.left(left));
        }
        void zoom(float delta) { editor.scale = Math.clamp(editor.scale + delta, 2F, 40F); editor.rebuild(); }
        void refreshCursor() {
            if (editor.playhead != null) editor.playhead.layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
                    .left(LABEL_WIDTH + (float) editor.time * editor.scale).top(0).width(2).height((ROW_HEIGHT + 1) * editor.rowCount + (editor.graphShown ? GRAPH_HEIGHT + 1 : 0)));
            time.setText(String.format(Locale.ROOT, "%.2f / %.2fs  t%d", editor.time / 20.0, editor.emote.length() / 20.0, editor.tick()));
        }
    }

    private static final class PreviewView extends View {
        final Preview preview;
        PreviewView(StudioEditor editor) {
            super("PREVIEW"); setFloatable(false); setCanRemove(false);
            getLayout().widthPercent(100).heightPercent(100);
            preview = new Preview(editor);
            preview.layout(l -> l.widthPercent(100).heightPercent(100));
            addChild(preview);
        }
    }


    private static final class Preview extends SceneEditor {
        static final Set<String> POSABLE = Set.of("body", "head", "torso", "rightArm", "leftArm", "rightLeg", "leftLeg", "rightItem", "leftItem");
        static final float SCALE = 0.9375F, ORIGIN = 1.501F;
        static final Set<String> UPPER = Set.of("head", "rightArm", "leftArm");
        static final Map<String, int[]> SKIN = Map.of("head", new int[]{0, 0, 32, 0}, "torso", new int[]{16, 16, 16, 32},
                "rightArm", new int[]{40, 16, 40, 32}, "leftArm", new int[]{32, 48, 48, 48}, "rightLeg", new int[]{0, 16, 0, 32}, "leftLeg", new int[]{16, 48, 0, 48});
        final StudioEditor editor;
        final SceneObject handle = new SceneObject();
        final Map<String, Matrix4f> displays = new HashMap<>();
        boolean dragging;
        boolean showItems = true;
        boolean cameraFrame;
        int focus = -1;
        int tint = -1;
        static final int DIM = 0xFF4D4D4D;

        Preview(StudioEditor editor) {
            this.editor = editor;
            Minecraft mc = Minecraft.getInstance();
            scene.createScene(new TrackedDummyWorld()).setTickWorld(false).setRenderedCore(List.of(BlockPos.ZERO), null, false).useCacheBuffer();
            setId("emote.preview"); setOverflowVisible(false);
            topBar.setDisplay(false);
            resetView();
            UIElement frame = new UIElement().setAllowHitTest(false)
                    .layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(0).widthPercent(100).heightPercent(100));
            frame.style(st -> st.backgroundTexture((g, mx, my, x, y, w, h, pt) -> {
                if (!cameraFrame) return;
                float side = Math.min(w, h), fx = x + (w - side) / 2, fy = y + (h - side) / 2;
                DrawerHelper.drawSolidRect(g, fx, fy, side, 1, 0xFFFFD040); DrawerHelper.drawSolidRect(g, fx, fy + side - 1, side, 1, 0xFFFFD040);
                DrawerHelper.drawSolidRect(g, fx, fy, 1, side, 0xFFFFD040); DrawerHelper.drawSolidRect(g, fx + side - 1, fy, 1, side, 0xFFFFD040);
            }));
            addChild(frame);
            String[] tips = {"Move (V)", "Rotate (R)", "Scale (S)"};
            for (int i = 0; i < tips.length && i < gizmoBar.getChildren().size(); i++) {
                Component tip = Component.literal(tips[i]);
                gizmoBar.getChildren().get(i).style(st -> st.tooltips(tip));
            }
            scene.getRenderer().setBeforeBatchEnd((buffers, partial) -> drawSolid(new PoseStack(), buffers));
            addSceneObject(handle);
            addSceneObject(new Outline());
            addEventListener(UIEvents.MOUSE_UP, e -> { if (e.button == 0 && dragging) { dragging = false; editor.changed(); editor.inspect(); } }, true);
            setTransformGizmoMode(TransformGizmo.Mode.ROTATE);
        }

        void refresh() { if (!dragging) placeHandle(); }
        void resetView() { scene.setCenter(new Vector3f(0, 1, 0)).setCameraYawAndPitch(45, 15).setZoom(5); }


        record Piece(String part, boolean bend, Matrix4f toWorld, float[] box) {}
        float[] cuboid(String part) {
            return switch (part) {
                case "head" -> new float[]{-4, -8, -4, 4, 0, 4};
                case "torso" -> new float[]{-4, 0, -2, 4, 12, 2};
                case "rightArm" -> slim() ? new float[]{-2, -2, -2, 1, 10, 2} : new float[]{-3, -2, -2, 1, 10, 2};
                case "leftArm" -> slim() ? new float[]{-1, -2, -2, 2, 10, 2} : new float[]{-1, -2, -2, 3, 10, 2};
                default -> new float[]{-2, 0, -2, 2, 12, 2};
            };
        }
        Emote shown;
        Emote posed() { return shown == null ? editor.emote : shown; }
        Look look() { return editor.look(shown == null ? editor.actor : editor.cast.indexOf(shown)); }
        boolean slim() { return look().slim; }
        Held held(String p) {
            if (!showItems) return Held.NONE;
            return p.equals("leftItem") ? look().left : look().right;
        }
        Matrix4f display(Held h, boolean left) { return displays.computeIfAbsent(h.name() + left, k -> itemDisplay(h.stack, left)); }
        float[] values(String p, Group g) { return posed().values(p, g, editor.time); }
        static Quaternionf turn(float[] rot) { return new Quaternionf().rotationZYX(rad(rot[2]), rad(rot[1]), rad(rot[0])); }
        Matrix4f bodyFrame() {
            float[] pos = values("body", Group.POSITION);
            return parent("body").translate(pos[0], pos[1] + .7F, pos[2]).rotate(turn(values("body", Group.ROTATION)));
        }
        Matrix4f model() {
            return bodyFrame().translate(0, -.7F, 0).scale(-SCALE, -SCALE, SCALE).translate(0, -ORIGIN, 0).scale(1 / 16F);
        }
        Matrix4f parent(String p) {
            if (item(p)) return itemParent(p);
            if (p.equals("body")) {
                float[] sc = values("body", Group.SCALE), st = StudioEditor.stage(posed());
                return new Matrix4f().translate(st[0], st[1], st[2]).rotateY(rad(st[3])).rotateY((float) Math.PI).scale(sc[0], sc[1], sc[2]);
            }
            Matrix4f m = model();
            if (UPPER.contains(p)) {
                float[] b = values("body", Group.BEND);
                m.translate(0, 6, 0).rotate(rad(b[0]), bendAxis("torso", b[1])).translate(0, -6, 0);
            }
            return m;
        }
        Matrix4f partMatrix(String p) { return partMatrix(p, 0); }
        Matrix4f partMatrix(String p, float dx) {
            if (p.equals("body")) return bodyFrame();
            float[] pos = values(p, Group.POSITION);
            Matrix4f m = parent(p).translate(pos[0] + dx, pos[1], pos[2]).rotate(turn(values(p, Group.ROTATION)));
            if (item(p)) return m;
            float[] sc = values(p, Group.SCALE);
            return m.scale(sc[0], sc[1], sc[2]);
        }
        Matrix4f itemParent(String p) {
            boolean left = p.equals("leftItem");
            String arm = left ? "leftArm" : "rightArm";
            float[] bend = values(arm, Group.BEND), scale = values(p, Group.SCALE);
            float a = -rad(bend[1]);
            return partMatrix(arm, slim() ? (left ? -.5F : .5F) : 0).translate(0, 4, 0).rotate(rad(bend[0]), new Vector3f((float) Math.cos(a), 0, (float) Math.sin(a))).translate(0, -4, 0)
                    .rotateX(rad(-90)).rotateY((float) Math.PI).translate(left ? -1 : 1, 2, -10).scale(scale[0], scale[1], scale[2]);
        }
        static boolean item(String p) { return p.endsWith("Item"); }
        static Vector3f bendAxis(String p, float axisDegrees) {
            float a = rad(axisDegrees);
            return new Vector3f((float) Math.cos(a), 0, (p.equals("torso") ? -1 : 1) * (float) Math.sin(a));
        }
        static Vector3f middle(float[] b) { return new Vector3f((b[0] + b[3]) / 2, (b[1] + b[4]) / 2, (b[2] + b[5]) / 2); }
        float[] bendOf(String p) {
            float[] b = values(p, Group.BEND);
            if (!p.equals("torso")) return b;
            float[] body = values("body", Group.BEND);
            return new float[]{b[0] + body[0], b[1] + body[1]};
        }
        Matrix4f[] halves(String p) {
            Matrix4f m = partMatrix(p);
            if (!Emote.groups(p).contains(Group.BEND)) return new Matrix4f[]{m, m, m};
            float[] bend = bendOf(p);
            Vector3f c = middle(cuboid(p)), axis = bendAxis(p, bend[1]);
            return new Matrix4f[]{m, new Matrix4f(m).translate(c).rotate(rad(bend[0]) / 2, axis).translate(-c.x, -c.y, -c.z),
                    new Matrix4f(m).translate(c).rotate(rad(bend[0]), axis).translate(-c.x, -c.y, -c.z)};
        }
        static final float[] WAIST = {-4.5F, -1, -2.5F, 4.5F, 1, 2.5F};
        List<Piece> pieces(String p) {
            if (p.equals("body")) return List.of(new Piece(p, false, bodyFrame().scale(1 / 16F), WAIST));
            if (item(p)) {
                Held h = held(p);
                if (h == Held.NONE) return List.of();
                Matrix4f m = partMatrix(p).scale(16).mul(display(h, p.equals("leftItem"))).translate(8, 8, 0);
                return List.of(switch (h) {
                    case SWORD -> new Piece(p, false, m.rotateZ(rad(45)), SWORD);
                    case PAPER -> new Piece(p, false, m, PAPER);
                    default -> new Piece(p, false, m, BLOCK);
                });
            }
            Matrix4f[] h = halves(p);
            float[] b = cuboid(p);
            if (!Emote.groups(p).contains(Group.BEND)) return List.of(new Piece(p, false, h[0], b));
            float mid = (b[1] + b[4]) / 2;
            boolean torso = p.equals("torso");
            float[] fixed = torso ? new float[]{b[0], mid, b[2], b[3], b[4], b[5]} : new float[]{b[0], b[1], b[2], b[3], mid, b[5]};
            float[] moving = torso ? new float[]{b[0], b[1], b[2], b[3], mid, b[5]} : new float[]{b[0], mid, b[2], b[3], b[4], b[5]};
            return List.of(new Piece(p, false, h[0], fixed), new Piece(p, true, h[2], moving));
        }

        record Hit(String part, boolean bend, float distance, int actor) {}
        static String shape(String p) { return p.equals("body") ? "torso" : p; }
        Hit pick(Ray ray) {
            Hit best = null;
            for (int i = 0; i < editor.cast.size(); i++) {
                if (editor.look(i).hidden) continue;
                shown = editor.cast.get(i);
                for (String p : POSABLE) for (Piece piece : pieces(p)) {
                    Matrix4f toLocal = new Matrix4f(piece.toWorld).invert();
                    Vector3f a = toLocal.transformPosition(new Vector3f(ray.startPos())), b = toLocal.transformPosition(new Vector3f(ray.endPos()));
                    float t = slab(a, b, piece.box, .5F);
                    if (t >= 0 && (best == null || t < best.distance)) best = new Hit(p.equals("torso") && piece.bend ? "body" : p, piece.bend, t, i);
                }
            }
            shown = null;
            return best;
        }
        static float slab(Vector3f a, Vector3f b, float[] box, float grow) {
            float t0 = 0, t1 = 1;
            for (int i = 0; i < 3; i++) {
                float o = a.get(i), d = b.get(i) - o, lo = box[i] - grow, hi = box[i + 3] + grow;
                if (Math.abs(d) < 1e-9F) { if (o < lo || o > hi) return -1; continue; }
                float u = (lo - o) / d, v = (hi - o) / d;
                t0 = Math.max(t0, Math.min(u, v)); t1 = Math.min(t1, Math.max(u, v));
                if (t0 > t1) return -1;
            }
            return t0;
        }

        @Override protected void onMouseDown(UIEvent event) {
            if (event.button == 0 && event.target == scene) {
                Ray ray = getMouseRay().orElse(null);
                Hit hit = ray == null ? null : pick(ray);
                TransformGizmo.Handle on = ray != null && transformGizmo.isActive() ? transformGizmo.pickHandle(ray) : null;
                boolean other = hit != null && !(hit.actor == editor.actor && hit.part.equals(editor.part) && hit.bend == editor.bendSelected);
                if (other && (on == null || on == TransformGizmo.Handle.TRACKBALL)) {
                    editor.editActor(hit.actor);
                    editor.selectPart(hit.part, hit.bend);
                    event.stopPropagation();
                    return;
                }
            }
            super.onMouseDown(event);
        }

        private final class Outline extends SceneObject implements ISceneRendering {
            @Override public void drawInternal(PoseStack ps, MultiBufferSource buffers, float partial) {
                String p = editor.part;
                VertexConsumer lines = buffers.getBuffer(RenderType.lines());
                boolean bodyBend = "body".equals(p) && editor.bendSelected;
                if (!"body".equals(p) || bodyBend) box(ps, lines, pieces("body").getFirst(), 0x66FFFFFF);
                if (bodyBend) p = "torso";
                if (p == null || !POSABLE.contains(p)) return;
                for (Piece piece : pieces(p)) box(ps, lines, piece, piece.bend == editor.bendSelected ? 0xFFFFD040 : 0xAAFFFFFF);
            }
        }
        static void box(PoseStack ps, VertexConsumer lines, Piece piece, int color) {
            float[] b = piece.box;
            Vector3f[] c = new Vector3f[8];
            for (int i = 0; i < 8; i++)
                c[i] = piece.toWorld.transformPosition(new Vector3f((i & 1) == 0 ? b[0] - .3F : b[3] + .3F,
                        (i & 2) == 0 ? b[1] - .3F : b[4] + .3F, (i & 4) == 0 ? b[2] - .3F : b[5] + .3F));
            int[][] edges = {{0, 1}, {2, 3}, {4, 5}, {6, 7}, {0, 2}, {1, 3}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
            for (int[] e : edges) line(ps, lines, c[e[0]], c[e[1]], color);
        }
        static void line(PoseStack pose, VertexConsumer lines, Vector3f a, Vector3f b, int color) {
            Vector3f n = new Vector3f(b).sub(a).normalize();
            lines.addVertex(pose.last(), a.x, a.y, a.z).setColor(color).setNormal(pose.last(), n.x, n.y, n.z);
            lines.addVertex(pose.last(), b.x, b.y, b.z).setColor(color).setNormal(pose.last(), n.x, n.y, n.z);
        }

        void placeHandle() {
            String p = editor.part;
            if (p == null || !POSABLE.contains(p) && !p.equals("body")) { setTransformGizmoTarget((com.lowdragmc.lowdraglib2.math.Transform) null); return; }
            Matrix4f m = partMatrix(p);
            if (editor.bendSelected) {
                float[] bend = values(p, Group.BEND);
                Matrix4f joint = partMatrix(shape(p));
                handle.transform().position(joint.transformPosition(middle(cuboid(shape(p)))));
                handle.transform().rotation(joint.getUnnormalizedRotation(new Quaternionf()).mul(new Quaternionf().rotationAxis(rad(bend[0]), bendAxis(shape(p), bend[1]))));
                if (getTransformGizmoMode() != TransformGizmo.Mode.ROTATE) setTransformGizmoMode(TransformGizmo.Mode.ROTATE);
            } else {
                handle.transform().position(m.transformPosition(p.equals("torso") ? new Vector3f(0, WAIST_Y, 0) : new Vector3f()));
                handle.transform().rotation(m.getUnnormalizedRotation(new Quaternionf()));
            }
            handle.transform().localScale(editor.bendSelected ? new Vector3f(1) : new Vector3f(values(p, Group.SCALE)));
            setTransformGizmoTarget(handle.transform(), this::handleMoved);
        }

        void handleMoved() {
            String p = editor.part;
            if (p == null) return;
            if (!dragging) {
                dragging = true; editor.playing = false; editor.time = editor.tick();
                for (String c : List.of("torso", "head", "rightArm", "leftArm")) dragStart.put(c, local(c));
            }
            int tick = editor.tick();
            if (getTransformGizmoMode() == TransformGizmo.Mode.SCALE && !editor.bendSelected) {
                Vector3f sc = handle.transform().localScale();
                editor.emote.keyGroup(p, Group.SCALE, tick, new float[]{round(sc.x), round(sc.y), round(sc.z)});
            } else if (editor.bendSelected) {
                Quaternionf rel = partMatrix(shape(p)).getUnnormalizedRotation(new Quaternionf()).conjugate().mul(new Quaternionf(handle.transform().rotation()));
                AxisAngle4f aa = new AxisAngle4f(rel);
                float angle = aa.angle > Math.PI ? aa.angle - (float) (2 * Math.PI) : aa.angle;
                float ax = aa.x, az = (shape(p).equals("torso") ? -1 : 1) * aa.z, flat = (float) Math.sqrt(ax * ax + az * az);
                float[] old = values(p, Group.BEND);
                float axis = flat < 1e-4F ? old[1] : (float) Math.toDegrees(Math.atan2(az, ax)), bend = (float) Math.toDegrees(angle) * flat;
                float a1 = near(rad(axis), old[1]), a2 = near(rad(axis + 180), old[1]);
                boolean flip = Math.abs(a2 - old[1]) < Math.abs(a1 - old[1]);
                editor.emote.keyGroup(p, Group.BEND, tick, new float[]{round(flip ? -bend : bend), flip ? a2 : a1});
            } else if (p.equals("torso")) {
                float[] rot = values(p, Group.ROTATION);
                Matrix4f start = dragStart.get("torso");
                Vector3f waist = start.transformPosition(new Vector3f(0, WAIST_Y, 0));
                float[] turned = rot;
                if (getTransformGizmoMode() == TransformGizmo.Mode.ROTATE) {
                    Vector3f e = parent(p).getUnnormalizedRotation(new Quaternionf()).conjugate().mul(new Quaternionf(handle.transform().rotation())).getEulerAnglesZYX(new Vector3f());
                    turned = new float[]{near(e.x, rot[0]), near(e.y, rot[1]), near(e.z, rot[2])};
                } else if (getTransformGizmoMode() == TransformGizmo.Mode.TRANSLATE) {
                    waist = parent(p).invert().transformPosition(new Vector3f(handle.transform().position()));
                } else return;
                Vector3f top = waist.sub(turn(turned).transform(new Vector3f(0, WAIST_Y, 0)));
                editor.emote.keyGroup(p, Group.ROTATION, tick, turned);
                editor.emote.keyGroup(p, Group.POSITION, tick, new float[]{round(top.x), round(top.y), round(top.z)});
                carry(local("torso").mul(new Matrix4f(start).invert()), tick);
            } else if (getTransformGizmoMode() == TransformGizmo.Mode.ROTATE) {
                Vector3f e = parent(p).getUnnormalizedRotation(new Quaternionf()).conjugate().mul(new Quaternionf(handle.transform().rotation())).getEulerAnglesZYX(new Vector3f());
                float[] old = values(p, Group.ROTATION);
                editor.emote.keyGroup(p, Group.ROTATION, tick, new float[]{near(e.x, old[0]), near(e.y, old[1]), near(e.z, old[2])});
            } else if (getTransformGizmoMode() == TransformGizmo.Mode.TRANSLATE) {
                Vector3f l = parent(p).invert().transformPosition(new Vector3f(handle.transform().position()));
                if (p.equals("body")) l.y -= .7F;
                editor.emote.keyGroup(p, Group.POSITION, tick, new float[]{round(l.x), round(l.y), round(l.z)});
            } else return;
            editor.expanded.add(p);
            refresh();
        }
        static final float WAIST_Y = 12;
        final Map<String, Matrix4f> dragStart = new HashMap<>();
        Matrix4f local(String p) {
            float[] pos = values(p, Group.POSITION);
            return new Matrix4f().translate(pos[0], pos[1], pos[2]).rotate(turn(values(p, Group.ROTATION)));
        }
        void carry(Matrix4f delta, int tick) {
            float[] b = values("body", Group.BEND);
            Matrix4f bend = new Matrix4f().translate(0, 6, 0).rotate(rad(b[0]), bendAxis("torso", b[1])).translate(0, -6, 0);
            Matrix4f d = new Matrix4f(bend).invert().mul(delta).mul(bend);
            for (String c : UPPER) {
                Matrix4f f = new Matrix4f(d).mul(dragStart.get(c));
                Vector3f at = f.getTranslation(new Vector3f()), e = f.getUnnormalizedRotation(new Quaternionf()).getEulerAnglesZYX(new Vector3f());
                float[] rot = values(c, Group.ROTATION);
                editor.emote.keyGroup(c, Group.POSITION, tick, new float[]{round(at.x), round(at.y), round(at.z)});
                editor.emote.keyGroup(c, Group.ROTATION, tick, new float[]{near(e.x, rot[0]), near(e.y, rot[1]), near(e.z, rot[2])});
            }
        }
        static float rad(float degrees) { return (float) Math.toRadians(degrees); }
        static float round(float v) { return Math.round(v * 100) / 100F; }
        static float near(float radians, float previous) {
            float d = (float) Math.toDegrees(radians);
            return round(d + 360 * Math.round((previous - d) / 360));
        }

        void drawSolid(PoseStack ps, MultiBufferSource buffers) {
            drawFloor(ps, buffers);
            drawCharacters(ps, buffers);
        }
        void drawCharacters(PoseStack ps, MultiBufferSource buffers) {
            com.mojang.blaze3d.platform.Lighting.setupLevel();
            for (int i = 0; i < editor.cast.size(); i++) {
                Look look = editor.look(i);
                if (look.hidden) continue;
                shown = editor.cast.get(i);
                tint = focus >= 0 && focus != i ? DIM : -1;
                MultiBufferSource items = tint == -1 ? buffers : type -> new Dimmed(buffers.getBuffer(type));
                ResourceLocation skin = editor.skinTexture(look);
                VertexConsumer base = buffers.getBuffer(RenderType.entityCutoutNoCull(skin));
                SKIN.forEach((p, uv) -> drawBox(base, ps.last(), p, 0, uv[0], uv[1]));
                VertexConsumer outer = buffers.getBuffer(RenderType.entityTranslucent(skin));
                SKIN.forEach((p, uv) -> drawBox(outer, ps.last(), p, p.equals("head") ? .5F : .25F, uv[2], uv[3]));
                drawItem(ps, items, "rightItem");
                drawItem(ps, items, "leftItem");
            }
            shown = null; tint = -1;
        }
        void capture(Path out) throws IOException {
            int size = 512;
            var renderer = scene.getRenderer();
            TextureTarget target = new TextureTarget(size, size, true, Minecraft.ON_OSX);
            NativeImage full = new NativeImage(size, size, false);
            try {
                target.setClearColor(0, 0, 0, 0);
                target.clear(Minecraft.ON_OSX);
                target.bindWrite(true);
                Vector3f eye = renderer.getEyePos(), at = renderer.getLookAt(), up = renderer.getWorldUp();
                RenderSystem.backupProjectionMatrix();
                RenderSystem.setProjectionMatrix(new Matrix4f().setPerspective(rad(renderer.getFov()), 1, .1F, 1000), VertexSorting.byDistance(eye));
                Matrix4fStack view = RenderSystem.getModelViewStack();
                view.pushMatrix();
                view.identity().lookAt(eye.x, eye.y, eye.z, at.x, at.y, at.z, up.x, up.y, up.z);
                RenderSystem.applyModelViewMatrix();
                RenderSystem.enableDepthTest();
                MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
                drawCharacters(new PoseStack(), buffers);
                buffers.endBatch();
                view.popMatrix();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.restoreProjectionMatrix();
                RenderSystem.bindTexture(target.getColorTextureId());
                full.downloadTexture(0, false);
                full.flipY();
            } finally {
                target.destroyBuffers();
                Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            }
            try (full; NativeImage icon = new NativeImage(size / 2, size / 2, false)) {
                for (int y = 0; y < size / 2; y++) for (int x = 0; x < size / 2; x++) {
                    int[] sum = new int[4];
                    for (int d = 0; d < 4; d++) {
                        int c = full.getPixelRGBA(2 * x + (d & 1), 2 * y + (d >> 1));
                        for (int ch = 0; ch < 4; ch++) sum[ch] += c >>> (8 * ch) & 0xFF;
                    }
                    int c = 0;
                    for (int ch = 0; ch < 4; ch++) c |= (sum[ch] / 4) << (8 * ch);
                    icon.setPixelRGBA(x, y, c);
                }
                icon.writeToFile(out);
            }
        }
        static final int[][] FACES = {{5, 4, 0, 1}, {3, 2, 6, 7}, {0, 4, 6, 2}, {1, 0, 2, 3}, {5, 1, 3, 7}, {4, 5, 7, 6}};
        static final float[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}, {0, 0, 1}};
        void drawBox(VertexConsumer vc, PoseStack.Pose pose, String p, float grow, int u, int v) {
            float[] b = cuboid(p);
            Matrix4f[] h = halves(p);
            boolean torso = p.equals("torso"), bends = Emote.groups(p).contains(Group.BEND);
            float w = b[3] - b[0], ht = b[4] - b[1], d = b[5] - b[2], mid = (b[1] + b[4]) / 2, y0 = b[1] - grow, y1 = b[4] + grow;
            float[][] rect = {{u + d, v, u + d + w, v + d}, {u + d + w, v + d, u + d + 2 * w, v}, {u, v + d, u + d, v + d + ht},
                    {u + d, v + d, u + d + w, v + d + ht}, {u + d + w, v + d, u + 2 * d + w, v + d + ht}, {u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + ht}};
            float cut = bends ? (mid - y0) / (y1 - y0) : 1;
            for (int f = 0; f < 6; f++)
                for (float[] span : f < 2 ? new float[][]{{0, 1}} : new float[][]{{0, cut}, {cut, 1}}) {
                    if (span[1] - span[0] < 1e-4F) continue;
                    for (int i = 0; i < 4; i++) {
                        int c = FACES[f][i];
                        float fy = (c & 2) == 0 ? span[0] : span[1], y = y0 + fy * (y1 - y0);
                        Matrix4f m = Math.abs(y - mid) < 1e-4F ? h[1] : (y < mid) == torso ? h[2] : h[0];
                        Vector3f at = m.transformPosition(new Vector3f((c & 1) == 0 ? b[0] - grow : b[3] + grow, y, (c & 4) == 0 ? b[2] - grow : b[5] + grow));
                        Vector3f n = m.transformDirection(new Vector3f(NORMALS[f])).normalize();
                        float tu = i == 0 || i == 3 ? rect[f][2] : rect[f][0];
                        float tv = f < 2 ? (i < 2 ? rect[f][1] : rect[f][3]) : rect[f][1] + fy * (rect[f][3] - rect[f][1]);
                        vc.addVertex(pose, at.x, at.y, at.z).setColor(tint).setUv(tu / 64, tv / 64).setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, n.x, n.y, n.z);
                    }
                }
        }
        static final float[] SWORD = {-10.5F, -2.5F, 7.5F, 10.5F, 2.5F, 8.5F};
        static final float[] PAPER = {-7, -7, 7.5F, 7, 7, 8.5F}, BLOCK = {-8, -8, 0, 8, 8, 16};
        static Matrix4f itemDisplay(ItemStack stack, boolean left) {
            Minecraft mc = Minecraft.getInstance();
            PoseStack pose = new PoseStack();
            mc.getItemRenderer().getModel(stack, mc.level, null, 0)
                    .applyTransform(left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, pose, left);
            return new Matrix4f(pose.last().pose()).translate(-.5F, -.5F, -.5F).scale(1 / 16F);
        }
        void drawItem(PoseStack ps, MultiBufferSource buffers, String p) {
            ItemStack stack = held(p).stack;
            if (stack.isEmpty()) return;
            boolean left = p.equals("leftItem");
            ps.pushPose();
            ps.mulPose(partMatrix(p).scale(16));
            Minecraft mc = Minecraft.getInstance();
            mc.getItemRenderer().renderStatic(null, stack, left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    left, ps, buffers, mc.level, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            ps.popPose();
        }
        static void drawFloor(PoseStack pose, MultiBufferSource buffers) {
            VertexConsumer quads = buffers.getBuffer(RenderType.debugQuads());
            for (int i = -ARENA; i <= ARENA; i++) {
                float c = i == 0 ? .6F : .22F, half = i == 0 ? .02F : .01F;
                strip(pose, quads, -ARENA, i - half, ARENA, i + half, c);
                strip(pose, quads, i - half, -ARENA, i + half, ARENA, c);
            }
        }
        static void strip(PoseStack pose, VertexConsumer quads, float x0, float z0, float x1, float z1, float c) {
            for (float[] v : new float[][]{{x0, z0}, {x1, z0}, {x1, z1}, {x0, z1}})
                quads.addVertex(pose.last(), v[0], .002F, v[1]).setColor(c, c, c, 1F);
        }

        record Dimmed(VertexConsumer to) implements VertexConsumer {
            @Override public VertexConsumer addVertex(float x, float y, float z) { to.addVertex(x, y, z); return this; }
            @Override public VertexConsumer setColor(int r, int g, int b, int a) { to.setColor(r * 3 / 10, g * 3 / 10, b * 3 / 10, a); return this; }
            @Override public VertexConsumer setUv(float u, float v) { to.setUv(u, v); return this; }
            @Override public VertexConsumer setUv1(int u, int v) { to.setUv1(u, v); return this; }
            @Override public VertexConsumer setUv2(int u, int v) { to.setUv2(u, v); return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { to.setNormal(x, y, z); return this; }
        }

        @Override public void initTopBar() {}
    }

    static IGuiTexture fill(int color) { return (g, mx, my, x, y, w, h, pt) -> DrawerHelper.drawSolidRect(g, x, y, w, h, color); }

    private static final int G_START = 0, G_BACK = 1, G_PLAY = 2, G_PAUSE = 3, G_FORWARD = 4, G_END = 5;
    static IGuiTexture glyph(int kind) {
        return (g, mx, my, x, y, w, h, pt) -> {
            int c = 0xFF3A3A3A; float cx = Math.round(x + w / 2), cy = Math.round(y + h / 2);
            java.util.function.DoubleConsumer right = x0 -> { for (int i = 0; i < 4; i++) DrawerHelper.drawSolidRect(g, (float) x0 + i, cy - 3 + i, 1, 7 - 2 * i, c); };
            java.util.function.DoubleConsumer left = x0 -> { for (int i = 0; i < 4; i++) DrawerHelper.drawSolidRect(g, (float) x0 + i, cy - i, 1, 2 * i + 1, c); };
            java.util.function.DoubleConsumer bar = x0 -> DrawerHelper.drawSolidRect(g, (float) x0, cy - 3, 1, 7, c);
            switch (kind) {
                case G_START -> { bar.accept(cx - 3); left.accept(cx - 1); }
                case G_BACK -> { left.accept(cx - 3); bar.accept(cx + 2); }
                case G_PLAY -> right.accept(cx - 2);
                case G_PAUSE -> { DrawerHelper.drawSolidRect(g, cx - 3, cy - 3, 2, 7, c); DrawerHelper.drawSolidRect(g, cx + 1, cy - 3, 2, 7, c); }
                case G_FORWARD -> { bar.accept(cx - 3); right.accept(cx - 1); }
                default -> { right.accept(cx - 3); bar.accept(cx + 2); }
            }
        };
    }
}
