package com.zengui;

import net.minecraft.class_437;   // Screen
import net.minecraft.class_332;   // DrawContext
import net.minecraft.class_327;   // TextRenderer
import net.minecraft.class_310;   // MinecraftClient
import net.minecraft.class_2561;  // Text
import net.minecraft.class_11908; // MouseClickEvent
import net.minecraft.class_11905; // MouseScrollEvent

import com.google.gson.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Zen config GUI — stella-style horizontal panel layout with Catppuccin Mocha palette.
 * One panel per category, laid out side-by-side. Pan horizontally with scroll wheel;
 * hover a panel and scroll to scroll its module list vertically.
 */
public class ZenConfigScreen extends class_437 {

    private final File configFile;
    private final JsonObject config;
    private List<String> categories;

    // Per-panel vertical scroll offsets
    private int[] scrollOffsets;
    // Horizontal panning offset for all panels
    private int panelScrollX = 0;
    // Last known mouse position (updated in render) for scroll direction
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    // Debug overlay (shows which click event path fired)
    private String dbgLast = "";
    private long dbgMs = 0L;

    // ── Catppuccin Mocha palette ──────────────────────────────────
    static final int CRUST    = 0xFF11111B;
    static final int BASE     = 0xFF1E1E2E;
    static final int MANTLE   = 0xFF181825;
    static final int SURFACE0 = 0xFF313244;
    static final int SURFACE1 = 0xFF45475A;
    static final int SURFACE2 = 0xFF585B70;
    static final int TEXT_C   = 0xFFCDD6F4;
    static final int SUBTEXT1 = 0xFFBAC2DE;
    static final int SUBTEXT0 = 0xFFA6ADC8;
    static final int GREEN    = 0xFFA6E3A1;
    static final int MAUVE    = 0xFFCBA6F7;
    static final int WHITE    = 0xFFFFFFFF;

    // ── Layout (mirrors stella constants) ────────────────────────
    static final int PANEL_W   = 160; // panel width
    static final int PANEL_GAP = 20;  // gap between panels
    static final int PANEL_Y   = 46;  // top edge of panels
    static final int HEADER_H  = 28;  // panel header height
    static final int ROW_H     = 20;  // module row height
    static final int TOGGLE_W  = 28;  // toggle pill width
    static final int TOGGLE_H  = 10;  // toggle pill height
    static final int MARGIN    = 6;   // inner horizontal padding
    static final int START_X   = 20;  // first panel x offset

    // ── Constructor ───────────────────────────────────────────────
    public ZenConfigScreen(File configFile, JsonObject config) {
        super(class_2561.method_43470("Zen Config"));
        this.configFile = configFile;
        this.config     = config;
        JsonObject cats = config.getAsJsonObject("categories");
        this.categories  = cats != null ? new ArrayList<>(cats.keySet()) : new ArrayList<>();
        this.scrollOffsets = new int[this.categories.size()];
    }

    // ── Render ────────────────────────────────────────────────────
    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        int W = this.field_22789;
        int H = this.field_22790;
        class_327 tr = this.field_22793;

        // Full-screen background
        ctx.method_25294(0, 0, W, H, CRUST);

        // Title bar (Mantle strip at top, like stella's header)
        ctx.method_25294(0, 0, W, PANEL_Y - 6, MANTLE);
        String title = "Zen Config";
        ctx.method_25303(tr, title, (W - tr.method_1727(title)) / 2, (PANEL_Y - 6 - 8) / 2, TEXT_C);

        // Hint text at bottom
        String hint = "Scroll: pan panels  |  Hover panel + scroll: module list  |  ESC: save & close";
        ctx.method_25303(tr, hint, MARGIN, H - 10, SURFACE2);

        // Debug overlay (fades after 4 s)
        if (System.currentTimeMillis() - dbgMs < 4000L) {
            ctx.method_25294(0, 0, W, 12, 0xCC000000);
            ctx.method_25303(tr, dbgLast, 2, 2, 0xFFFF5555);
        }

        // ── Panels ───────────────────────────────────────────────
        JsonObject cats = config.getAsJsonObject("categories");
        if (cats == null) return;

