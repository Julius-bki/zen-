package com.zengui;

import net.minecraft.class_437;   // Screen
import net.minecraft.class_332;   // DrawContext
import net.minecraft.class_327;   // TextRenderer
import net.minecraft.class_2561;  // Text
import net.minecraft.class_11908; // MouseClickEvent  (comp_4795=x, comp_4796=y, comp_4797=button)
import net.minecraft.class_11909; // MouseButtonEvent (comp_4797=button)
import net.minecraft.class_11905; // MouseScrollEvent (comp_4794=vertical)

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Zen config GUI — Athen-style draggable panels with Catppuccin Mocha palette.
 * One panel per category. Left-click header = drag panel. Left-click feature row = toggle.
 */
public class ZenConfigScreen extends class_437 {

    private final File configFile;
    private final JsonObject config;
    private final List<String> categories;

    // Per-panel state
    private final float[]   panelX;
    private final float[]   panelY;
    private final int[]     scrollOffset;  // 0 = top; negative = scrolled down
    private final boolean[] dragging;
    private final float[]   dragDeltaX;
    private final float[]   dragDeltaY;

    // Current mouse position (updated every render frame)
    private int lastMX = 0;
    private int lastMY = 0;

    // Debug overlay (shows which code path handled the last click)
    private String dbgText  = "";
    private long   dbgUntil = 0L;

    // ── Catppuccin Mocha ──────────────────────────────────────────
    static final int CRUST      = 0xFF11111B;
    static final int BASE       = 0xFF1E1E2E;
    static final int MANTLE     = 0xFF181825;
    static final int SURFACE0   = 0xFF313244;
    static final int SURFACE1   = 0xFF45475A;
    static final int TEXT_C     = 0xFFCDD6F4;
    static final int SUBTEXT0   = 0xFFA6ADC8;
    static final int MAUVE      = 0xFFCBA6F7;
    static final int GREEN      = 0xFFA6E3A1;
    static final int WHITE      = 0xFFFFFFFF;

    // Semi-transparent row backgrounds (matching Athen's withAlpha(0.5f) style)
    static final int MAUVE_50   = 0x80CBA6F7;  // enabled feature row
    static final int SURF_50    = 0x80313244;  // disabled feature row
    static final int SURF_04    = 0x0A313244;  // panel body background (Athen: Surface0 @ 4%)

    // ── Layout constants (mirrors Athen) ──────────────────────────
    static final int PANEL_W    = 240;  // Panel.WIDTH
    static final int PANEL_STEP = 260;  // 240 + 20px gap
    static final int START_X    = 50;
    static final int START_Y    = 50;
    static final int HEADER_H   = 32;   // Panel.HEADER_HEIGHT
    static final int ROW_H      = 32;   // SectionButton.HEIGHT

    // ── Constructor ───────────────────────────────────────────────
    public ZenConfigScreen(File configFile, JsonObject config) {
        super(class_2561.method_43470("Zen Config"));
        this.configFile = configFile;
        this.config     = config;
        JsonObject cats = config.getAsJsonObject("categories");
        this.categories   = cats != null ? new ArrayList<>(cats.keySet()) : new ArrayList<>();
        int n = categories.size();
        this.panelX       = new float[n];
        this.panelY       = new float[n];
        this.scrollOffset = new int[n];
        this.dragging     = new boolean[n];
        this.dragDeltaX   = new float[n];
        this.dragDeltaY   = new float[n];

        // Position panels in a 7-column grid (same as Athen)
        for (int i = 0; i < n; i++) {
            int col = i % 7;
            int row = i / 7;
            panelX[i] = START_X + col * PANEL_STEP;
            panelY[i] = START_Y + row * 400;
        }
    }

    // ── Render ────────────────────────────────────────────────────
    @Override
    public void method_25394(class_332 ctx, int mx, int my, float delta) {
        lastMX = mx;
        lastMY = my;

        // Update dragged panels
        for (int i = 0; i < categories.size(); i++) {
            if (dragging[i]) {
                panelX[i] = dragDeltaX[i] + mx;
                panelY[i] = dragDeltaY[i] + my;
            }
        }

        int W = this.field_22789;
        int H = this.field_22790;
        class_327 tr = this.field_22793;

        // Background
        ctx.method_25294(0, 0, W, H, CRUST);

        // Hint
        ctx.method_25303(tr, "ESC: save & close", 4, H - 10, SURFACE1);

        // Debug overlay
        if (System.currentTimeMillis() < dbgUntil) {
            ctx.method_25294(0, 0, W, 12, 0xCC000000);
            ctx.method_25303(tr, dbgText, 2, 2, 0xFFFF5555);
        }

        JsonObject cats = config.getAsJsonObject("categories");
        if (cats == null) return;

        for (int i = 0; i < categories.size(); i++) {
            drawPanel(ctx, tr, cats, i, H);
        }
    }

    private void drawPanel(class_332 ctx, class_327 tr, JsonObject cats, int i, int H) {
        String catName = categories.get(i);
        JsonObject catObj = cats.getAsJsonObject(catName);
        List<Map.Entry<String, JsonElement>> modules = getModuleEntries(catObj);

        int x   = (int) panelX[i];
        int y   = (int) panelY[i];
        int sc  = scrollOffset[i];  // 0 or negative

        int contentH    = modules.size() * ROW_H;
        int maxDisplayH = Math.max(0, H - y - HEADER_H - 20);
        int displayH    = Math.min(contentH, maxDisplayH);
        int bodyTop     = y + HEADER_H;
        int bodyBottom  = bodyTop + displayH;

        // Panel body background (very transparent, like Athen)
        if (displayH > 0)
            ctx.method_25294(x, bodyTop, x + PANEL_W, bodyBottom, SURF_04);

        // Header (Mantle) + Mauve accent line at bottom
        ctx.method_25294(x, y, x + PANEL_W, y + HEADER_H, MANTLE);
        ctx.method_25294(x, y + HEADER_H - 2, x + PANEL_W, y + HEADER_H, MAUVE);

        // Category name centered in header
        int nameW = tr.method_1727(catName);
        ctx.method_25303(tr, catName, x + (PANEL_W - nameW) / 2, y + (HEADER_H - 8) / 2, TEXT_C);

        if (displayH == 0) return;

        // Feature rows
        for (int j = 0; j < modules.size(); j++) {
            Map.Entry<String, JsonElement> me = modules.get(j);
            if (!me.getValue().isJsonObject()) continue;
            JsonObject module = me.getValue().getAsJsonObject();
            String boolKey = getFirstBoolKey(module);
            boolean enabled = boolKey != null && module.get(boolKey).getAsBoolean();

            int rowY = bodyTop + j * ROW_H + sc;
            if (rowY + ROW_H <= bodyTop || rowY >= bodyBottom) continue;

            int drawTop = Math.max(rowY, bodyTop);
            int drawBot = Math.min(rowY + ROW_H, bodyBottom);

            // Row background — Mauve 50% if on, Surface0 50% if off (matches Athen)
            ctx.method_25294(x, drawTop, x + PANEL_W, drawBot, enabled ? MAUVE_50 : SURF_50);

            // Separator line at row bottom
            ctx.method_25294(x, drawBot - 1, x + PANEL_W, drawBot, 0x22FFFFFF);

            // Text + dot only for fully visible rows
            if (rowY >= bodyTop && rowY + ROW_H <= bodyBottom) {
                String modName = me.getKey();
                String display = tr.method_1727(modName) > PANEL_W - 16
                        ? truncate(tr, modName, PANEL_W - 16) : modName;
                ctx.method_25303(tr, display, x + 6, rowY + (ROW_H - 8) / 2, TEXT_C);

                // Small status dot on right (green = on, grey = off)
                int dotX = x + PANEL_W - 10;
                int dotY = rowY + (ROW_H - 6) / 2;
                ctx.method_25294(dotX, dotY, dotX + 6, dotY + 6, enabled ? GREEN : SURFACE1);
            }
        }
    }

    // ── Mouse click (primary path) ────────────────────────────────
    @Override
    public boolean method_25404(net.minecraft.class_11908 event) {
        int mx     = event.comp_4795();
        int my     = event.comp_4796();
        int button = event.comp_4797();

        if (button == 0) {
            // Iterate panels in reverse (last drawn = topmost)
            for (int i = categories.size() - 1; i >= 0; i--) {
                int x = (int) panelX[i];
                int y = (int) panelY[i];

                // Header → start drag
                if (mx >= x && mx < x + PANEL_W && my >= y && my < y + HEADER_H) {
                    dragging[i]   = true;
                    dragDeltaX[i] = panelX[i] - mx;
                    dragDeltaY[i] = panelY[i] - my;
                    dbg("drag panel " + i);
                    return true;
                }

                // Body → toggle feature
                if (toggleAt(i, mx, my)) return true;

                // Click inside panel area but no row hit — still consume to prevent ghost clicks
                int bodyTop = y + HEADER_H;
                int H = this.field_22790;
                int maxDisplayH = Math.max(0, H - y - HEADER_H - 20);
                JsonObject cats = config.getAsJsonObject("categories");
                List<Map.Entry<String, JsonElement>> modules =
                        cats != null ? getModuleEntries(cats.getAsJsonObject(categories.get(i))) : Collections.emptyList();
                int bodyBottom = bodyTop + Math.min(modules.size() * ROW_H, maxDisplayH);
                if (mx >= x && mx < x + PANEL_W && my >= bodyTop && my < bodyBottom) {
                    dbg("body miss " + mx + "," + my);
                    return true;
                }
            }
            dbg("no panel hit " + mx + "," + my);
        }
        return super.method_25404(event);
    }

    // Mouse release → stop all drags
    @Override
    public boolean method_25406(net.minecraft.class_11909 event) {
        if (event.comp_4797() == 0) Arrays.fill(dragging, false);
        return super.method_25406(event);
    }