        for (int i = 0; i < categories.size(); i++) {
            String catName = categories.get(i);
            int panelX = START_X + (PANEL_W + PANEL_GAP) * i + panelScrollX;

            // Skip panels entirely off-screen
            if (panelX + PANEL_W < 0 || panelX > W) continue;

            JsonObject catObj = cats.getAsJsonObject(catName);
            List<Map.Entry<String, JsonElement>> modules = getModuleEntries(catObj);

            int maxContentH = H - PANEL_Y - HEADER_H - 20;
            int visibleContentH = Math.max(0, Math.min(modules.size() * ROW_H, maxContentH));
            int panelH = HEADER_H + visibleContentH;

            // Panel header — Mantle background
            ctx.method_25294(panelX, PANEL_Y, panelX + PANEL_W, PANEL_Y + HEADER_H, MANTLE);
            // Thin accent line under header (Mauve)
            ctx.method_25294(panelX, PANEL_Y + HEADER_H - 1, panelX + PANEL_W, PANEL_Y + HEADER_H, MAUVE);
            // Category name centered in header
            int nameW = tr.method_1727(catName);
            ctx.method_25303(tr, catName, panelX + (PANEL_W - nameW) / 2, PANEL_Y + (HEADER_H - 8) / 2, TEXT_C);

            // Panel body — Base background
            int bodyTop    = PANEL_Y + HEADER_H;
            int bodyBottom = PANEL_Y + panelH;
            ctx.method_25294(panelX, bodyTop, panelX + PANEL_W, bodyBottom, BASE);

            // Module rows
            int scroll = scrollOffsets[i];
            for (int j = 0; j < modules.size(); j++) {
                Map.Entry<String, JsonElement> me = modules.get(j);
                String modName = me.getKey();
                if (!me.getValue().isJsonObject()) continue;
                JsonObject module = me.getValue().getAsJsonObject();

                int rowY = bodyTop + j * ROW_H - scroll;
                // Skip rows outside visible body area
                if (rowY + ROW_H <= bodyTop || rowY >= bodyBottom) continue;

                // Row background (alternating Surface0 / Base)
                int rowBg = (j % 2 == 0) ? SURFACE0 : BASE;
                int clampTop = Math.max(rowY, bodyTop);
                int clampBot = Math.min(rowY + ROW_H, bodyBottom);
                ctx.method_25294(panelX, clampTop, panelX + PANEL_W, clampBot, rowBg);

                // Only draw text/toggle for fully visible rows (no partial rendering)
                if (rowY < bodyTop || rowY + ROW_H > bodyBottom) continue;

                // Module name — truncated if too long
                String displayName = tr.method_1727(modName) > (PANEL_W - TOGGLE_W - MARGIN * 3 - 4)
                        ? truncate(tr, modName, PANEL_W - TOGGLE_W - MARGIN * 3 - 4)
                        : modName;
                ctx.method_25303(tr, displayName, panelX + MARGIN, rowY + (ROW_H - 8) / 2, SUBTEXT1);

                // Toggle pill
                String mainKey = getFirstBoolKey(module);
                if (mainKey != null) {
                    boolean on = module.get(mainKey).getAsBoolean();
                    int tbX = panelX + PANEL_W - MARGIN - TOGGLE_W;
                    int tbY = rowY + (ROW_H - TOGGLE_H) / 2;
                    // Pill background
                    ctx.method_25294(tbX, tbY, tbX + TOGGLE_W, tbY + TOGGLE_H, on ? GREEN : SURFACE2);
                    // Knob (white square inside pill)
                    int knobSize = TOGGLE_H - 4;
                    int knobX = on ? tbX + TOGGLE_W - knobSize - 2 : tbX + 2;
                    ctx.method_25294(knobX, tbY + 2, knobX + knobSize, tbY + 2 + knobSize, WHITE);
                }
            }
        }
    }

    // ── Mouse click (primary event path) ─────────────────────────
    @Override
    public boolean method_25404(net.minecraft.class_11908 event) {
        int mx     = event.comp_4795();
        int my     = event.comp_4796();
        int button = event.comp_4797();
        lastMouseX = mx;
        lastMouseY = my;
        if (button == 0) {
            if (handleModuleClick(mx, my)) {
                dbgLast = "25404 toggle: " + mx + "," + my;
                dbgMs = System.currentTimeMillis();
                return true;
            }
            dbgLast = "25404 miss: " + mx + "," + my;
            dbgMs = System.currentTimeMillis();
        }
        return super.method_25404(event);
    }

    private boolean handleModuleClick(int mx, int my) {
        int H = this.field_22790;
        JsonObject cats = config.getAsJsonObject("categories");
        if (cats == null) return false;

        for (int i = 0; i < categories.size(); i++) {
            int panelX = START_X + (PANEL_W + PANEL_GAP) * i + panelScrollX;
            if (mx < panelX || mx >= panelX + PANEL_W) continue;

            JsonObject catObj = cats.getAsJsonObject(categories.get(i));
            List<Map.Entry<String, JsonElement>> modules = getModuleEntries(catObj);

            int maxContentH = H - PANEL_Y - HEADER_H - 20;
            int bodyTop    = PANEL_Y + HEADER_H;
            int bodyBottom = bodyTop + Math.max(0, Math.min(modules.size() * ROW_H, maxContentH));
            if (my < bodyTop || my >= bodyBottom) continue;

            int scroll = scrollOffsets[i];
            for (int j = 0; j < modules.size(); j++) {
                Map.Entry<String, JsonElement> me = modules.get(j);
                JsonObject module = me.getValue().getAsJsonObject();
                String mainKey = getFirstBoolKey(module);
                if (mainKey == null) continue;

                int rowY = bodyTop + j * ROW_H - scroll;
                // Skip rows entirely outside visible body area
                if (rowY + ROW_H <= bodyTop || rowY >= bodyBottom) continue;
                // Check if click Y lands in this row
                if (my < rowY || my >= rowY + ROW_H) continue;

                boolean next = !module.get(mainKey).getAsBoolean();
                module.addProperty(mainKey, next);
                setZenValue(mainKey, next);
                saveZenConfig();
                return true;
            }
        }
        return false;
    }

    // ── Scroll ────────────────────────────────────────────────────
    @Override
    public boolean method_25400(net.minecraft.class_11905 event) {
        double v = event.comp_4794(); // vertical delta: positive = scroll down
        return applyScroll(v);
    }

    // Fallback scroll path used by some input routes.
    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        lastMouseX = (int) mouseX;
        lastMouseY = (int) mouseY;
        return applyScroll(verticalAmount);
    }

    private boolean applyScroll(double v) {
        int W = this.field_22789;
        int H = this.field_22790;
        int verticalDelta = (int) Math.round(v * 10.0);
        if (verticalDelta == 0 && v != 0.0) verticalDelta = v > 0 ? 1 : -1;
        int horizontalDelta = (int) Math.round(v * 36.0);
        if (horizontalDelta == 0 && v != 0.0) horizontalDelta = v > 0 ? 1 : -1;

        JsonObject cats = config.getAsJsonObject("categories");

        // If the mouse is over a panel's body, scroll that panel vertically
        if (cats != null) {
            for (int i = 0; i < categories.size(); i++) {
                int panelX = START_X + (PANEL_W + PANEL_GAP) * i + panelScrollX;
                if (lastMouseX < panelX || lastMouseX > panelX + PANEL_W) continue;

                int bodyTop = PANEL_Y + HEADER_H;
                if (lastMouseY < bodyTop) continue;

                JsonObject catObj = cats.getAsJsonObject(categories.get(i));
                List<Map.Entry<String, JsonElement>> modules = getModuleEntries(catObj);
                int maxContentH = H - PANEL_Y - HEADER_H - 20;
                int maxScroll = Math.max(0, modules.size() * ROW_H - maxContentH);

                scrollOffsets[i] -= verticalDelta;
                if (scrollOffsets[i] < 0) scrollOffsets[i] = 0;
                if (scrollOffsets[i] > maxScroll) scrollOffsets[i] = maxScroll;
                return true;
            }
        }

        // Otherwise pan panels horizontally
        panelScrollX -= horizontalDelta;
        // Clamp: leftmost = 0 (first panel at START_X), rightmost = first panel visible
        if (panelScrollX > 0) panelScrollX = 0;
        int step = PANEL_W + PANEL_GAP;
        int totalW = categories.size() * step - PANEL_GAP;
        int minScroll = Math.min(0, W - START_X - totalW - 20);
        if (panelScrollX < minScroll) panelScrollX = minScroll;
        return true;
    }

    // ── Close / save ──────────────────────────────────────────────
    @Override
    public void method_25393() {
        saveZenConfig();
        super.method_25393();
    }

    @Override
    public boolean method_25421() {
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────
    private static List<Map.Entry<String, JsonElement>> getModuleEntries(JsonObject catObj) {
        List<Map.Entry<String, JsonElement>> result = new ArrayList<>();
        if (catObj == null) return result;
        for (Map.Entry<String, JsonElement> e : catObj.entrySet()) {
            if (e.getValue().isJsonObject()) result.add(e);
        }
        return result;
    }

    private static String getFirstBoolKey(JsonObject obj) {
        for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
            if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isBoolean()) {
                return e.getKey();
            }
        }
        return null;
    }

    private static String truncate(class_327 tr, String s, int maxW) {
        String ellipsis = "..";
        while (s.length() > 1 && tr.method_1727(s + ellipsis) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s + ellipsis;
    }

    // ── Reflect into zen's live config map ───────────────────────
    private static Object zenManagerInstance = null;
    private static Method zenGetMapMethod    = null;
    private static Method zenSaveMethod      = null;

    private static boolean initZenReflection() {
        if (zenManagerInstance != null) return true;
        try {
            Class<?> cls = Class.forName("xyz.meowing.zen.managers.config.ConfigManager");
            Field instanceField = cls.getField("INSTANCE");
            zenManagerInstance = instanceField.get(null);
            zenGetMapMethod = cls.getMethod("getConfigValueMap");
            zenSaveMethod   = cls.getMethod("saveConfig", boolean.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void setZenValue(String key, boolean value) {
        try {
            if (!initZenReflection()) return;
            Map<String, Object> map = (Map<String, Object>) zenGetMapMethod.invoke(zenManagerInstance);
            map.put(key, value);
        } catch (Exception e) {
            // ignore
        }
    }

    private static void saveZenConfig() {
        try {
            if (!initZenReflection()) return;
            zenSaveMethod.invoke(zenManagerInstance, false);
        } catch (Exception e) {
            // ignore
        }
    }
}