    private boolean toggleAt(int i, int mx, int my) {
        int x = (int) panelX[i];
        if (mx < x || mx >= x + PANEL_W) return false;

        int y           = (int) panelY[i];
        int bodyTop     = y + HEADER_H;
        int H           = this.field_22790;
        int maxDisplayH = Math.max(0, H - y - HEADER_H - 20);

        JsonObject cats = config.getAsJsonObject("categories");
        if (cats == null) return false;
        List<Map.Entry<String, JsonElement>> modules = getModuleEntries(cats.getAsJsonObject(categories.get(i)));

        int bodyBottom = bodyTop + Math.min(modules.size() * ROW_H, maxDisplayH);
        if (my < bodyTop || my >= bodyBottom) return false;

        int sc = scrollOffset[i];
        for (int j = 0; j < modules.size(); j++) {
            Map.Entry<String, JsonElement> me = modules.get(j);
            if (!me.getValue().isJsonObject()) continue;
            JsonObject module = me.getValue().getAsJsonObject();
            String boolKey = getFirstBoolKey(module);
            if (boolKey == null) continue;

            int rowY = bodyTop + j * ROW_H + sc;
            if (rowY + ROW_H <= bodyTop || rowY >= bodyBottom) continue;
            if (my < rowY || my >= rowY + ROW_H) continue;

            boolean next = !module.get(boolKey).getAsBoolean();
            module.addProperty(boolKey, next);
            setZenValue(boolKey, next);
            saveZenConfig();
            dbg("toggled " + me.getKey() + " -> " + next);
            return true;
        }
        return false;
    }

    // ── Scroll ────────────────────────────────────────────────────
    @Override
    public boolean method_25401(double scrollX, double scrollY, double horizontal, double vertical) {
        lastMX = (int) scrollX;
        lastMY = (int) scrollY;
        return applyScroll(lastMX, lastMY, vertical);
    }

    @Override
    public boolean method_25400(net.minecraft.class_11905 event) {
        return applyScroll(lastMX, lastMY, event.comp_4794());
    }

    private boolean applyScroll(int mx, int my, double vertical) {
        // vertical > 0 = wheel up → offset toward 0 (content down)
        // vertical < 0 = wheel down → offset more negative (content up)
        int amount = vertical > 0 ? 16 : (vertical < 0 ? -16 : 0);
        if (amount == 0) return false;

        int H = this.field_22790;
        JsonObject cats = config.getAsJsonObject("categories");

        for (int i = 0; i < categories.size(); i++) {
            int x = (int) panelX[i];
            int y = (int) panelY[i];
            if (mx < x || mx >= x + PANEL_W) continue;
            if (my < y) continue;

            List<Map.Entry<String, JsonElement>> modules =
                    cats != null ? getModuleEntries(cats.getAsJsonObject(categories.get(i))) : Collections.emptyList();
            int contentH    = modules.size() * ROW_H;
            int maxDisplayH = Math.max(0, H - y - HEADER_H - 20);
            int maxScroll   = -Math.max(0, contentH - maxDisplayH); // negative

            scrollOffset[i] = Math.max(maxScroll, Math.min(0, scrollOffset[i] + amount));
            return true;
        }
        return false;
    }

    // ── Close / save ──────────────────────────────────────────────
    @Override
    public void method_25393() {
        saveZenConfig();
        super.method_25393();
    }

    @Override
    public boolean method_25421() {
        return true; // don't pause game
    }

    // ── Helpers ───────────────────────────────────────────────────
    private void dbg(String msg) {
        dbgText  = msg;
        dbgUntil = System.currentTimeMillis() + 4000L;
    }

    private static List<Map.Entry<String, JsonElement>> getModuleEntries(JsonObject catObj) {
        List<Map.Entry<String, JsonElement>> result = new ArrayList<>();
        if (catObj == null) return result;
        for (Map.Entry<String, JsonElement> e : catObj.entrySet())
            if (e.getValue().isJsonObject()) result.add(e);
        return result;
    }

    private static String getFirstBoolKey(JsonObject obj) {
        for (Map.Entry<String, JsonElement> e : obj.entrySet())
            if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isBoolean())
                return e.getKey();
        return null;
    }

    private static String truncate(class_327 tr, String s, int maxW) {
        String ell = "..";
        while (s.length() > 1 && tr.method_1727(s + ell) > maxW)
            s = s.substring(0, s.length() - 1);
        return s + ell;
    }

    // ── Reflect into zen's live config ───────────────────────────
    private static Object zenManagerInstance = null;
    private static Method zenGetMapMethod    = null;
    private static Method zenSaveMethod      = null;

    private static boolean initZenReflection() {
        if (zenManagerInstance != null) return true;
        try {
            Class<?> cls = Class.forName("xyz.meowing.zen.managers.config.ConfigManager");
            Field f = cls.getField("INSTANCE");
            zenManagerInstance = f.get(null);
            zenGetMapMethod    = cls.getMethod("getConfigValueMap");
            zenSaveMethod      = cls.getMethod("saveConfig", boolean.class);
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
        } catch (Exception ignored) {}
    }

    private static void saveZenConfig() {
        try {
            if (!initZenReflection()) return;
            zenSaveMethod.invoke(zenManagerInstance, false);
        } catch (Exception ignored) {}
    }
}
