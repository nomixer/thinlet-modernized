/* Thinlet GUI toolkit - www.thinlet.com
 * Copyright (C) 2002-2005 Robert Bajzat (rbajzat@freemail.hu)
 *
 * This file is part of the Thinlet modernization fork; its method bodies are
 * extracted verbatim from the 2005 Thinlet.java (paint branches) and remain
 * under the original license and copyright.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */
package thinlet;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;

/**
 * Per-widget paint branches — plus their already-golden-guarded shared helpers
 * (D50), e.g. {@link #field} — extracted from {@code Thinlet}'s recursive paint:
 * the Cut 2 "typed Renderer" seam (DECISIONS.md D42/D48), grown one widget
 * branch at a time. Behavior-preserving by contract: each method body is the
 * 2005 code moved verbatim, guarded by the golden + interaction-golden net.
 *
 * <p>Seam style (D48): <b>stateless with explicit context</b> — no fields; the
 * {@code Thinlet} instance, the widget, the {@code Graphics}, and the
 * paint-time state ({@code pressed}/{@code inside}/{@code focus}/{@code
 * enabled}) are all passed in. Package-private through 3a (D43): no public
 * surface until the later new-API phase. The dispatch (the classname chain)
 * lives here too ({@link #paint}); only the tooltip-coupled {@code desktop}
 * body remains in {@code Thinlet} behind the {@code Thinlet#paintDesktop}
 * callback (the one net-invisible paint path, D45), and {@code Thinlet.paint}
 * is now a shim delegating here.
 */
final class Renderer {

    private Renderer() {}

    /**
     * The recursive per-component paint: visibility/bounds gate, clip-reject,
     * translate, then the 2005 classname dispatch chain, verbatim — each widget
     * branch delegating to its extracted method. Only the tooltip-coupled
     * {@code desktop} body remains in {@code Thinlet} (the one net-invisible
     * paint path, D45) behind the {@link Thinlet#paintDesktop} callback.
     */
    static void paint(
            Thinlet t,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            Object component,
            boolean enabled) {
        if (!t.getBoolean(component, "visible", true)) {
            return;
        }
        Rectangle bounds = t.getRectangle(component, "bounds");
        if (bounds == null) {
            return;
        }
        t.layoutIfDirty(component, bounds);
        // return if the component was out of the cliping rectangle
        if ((clipx + clipwidth < bounds.x)
                || (clipx > bounds.x + bounds.width)
                || (clipy + clipheight < bounds.y)
                || (clipy > bounds.y + bounds.height)) {
            return;
        }
        // set the clip rectangle relative to the component location
        clipx -= bounds.x;
        clipy -= bounds.y;
        g.translate(bounds.x, bounds.y);
        // g.setClip(0, 0, bounds.width, bounds.height);
        String classname = Thinlet.getClass(component);
        boolean pressed = (t.mousepressed == component);
        boolean inside = (t.mouseinside == component) && ((t.mousepressed == null) || pressed);
        boolean focus = t.focusinside && (t.focusowner == component);
        enabled = t.getBoolean(component, "enabled", true); // enabled &&

        if (Thinlet.is(classname, "label")) {
            Renderer.label(t, component, bounds, g, clipx, clipy, clipwidth, clipheight, enabled);
        } else if ((Thinlet.is(classname, "button")) || (Thinlet.is(classname, "togglebutton"))) {
            Renderer.button(
                    t,
                    component,
                    classname,
                    bounds,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight,
                    pressed,
                    inside,
                    focus,
                    enabled);
        } else if (Thinlet.is(classname, "checkbox")) {
            Renderer.checkbox(
                    t, component, bounds, g, clipx, clipy, clipwidth, clipheight, pressed, inside, focus, enabled);
        } else if (Thinlet.is(classname, "combobox")) {
            Renderer.combobox(
                    t, component, bounds, g, clipx, clipy, clipwidth, clipheight, pressed, inside, focus, enabled);
        } else if (Thinlet.is(classname, ":combolist")) {
            Renderer.scroll(
                    t,
                    component,
                    classname,
                    pressed,
                    inside,
                    focus,
                    false,
                    enabled,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight);
        } else if ((Thinlet.is(classname, "textfield")) || (Thinlet.is(classname, "passwordfield"))) {
            Renderer.field(
                    t,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight,
                    component,
                    bounds.width,
                    bounds.height,
                    focus,
                    enabled,
                    (Thinlet.is(classname, "passwordfield")),
                    0);
        } else if (Thinlet.is(classname, "textarea")) {
            Renderer.scroll(
                    t,
                    component,
                    classname,
                    pressed,
                    inside,
                    focus,
                    true,
                    enabled,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight);
        } else if (Thinlet.is(classname, "tabbedpane")) {
            Renderer.tabbedpane(
                    t, component, bounds, g, clipx, clipy, clipwidth, clipheight, pressed, inside, focus, enabled);
        } else if ((Thinlet.is(classname, "panel")) || (Thinlet.is(classname, "dialog"))) {
            Renderer.container(
                    t,
                    component,
                    classname,
                    bounds,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight,
                    pressed,
                    inside,
                    focus,
                    enabled);
        } else if (Thinlet.is(classname, "desktop")) {
            t.paintDesktop(component, bounds, g, clipx, clipy, clipwidth, clipheight, enabled);
        } else if (Thinlet.is(classname, "spinbox")) {
            Renderer.spinbox(
                    t, component, bounds, g, clipx, clipy, clipwidth, clipheight, pressed, inside, focus, enabled);
        } else if (Thinlet.is(classname, "progressbar")) {
            Renderer.progressbar(t, component, bounds, g, enabled);
        } else if (Thinlet.is(classname, "slider")) {
            Renderer.slider(t, component, bounds, g, focus, enabled);
        } else if (Thinlet.is(classname, "splitpane")) {
            Renderer.splitpane(t, component, bounds, g, clipx, clipy, clipwidth, clipheight, focus, enabled);
        } else if ((Thinlet.is(classname, "list"))
                || (Thinlet.is(classname, "table"))
                || (Thinlet.is(classname, "tree"))) {
            Renderer.scroll(
                    t,
                    component,
                    classname,
                    pressed,
                    inside,
                    focus,
                    focus && (Thinlet.get(component, ":comp") == null),
                    enabled,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight);
        } else if (Thinlet.is(classname, "separator")) {
            g.setColor(enabled ? t.c_border : t.c_disable);
            g.fillRect(0, 0, bounds.width + Thinlet.evm, bounds.height + Thinlet.evm);
        } else if (Thinlet.is(classname, "menubar")) {
            Renderer.menubar(t, component, bounds, g, clipx, clipy, clipwidth, clipheight, enabled);
        } else if (Thinlet.is(classname, ":popup")) {
            Renderer.popup(t, component, bounds, g, clipx, clipy, clipwidth, clipheight);
        } else if (Thinlet.is(classname, "bean")) {
            g.clipRect(0, 0, bounds.width, bounds.height);
            ((Component) Thinlet.get(component, "bean")).paint(g);
            g.setClip(clipx, clipy, clipwidth, clipheight);
        } else throw new IllegalArgumentException(classname);
        g.translate(-bounds.x, -bounds.y);
        clipx += bounds.x;
        clipy += bounds.y;
    }

    /** The 2005 {@code label} paint branch, verbatim. */
    static void label(
            Thinlet t,
            Object component,
            Rectangle bounds,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            boolean enabled) {
        t.paint(
                component,
                0,
                0,
                bounds.width,
                bounds.height,
                g,
                clipx,
                clipy,
                clipwidth,
                clipheight,
                false,
                false,
                false,
                false,
                0,
                0,
                0,
                0,
                false,
                enabled ? 'e' : 'd',
                "left",
                true,
                false);
    }

    /** The 2005 {@code button}/{@code togglebutton} paint branch, verbatim. */
    static void button(
            Thinlet t,
            Object component,
            String classname,
            Rectangle bounds,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            boolean pressed,
            boolean inside,
            boolean focus,
            boolean enabled) {
        boolean toggled = (Thinlet.is(classname, "togglebutton")) && t.getBoolean(component, "selected", false);
        boolean link = (Thinlet.is(classname, "button")) && (Thinlet.is(Thinlet.get(component, "type"), "link"));
        if (link) {
            t.paint(
                    component,
                    0,
                    0,
                    bounds.width,
                    bounds.height,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight,
                    false,
                    false,
                    false,
                    false,
                    0,
                    0,
                    0,
                    0,
                    focus,
                    enabled ? (pressed ? 'e' : 'l') : 'd',
                    "center",
                    true,
                    enabled && (inside != pressed));
        } else { // disabled toggled
            char mode = enabled ? ((inside != pressed) ? 'h' : ((pressed || toggled) ? 'p' : 'g')) : 'd';
            t.paint(
                    component,
                    0,
                    0,
                    bounds.width,
                    bounds.height,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight,
                    true,
                    true,
                    true,
                    true,
                    2,
                    5,
                    2,
                    5,
                    focus,
                    mode,
                    "center",
                    true,
                    false);
            // (enabled && (is(classname, "button")) && is(get(component, "type"), "default"))...
        }
    }

    /** The 2005 {@code checkbox} (and radio-button {@code group}) paint branch, verbatim. */
    static void checkbox(
            Thinlet t,
            Object component,
            Rectangle bounds,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            boolean pressed,
            boolean inside,
            boolean focus,
            boolean enabled) {
        t.paint(
                component,
                0,
                0,
                bounds.width,
                bounds.height,
                g,
                clipx,
                clipy,
                clipwidth,
                clipheight,
                false,
                false,
                false,
                false,
                0,
                t.block + 3,
                0,
                0,
                false,
                enabled ? 'e' : 'd',
                "left",
                true,
                false);

        boolean selected = t.getBoolean(component, "selected", false);
        String group = t.getString(component, "group", null);
        Color border = enabled ? t.c_border : t.c_disable;
        Color foreground = enabled ? ((inside != pressed) ? t.c_hover : (pressed ? t.c_press : t.c_ctrl)) : t.c_bg;
        int dy = (bounds.height - t.block + 2) / 2;
        if (group == null) {
            t.paintRect(g, 1, dy + 1, t.block - 2, t.block - 2, border, foreground, true, true, true, true, true);
        } else {
            g.setColor((foreground != t.c_ctrl) ? foreground : t.c_bg);
            g.fillOval(1, dy + 1, t.block - 3 + Thinlet.evm, t.block - 3 + Thinlet.evm);
            g.setColor(border);
            g.drawOval(1, dy + 1, t.block - 3, t.block - 3);
        }
        if (focus) {
            t.drawFocus(g, 0, 0, bounds.width - 1, bounds.height - 1);
        }
        if ((!selected && inside && pressed) || (selected && (!inside || !pressed))) {
            g.setColor(enabled ? t.c_text : t.c_disable);
            if (group == null) {
                g.fillRect(3, dy + t.block - 9, 2 + Thinlet.evm, 6 + Thinlet.evm);
                g.drawLine(3, dy + t.block - 4, t.block - 4, dy + 3);
                g.drawLine(4, dy + t.block - 4, t.block - 4, dy + 4);
            } else {
                g.fillOval(5, dy + 5, t.block - 10 + Thinlet.evm, t.block - 10 + Thinlet.evm);
                g.drawOval(4, dy + 4, t.block - 9, t.block - 9);
            }
        }
    }

    /**
     * The 2005 {@code paintField} helper, verbatim: the text-field body shared by
     * textfield/passwordfield, the editable combobox, and the spinbox — border,
     * selection highlight, caret, text (or {@code '*'} echo when {@code hidden}),
     * and the focus rectangle.
     */
    static void field(
            Thinlet t,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            Object component,
            int width,
            int height,
            boolean focus,
            boolean enabled,
            boolean hidden,
            int left) {
        boolean editable = t.getBoolean(component, "editable", true);
        t.paintRect(
                g,
                0,
                0,
                width,
                height,
                enabled ? t.c_border : t.c_disable,
                editable ? t.getColor(component, "background", t.c_textbg) : t.c_bg,
                true,
                true,
                true,
                true,
                true);
        g.clipRect(1 + left, 1, width - left - 2, height - 2);

        String text = t.getString(component, "text", "");
        int offset = t.getInteger(component, ":offset", 0);
        Font currentfont = (Font) Thinlet.get(component, "font");
        if (currentfont != null) {
            g.setFont(currentfont);
        }
        FontMetrics fm = g.getFontMetrics();

        int caret = 0;
        if (focus) {
            int start = t.getInteger(component, "start", 0);
            int end = t.getInteger(component, "end", 0);
            caret = hidden ? (fm.charWidth('*') * end) : fm.stringWidth(text.substring(0, end));
            if (start != end) {
                int is = hidden ? (fm.charWidth('*') * start) : fm.stringWidth(text.substring(0, start));
                g.setColor(t.c_select);
                g.fillRect(
                        2 + left - offset + Math.min(is, caret),
                        1,
                        Math.abs(caret - is) + Thinlet.evm,
                        height - 2 + Thinlet.evm);
            }
        }

        if (focus) { // draw caret
            g.setColor(t.c_focus);
            g.fillRect(1 + left - offset + caret, 1, 1 + Thinlet.evm, height - 2 + Thinlet.evm);
        }

        g.setColor(enabled ? t.getColor(component, "foreground", t.c_text) : t.c_disable);
        int fx = 2 + left - offset;
        int fy = (height + fm.getAscent() - fm.getDescent()) / 2;
        if (hidden) {
            int fh = fm.charWidth('*');
            for (int i = text.length(); i > 0; i--) {
                g.drawString("*", fx, fy);
                fx += fh;
            }
        } else {
            g.drawString(text, fx, fy);
        }
        if (currentfont != null) {
            g.setFont(t.font);
        }
        g.setClip(clipx, clipy, clipwidth, clipheight);

        if (focus) { // draw dotted rectangle
            t.drawFocus(g, 1, 1, width - 3, height - 3);
        }
    }

    /**
     * The 2005 tinted arrow button (scrollbar/spinbox/combobox arrows), verbatim:
     * bordered rect with the hover/press tint gated on the arrow's <em>part</em>
     * name, then the directional glyph.
     */
    static void arrow(
            Thinlet t,
            Graphics g,
            int x,
            int y,
            int width,
            int height,
            char dir,
            boolean enabled,
            boolean inside,
            boolean pressed,
            String part,
            boolean top,
            boolean left,
            boolean bottom,
            boolean right,
            boolean horizontal) {
        inside = inside && (t.insidepart == part);
        pressed = pressed && (t.pressedpart == part);
        t.paintRect(
                g,
                x,
                y,
                width,
                height,
                enabled ? t.c_border : t.c_disable,
                enabled ? ((inside != pressed) ? t.c_hover : (pressed ? t.c_press : t.c_ctrl)) : t.c_bg,
                top,
                left,
                bottom,
                right,
                horizontal);
        g.setColor(enabled ? t.c_text : t.c_disable);
        arrow(
                g,
                x + (left ? 1 : 0),
                y + (top ? 1 : 0),
                width - (left ? 1 : 0) - (right ? 1 : 0),
                height - (top ? 1 : 0) - (bottom ? 1 : 0),
                dir);
    }

    /**
     * The 2005 bare arrow glyph, verbatim — a pure function of the
     * {@code Graphics}, needing no {@code Thinlet} context at all.
     */
    static void arrow(Graphics g, int x, int y, int width, int height, char dir) {
        int cx = x + width / 2 - 2;
        int cy = y + height / 2 - 2;
        for (int i = 0; i < 4; i++) {
            if (dir == 'N') { // north
                g.drawLine(cx + 1 - i, cy + i, cx + 1 /*2*/ + i, cy + i);
            } else if (dir == 'W') { // west
                g.drawLine(cx + i, cy + 1 - i, cx + i, cy + 1 /*2*/ + i);
            } else if (dir == 'S') { // south
                g.drawLine(cx + 1 - i, cy + 4 - i, cx + 1 /*2*/ + i, cy + 4 - i);
            } else { // east
                g.drawLine(cx + 4 - i, cy + 1 - i, cx + 4 - i, cy + 1 /*2*/ + i);
            }
        }
    }

    /**
     * @param component scrollable widget
     * @param classname
     * @param pressed
     * @param inside
     * @param focus
     * @param enabled
     * @param g grahics context
     * @param clipx current cliping x location relative to the component
     * @param clipy y location of the cliping area relative to the component
     * @param clipwidth width of the cliping area
     * @param clipheight height of the cliping area
     * @param header column height
     * @param topborder bordered on the top if true
     * @param border define left, bottom, and right border if true
     */
    static void scroll(
            Thinlet t,
            Object component,
            String classname,
            boolean pressed,
            boolean inside,
            boolean focus,
            boolean drawfocus,
            boolean enabled,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight) {
        Rectangle port = t.getRectangle(component, ":port");
        Rectangle horizontal = t.getRectangle(component, ":horizontal");
        Rectangle vertical = t.getRectangle(component, ":vertical");
        Rectangle view = t.getRectangle(component, ":view");

        if (horizontal != null) { // paint horizontal scrollbar
            int x = horizontal.x;
            int y = horizontal.y;
            int width = horizontal.width;
            int height = horizontal.height;
            arrow(t, g, x, y, t.block, height, 'W', enabled, inside, pressed, "left", true, true, true, false, true);
            arrow(
                    t,
                    g,
                    x + width - t.block,
                    y,
                    t.block,
                    height,
                    'E',
                    enabled,
                    inside,
                    pressed,
                    "right",
                    true,
                    false,
                    true,
                    true,
                    true);

            int track = width - (2 * t.block);
            if (track < 10) {
                t.paintRect(
                        g,
                        x + t.block,
                        y,
                        track,
                        height,
                        enabled ? t.c_border : t.c_disable,
                        t.c_bg,
                        true,
                        true,
                        true,
                        true,
                        true);
            } else {
                int knob = Math.max(track * port.width / view.width, 10);
                int decrease = view.x * (track - knob) / (view.width - port.width);
                t.paintRect(
                        g,
                        x + t.block,
                        y,
                        decrease,
                        height,
                        enabled ? t.c_border : t.c_disable,
                        t.c_bg,
                        false,
                        true,
                        true,
                        false,
                        true);
                t.paintRect(
                        g,
                        x + t.block + decrease,
                        y,
                        knob,
                        height,
                        enabled ? t.c_border : t.c_disable,
                        enabled ? t.c_ctrl : t.c_bg,
                        true,
                        true,
                        true,
                        true,
                        true);
                int n = Math.min(5, (knob - 4) / 3);
                g.setColor(enabled ? t.c_border : t.c_disable);
                int cx = (x + t.block + decrease) + (knob + 2 - n * 3) / 2;
                for (int i = 0; i < n; i++) {
                    g.drawLine(cx + i * 3, y + 3, cx + i * 3, y + height - 5);
                }
                int increase = track - decrease - knob;
                t.paintRect(
                        g,
                        x + t.block + decrease + knob,
                        y,
                        increase,
                        height,
                        enabled ? t.c_border : t.c_disable,
                        t.c_bg,
                        false,
                        false,
                        true,
                        true,
                        true);
            }
        }

        if (vertical != null) { // paint vertical scrollbar
            int x = vertical.x;
            int y = vertical.y;
            int width = vertical.width;
            int height = vertical.height;
            arrow(t, g, x, y, width, t.block, 'N', enabled, inside, pressed, "up", true, true, false, true, false);
            arrow(
                    t,
                    g,
                    x,
                    y + height - t.block,
                    width,
                    t.block,
                    'S',
                    enabled,
                    inside,
                    pressed,
                    "down",
                    false,
                    true,
                    true,
                    true,
                    false);

            int track = height - (2 * t.block);
            if (track < 10) {
                t.paintRect(
                        g,
                        x,
                        y + t.block,
                        width,
                        track,
                        enabled ? t.c_border : t.c_disable,
                        t.c_bg,
                        true,
                        true,
                        true,
                        true,
                        false);
            } else {
                int knob = Math.max(track * port.height / view.height, 10);
                int decrease = view.y * (track - knob) / (view.height - port.height);
                t.paintRect(
                        g,
                        x,
                        y + t.block,
                        width,
                        decrease,
                        enabled ? t.c_border : t.c_disable,
                        t.c_bg,
                        true,
                        false,
                        false,
                        true,
                        false);
                t.paintRect(
                        g,
                        x,
                        y + t.block + decrease,
                        width,
                        knob,
                        enabled ? t.c_border : t.c_disable,
                        enabled ? t.c_ctrl : t.c_bg,
                        true,
                        true,
                        true,
                        true,
                        false);
                int n = Math.min(5, (knob - 4) / 3);
                g.setColor(enabled ? t.c_border : t.c_disable);
                int cy = (y + t.block + decrease) + (knob + 2 - n * 3) / 2;
                for (int i = 0; i < n; i++) {
                    g.drawLine(x + 3, cy + i * 3, x + width - 5, cy + i * 3);
                }
                int increase = track - decrease - knob;
                t.paintRect(
                        g,
                        x,
                        y + t.block + decrease + knob,
                        width,
                        increase,
                        enabled ? t.c_border : t.c_disable,
                        t.c_bg,
                        false,
                        false,
                        true,
                        true,
                        false);
            }
        }

        boolean hneed = (horizontal != null);
        boolean vneed = (vertical != null);
        if ((!Thinlet.is(classname, "panel"))
                && (!Thinlet.is(classname, "dialog"))
                && ((!Thinlet.is(classname, "textarea")) || t.getBoolean(component, "border", true))) {
            t.paintRect(
                    g,
                    port.x - 1,
                    port.y - 1,
                    port.width + (vneed ? 1 : 2),
                    port.height + (hneed ? 1 : 2),
                    enabled ? t.c_border : t.c_disable,
                    t.getColor(component, "background", t.c_textbg),
                    true,
                    true,
                    !hneed,
                    !vneed,
                    true); // TODO not editable textarea background color
            if (Thinlet.is(classname, "table")) {
                Object header = Thinlet.get(component, "header");
                if (header != null) {
                    int[] columnwidths = (int[]) Thinlet.get(component, ":widths");
                    Object column = Thinlet.get(header, ":comp");
                    int x = 0;
                    g.clipRect(0, 0, port.width + 2, port.y); // not 2 and decrease clip area...
                    for (int i = 0; i < columnwidths.length; i++) {
                        if (i != 0) {
                            column = Thinlet.get(column, ":next");
                        }
                        boolean lastcolumn = (i == columnwidths.length - 1);
                        int width = lastcolumn ? (view.width - x + 2) : columnwidths[i];

                        t.paint(
                                column,
                                x - view.x,
                                0,
                                width,
                                port.y - 1,
                                g,
                                clipx,
                                clipy,
                                clipwidth,
                                clipheight,
                                true,
                                true,
                                false,
                                lastcolumn,
                                1,
                                1,
                                0,
                                0,
                                false,
                                enabled ? 'g' : 'd',
                                "left",
                                false,
                                false);

                        Object sort = Thinlet.get(column, "sort"); // "none", "ascent", "descent"
                        if (sort != null) {
                            arrow(
                                    g,
                                    x - view.x + width - t.block,
                                    0,
                                    t.block,
                                    port.y,
                                    (Thinlet.is(sort, "ascent")) ? 'S' : 'N');
                        }
                        x += width;
                    }
                    g.setClip(clipx, clipy, clipwidth, clipheight);
                }
            }
        }
        int x1 = Math.max(clipx, port.x);
        int x2 = Math.min(clipx + clipwidth, port.x + port.width);
        int y1 = Math.max(clipy, port.y);
        int y2 = Math.min(clipy + clipheight, port.y + port.height);
        if ((x2 > x1) && (y2 > y1)) {
            g.clipRect(x1, y1, x2 - x1, y2 - y1);
            g.translate(port.x - view.x, port.y - view.y);

            content(
                    t,
                    component,
                    classname,
                    focus,
                    enabled,
                    g,
                    view.x - port.x + x1,
                    view.y - port.y + y1,
                    x2 - x1,
                    y2 - y1,
                    port.width,
                    view.width);

            g.translate(view.x - port.x, view.y - port.y);
            g.setClip(clipx, clipy, clipwidth, clipheight);
        }
        if (focus && drawfocus) { // draw dotted rectangle around the viewport
            t.drawFocus(g, port.x, port.y, port.width - 1, port.height - 1);
        }
    }

    /**
     * Paint scrollable content
     * @param component a panel
     */
    static void content(
            Thinlet t,
            Object component,
            String classname,
            boolean focus,
            boolean enabled,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            int portwidth,
            int viewwidth) {
        if (Thinlet.is(classname, "textarea")) {
            char[] chars = (char[]) Thinlet.get(component, ":text");
            int start = focus ? t.getInteger(component, "start", 0) : 0;
            int end = focus ? t.getInteger(component, "end", 0) : 0;
            int is = Math.min(start, end);
            int ie = Math.max(start, end);
            Font customfont = (Font) Thinlet.get(component, "font");
            if (customfont != null) {
                g.setFont(customfont);
            }
            FontMetrics fm = g.getFontMetrics();
            int fontascent = fm.getAscent();
            int fontheight = fm.getHeight();
            int ascent = 1;

            Color textcolor = enabled ? t.getColor(component, "foreground", t.c_text) : t.c_disable;
            for (int i = 0, j = 0; j <= chars.length; j++) {
                if ((j == chars.length) || (chars[j] == '\n')) {
                    if (clipy + clipheight <= ascent) {
                        break;
                    } // the next lines are bellow paint rectangle
                    if (clipy < ascent + fontheight) { // t line is not above painting area
                        if (focus && (is != ie) && (ie >= i) && (is <= j)) {
                            int xs = (is < i) ? -1 : ((is > j) ? (viewwidth - 1) : fm.charsWidth(chars, i, is - i));
                            int xe = ((j != -1) && (ie > j)) ? (viewwidth - 1) : fm.charsWidth(chars, i, ie - i);
                            g.setColor(t.c_select);
                            g.fillRect(1 + xs, ascent, xe - xs + Thinlet.evm, fontheight + Thinlet.evm);
                        }
                        g.setColor(textcolor);
                        g.drawChars(chars, i, j - i, 1, ascent + fontascent);
                        if (focus && (end >= i) && (end <= j)) {
                            int caret = fm.charsWidth(chars, i, end - i);
                            g.setColor(t.c_focus);
                            g.fillRect(caret, ascent, 1 + Thinlet.evm, fontheight + Thinlet.evm);
                        }
                    }
                    ascent += fontheight;
                    i = j + 1;
                }
            }
            if (customfont != null) {
                g.setFont(t.font);
            } // restore the default font
        } else if (Thinlet.is(classname, ":combolist")) {
            Object lead = Thinlet.get(component, ":lead");
            for (Object choice = Thinlet.get(Thinlet.get(component, "combobox"), ":comp");
                    choice != null;
                    choice = Thinlet.get(choice, ":next")) {
                Rectangle r = t.getRectangle(choice, "bounds");
                if (clipy + clipheight <= r.y) {
                    break;
                }
                if (clipy >= r.y + r.height) {
                    continue;
                }
                t.paint(
                        choice,
                        r.x,
                        r.y,
                        portwidth,
                        r.height,
                        g,
                        clipx,
                        clipy,
                        clipwidth,
                        clipheight,
                        false,
                        false,
                        false,
                        false,
                        2,
                        4,
                        2,
                        4,
                        false,
                        t.getBoolean(choice, "enabled", true) ? ((lead == choice) ? 's' : 't') : 'd',
                        "left",
                        false,
                        false);
            }
        } else if ((Thinlet.is(classname, "panel")) || (Thinlet.is(classname, "dialog"))) {
            for (Object comp = Thinlet.get(component, ":comp"); comp != null; comp = Thinlet.get(comp, ":next")) {
                t.paint(g, clipx, clipy, clipwidth, clipheight, comp, enabled);
            }
        } else { // if ((Thinlet.is(classname, "list")) || (Thinlet.is(classname, "table")) || (Thinlet.is(classname,
            // "tree")))
            Object lead = t.ensureLeadForPaint(component, focus);
            int[] columnwidths = (Thinlet.is(classname, "table")) ? ((int[]) Thinlet.get(component, ":widths")) : null;
            boolean line = t.getBoolean(component, "line", true);
            int iline = line ? 1 : 0;
            boolean angle = (Thinlet.is(classname, "tree")) && t.getBoolean(component, "angle", false);
            for (Object item = Thinlet.get(component, ":comp"), next = null; item != null; item = next) {
                Rectangle r = t.getRectangle(item, "bounds");
                if (clipy + clipheight <= r.y) {
                    break;
                } // clip rectangle is above
                boolean subnode = false;
                boolean expanded = false;
                if (!Thinlet.is(classname, "tree")) {
                    next = Thinlet.get(item, ":next");
                } else {
                    subnode = (next = Thinlet.get(item, ":comp")) != null;
                    expanded = subnode && t.getBoolean(item, "expanded", true);
                    if (!expanded) {
                        for (Object node = item;
                                (node != component) && ((next = Thinlet.get(node, ":next")) == null);
                                node = t.getParent(node))
                            ;
                    }
                }
                if (clipy >= r.y + r.height + iline) {
                    if (angle) { // TODO draw dashed line
                        Object nodebelow = Thinlet.get(item, ":next");
                        if (nodebelow != null) { // and the next node is bellow clipy
                            g.setColor(t.c_bg);
                            int x = r.x - t.block / 2;
                            g.drawLine(x, r.y, x, t.getRectangle(nodebelow, "bounds").y);
                        }
                    }
                    continue; // clip rectangle is bellow
                }

                boolean selected = t.getBoolean(item, "selected", false);
                t.paintRect(
                        g,
                        (!Thinlet.is(classname, "tree")) ? 0 : r.x,
                        r.y,
                        (!Thinlet.is(classname, "tree")) ? viewwidth : r.width,
                        r.height,
                        null,
                        selected ? t.c_select : t.c_textbg,
                        false,
                        false,
                        false,
                        false,
                        true);
                if (focus && (lead == item)) { // focused
                    t.drawFocus(
                            g,
                            (!Thinlet.is(classname, "tree")) ? 0 : r.x,
                            r.y,
                            ((!Thinlet.is(classname, "tree")) ? viewwidth : r.width) - 1,
                            r.height - 1);
                }
                if (line) {
                    g.setColor(t.c_bg);
                    g.drawLine(0, r.y + r.height, viewwidth, r.y + r.height);
                }
                if (!Thinlet.is(classname, "table")) { // list or tree
                    boolean itemenabled = enabled && t.getBoolean(item, "enabled", true);
                    t.paint(
                            item,
                            r.x,
                            r.y,
                            viewwidth,
                            r.height,
                            g,
                            clipx,
                            clipy,
                            clipwidth,
                            clipheight,
                            false,
                            false,
                            false,
                            false,
                            1,
                            3,
                            1,
                            3,
                            false,
                            itemenabled ? 'e' : 'd',
                            "left",
                            false,
                            false);
                    if (Thinlet.is(classname, "tree")) {
                        int x = r.x - t.block / 2;
                        int y = r.y + (r.height - 1) / 2;
                        if (angle) {
                            g.setColor(t.c_bg);
                            g.drawLine(x, r.y, x, y);
                            g.drawLine(x, y, r.x - 1, y);
                            Object nodebelow = Thinlet.get(item, ":next");
                            if (nodebelow != null) {
                                g.drawLine(x, y, x, t.getRectangle(nodebelow, "bounds").y);
                            }
                        }
                        if (subnode) {
                            t.paintRect(
                                    g,
                                    x - 4,
                                    y - 4,
                                    9,
                                    9,
                                    itemenabled ? t.c_border : t.c_disable,
                                    itemenabled ? t.c_ctrl : t.c_bg,
                                    true,
                                    true,
                                    true,
                                    true,
                                    true);
                            g.setColor(itemenabled ? t.c_text : t.c_disable);
                            g.drawLine(x - 2, y, x + 2, y);
                            if (!expanded) {
                                g.drawLine(x, y - 2, x, y + 2);
                            }
                        }
                    }
                } else { // table
                    int i = 0;
                    int x = 0;
                    for (Object cell = Thinlet.get(item, ":comp"); cell != null; cell = Thinlet.get(cell, ":next")) {
                        if (clipx + clipwidth <= x) {
                            break;
                        }
                        // column width is defined by header calculated in layout, otherwise is 80
                        int iwidth = 80;
                        if ((columnwidths != null) && (columnwidths.length > i)) {
                            iwidth = (i != columnwidths.length - 1) ? columnwidths[i] : Math.max(iwidth, viewwidth - x);
                        }
                        if (clipx < x + iwidth) {
                            boolean cellenabled = enabled && t.getBoolean(cell, "enabled", true);
                            t.paint(
                                    cell,
                                    r.x + x,
                                    r.y,
                                    iwidth,
                                    r.height - 1,
                                    g,
                                    clipx,
                                    clipy,
                                    clipwidth,
                                    clipheight,
                                    false,
                                    false,
                                    false,
                                    false,
                                    1,
                                    1,
                                    1,
                                    1,
                                    false,
                                    cellenabled ? 'e' : 'd',
                                    "left",
                                    false,
                                    false);
                        }
                        i++;
                        x += iwidth;
                    }
                }
            }
        }
    }

    /** The 2005 {@code combobox} paint branch, verbatim (editable and static forms). */
    static void combobox(
            Thinlet t,
            Object component,
            Rectangle bounds,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            boolean pressed,
            boolean inside,
            boolean focus,
            boolean enabled) {
        if (t.getBoolean(component, "editable", true)) {
            Image icon = t.getIcon(component, "icon", null);
            int left = (icon != null) ? icon.getWidth(t) : 0;
            Renderer.field(
                    t,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight,
                    component,
                    bounds.width - t.block,
                    bounds.height,
                    focus,
                    enabled,
                    false,
                    left);
            if (icon != null) {
                g.drawImage(icon, 2, (bounds.height - icon.getHeight(t)) / 2, t);
            }
            arrow(
                    t,
                    g,
                    bounds.width - t.block,
                    0,
                    t.block,
                    bounds.height,
                    'S',
                    enabled,
                    inside,
                    pressed,
                    "down",
                    true,
                    false,
                    true,
                    true,
                    true);
        } else {
            t.paint(
                    component,
                    0,
                    0,
                    bounds.width,
                    bounds.height,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight,
                    true,
                    true,
                    true,
                    true,
                    1,
                    1,
                    1,
                    1 + t.block,
                    focus,
                    enabled ? ((inside != pressed) ? 'h' : (pressed ? 'p' : 'g')) : 'd',
                    "left",
                    false,
                    false);
            g.setColor(enabled ? t.c_text : t.c_disable);
            Renderer.arrow(g, bounds.width - t.block, 0, t.block, bounds.height, 'S');
        }
    }

    /** The 2005 {@code tabbedpane} paint branch, verbatim (tab headers with hover tint + content). */
    static void tabbedpane(
            Thinlet t,
            Object component,
            Rectangle bounds,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            boolean pressed,
            boolean inside,
            boolean focus,
            boolean enabled) {
        int i = 0;
        Object selectedtab = null;
        int selected = t.getInteger(component, "selected", 0);
        String placement = t.getString(component, "placement", "top");
        boolean horizontal = ((Thinlet.is(placement, "top")) || (Thinlet.is(placement, "bottom")));
        boolean stacked = (Thinlet.is(placement, "stacked"));
        int bx = stacked ? 0 : horizontal ? 2 : 1, by = stacked ? 0 : horizontal ? 1 : 2, bw = 2 * bx, bh = 2 * by;
        // paint tabs except the selected one
        int pcx = clipx, pcy = clipy, pcw = clipwidth, pch = clipheight;
        clipx = Math.max(0, clipx);
        clipy = Math.max(0, clipy);
        clipwidth = Math.min(bounds.width, pcx + pcw) - clipx;
        clipheight = Math.min(bounds.height, pcy + pch) - clipy;
        g.clipRect(clipx, clipy, clipwidth, clipheight); // intersection of clip and bound
        for (Object tab = Thinlet.get(component, ":comp"); tab != null; tab = Thinlet.get(tab, ":next")) {
            Rectangle r = t.getRectangle(tab, "bounds");
            if (selected != i) {
                boolean hover = inside && (t.mousepressed == null) && (t.insidepart == tab);
                boolean tabenabled = enabled && t.getBoolean(tab, "enabled", true);
                t.paint(
                        tab,
                        r.x + bx,
                        r.y + by,
                        r.width - bw,
                        r.height - bh,
                        g,
                        clipx,
                        clipy,
                        clipwidth,
                        clipheight,
                        (!Thinlet.is(placement, "bottom")),
                        (!Thinlet.is(placement, "right")),
                        !stacked && (!Thinlet.is(placement, "top")),
                        (!Thinlet.is(placement, "left")),
                        1,
                        3,
                        1,
                        3,
                        false,
                        tabenabled ? (hover ? 'h' : 'g') : 'd',
                        "left",
                        true,
                        false);
            } else {
                selectedtab = tab;
                // paint tabbedpane border
                t.paint(
                        tab,
                        (Thinlet.is(placement, "left")) ? r.width - 1 : 0,
                        stacked ? (r.y + r.height - 1) : (Thinlet.is(placement, "top")) ? r.height - 1 : 0,
                        (horizontal || stacked) ? bounds.width : (bounds.width - r.width + 1),
                        stacked
                                ? (bounds.height - r.y - r.height + 1)
                                : horizontal ? (bounds.height - r.height + 1) : bounds.height,
                        g,
                        true,
                        true,
                        true,
                        true,
                        enabled ? 'e' : 'd');
                Object comp = Thinlet.get(selectedtab, ":comp");
                if ((comp != null) && t.getBoolean(comp, "visible", true)) {
                    clipx -= r.x;
                    clipy -= r.y;
                    g.translate(r.x, r.y); // relative to tab
                    t.paint(g, clipx, clipy, clipwidth, clipheight, comp, enabled);
                    clipx += r.x;
                    clipy += r.y;
                    g.translate(-r.x, -r.y);
                }
            }
            i++;
        }

        // paint selected tab and its content
        if (selectedtab != null) {
            Rectangle r = t.getRectangle(selectedtab, "bounds");
            // paint selected tab
            int ph = stacked ? 3 : (horizontal ? 5 : 4);
            int pv = stacked ? 1 : (horizontal ? 2 : 3);
            t.paint(
                    selectedtab,
                    r.x,
                    r.y,
                    r.width,
                    r.height,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight,
                    (!Thinlet.is(placement, "bottom")),
                    (!Thinlet.is(placement, "right")),
                    !stacked && (!Thinlet.is(placement, "top")),
                    (!Thinlet.is(placement, "left")),
                    pv,
                    ph,
                    pv,
                    ph,
                    focus,
                    enabled ? 'b' : 'i',
                    "left",
                    true,
                    false);
        }
        g.setClip(pcx, pcy, pcw, pch);
    }

    /** The 2005 {@code menubar} paint branch, verbatim (titles: armed/hover/grayed + trailing strip). */
    static void menubar(
            Thinlet t,
            Object component,
            Rectangle bounds,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            boolean enabled) {
        Object selected = Thinlet.get(component, "selected");
        int lastx = 0;
        for (Object menu = Thinlet.get(component, ":comp"); menu != null; menu = Thinlet.get(menu, ":next")) {
            Rectangle mb = t.getRectangle(menu, "bounds");
            if (clipx + clipwidth <= mb.x) {
                break;
            }
            if (clipx >= mb.x + mb.width) {
                continue;
            }
            boolean menuenabled = enabled && t.getBoolean(menu, "enabled", true);
            boolean armed = (selected == menu);
            boolean hoover = (selected == null) && (t.insidepart == menu);
            t.paint(
                    menu,
                    mb.x,
                    0,
                    mb.width,
                    bounds.height,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight, // TODO disabled
                    armed,
                    armed,
                    true,
                    armed,
                    1,
                    3,
                    1,
                    3,
                    false,
                    enabled ? (menuenabled ? (armed ? 's' : (hoover ? 'h' : 'g')) : 'r') : 'd',
                    "left",
                    true,
                    false);
            lastx = mb.x + mb.width;
        }
        t.paintRect(
                g,
                lastx,
                0,
                bounds.width - lastx,
                bounds.height,
                enabled ? t.c_border : t.c_disable,
                enabled ? t.c_ctrl : t.c_bg,
                false,
                false,
                true,
                false,
                true);
    }

    /** The 2005 {@code :popup} paint branch, verbatim (menu items, checkbox marks, submenu arrows, accelerators). */
    static void popup(
            Thinlet t,
            Object component,
            Rectangle bounds,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight) {
        t.paintRect(g, 0, 0, bounds.width, bounds.height, t.c_border, t.c_textbg, true, true, true, true, true);
        Object selected = Thinlet.get(component, "selected");
        for (Object menu = Thinlet.get(Thinlet.get(component, "menu"), ":comp");
                menu != null;
                menu = Thinlet.get(menu, ":next")) {
            Rectangle r = t.getRectangle(menu, "bounds");
            if (clipy + clipheight <= r.y) {
                break;
            }
            if (clipy >= r.y + r.height) {
                continue;
            }
            String itemclass = Thinlet.getClass(menu);
            if (Thinlet.is(itemclass, "separator")) {
                g.setColor(t.c_border);
                g.fillRect(r.x, r.y, bounds.width - 2 + t.evm, r.height + t.evm);
            } else {
                boolean armed = (selected == menu);
                boolean menuenabled = t.getBoolean(menu, "enabled", true);
                t.paint(
                        menu,
                        r.x,
                        r.y,
                        bounds.width - 2,
                        r.height,
                        g,
                        clipx,
                        clipy,
                        clipwidth,
                        clipheight,
                        false,
                        false,
                        false,
                        false,
                        2,
                        (Thinlet.is(itemclass, "checkboxmenuitem")) ? (t.block + 7) : 4,
                        2,
                        4,
                        false,
                        menuenabled ? (armed ? 's' : 't') : 'd',
                        "left",
                        true,
                        false);
                if (Thinlet.is(itemclass, "checkboxmenuitem")) {
                    boolean checked = t.getBoolean(menu, "selected", false);
                    String group = t.getString(menu, "group", null);
                    g.translate(r.x + 4, r.y + 2);
                    g.setColor(menuenabled ? t.c_border : t.c_disable);
                    if (group == null) {
                        g.drawRect(1, 1, t.block - 3, t.block - 3);
                    } else {
                        g.drawOval(1, 1, t.block - 3, t.block - 3);
                    }
                    if (checked) {
                        g.setColor(menuenabled ? t.c_text : t.c_disable);
                        if (group == null) {
                            g.fillRect(3, t.block - 9, 2 + t.evm, 6 + t.evm);
                            g.drawLine(3, t.block - 4, t.block - 4, 3);
                            g.drawLine(4, t.block - 4, t.block - 4, 4);
                        } else {
                            g.fillOval(5, 5, t.block - 10 + t.evm, t.block - 10 + t.evm);
                            g.drawOval(4, 4, t.block - 9, t.block - 9);
                        }
                    }
                    g.translate(-r.x - 4, -r.y - 2);
                }
                if (Thinlet.is(itemclass, "menu")) {
                    Renderer.arrow(g, r.x + bounds.width - t.block, r.y, t.block, r.height, 'E');
                } else {
                    String accelerator = t.getAccelerator(menu);
                    if (accelerator != null) { // TODO
                        g.drawString(
                                accelerator,
                                bounds.width - 4 - t.getFontMetrics(t.font).stringWidth(accelerator),
                                r.y + 2 + 10);
                    }
                }
            }
        }
    }

    /** The 2005 {@code progressbar} paint branch, verbatim. */
    static void progressbar(Thinlet t, Object component, Rectangle bounds, Graphics g, boolean enabled) {
        int minimum = t.getInteger(component, "minimum", 0);
        int maximum = t.getInteger(component, "maximum", 100);
        int value = t.getInteger(component, "value", 0);
        // fixed by by Mike Hartshorn and Timothy Stack
        boolean horizontal = (!Thinlet.is(Thinlet.get(component, "orientation"), "vertical"));
        int length = (value - minimum) * ((horizontal ? bounds.width : bounds.height) - 1) / (maximum - minimum);
        t.paintRect(
                g,
                0,
                0,
                horizontal ? length : bounds.width,
                horizontal ? bounds.height : length,
                enabled ? t.c_border : t.c_disable,
                t.c_select,
                true,
                true,
                horizontal,
                !horizontal,
                true);
        t.paintRect(
                g,
                horizontal ? length : 0,
                horizontal ? 0 : length,
                horizontal ? (bounds.width - length) : bounds.width,
                horizontal ? bounds.height : (bounds.height - length),
                enabled ? t.c_border : t.c_disable,
                t.c_bg,
                true,
                true,
                true,
                true,
                true);
    }

    /** The 2005 {@code slider} paint branch, verbatim (track, knob, focus rect). */
    static void slider(Thinlet t, Object component, Rectangle bounds, Graphics g, boolean focus, boolean enabled) {
        if (focus) {
            t.drawFocus(g, 0, 0, bounds.width - 1, bounds.height - 1);
        }
        int minimum = t.getInteger(component, "minimum", 0);
        int maximum = t.getInteger(component, "maximum", 100);
        int value = t.getInteger(component, "value", 0);
        boolean horizontal = (!Thinlet.is(Thinlet.get(component, "orientation"), "vertical"));
        int length = (value - minimum) * ((horizontal ? bounds.width : bounds.height) - t.block) / (maximum - minimum);
        t.paintRect(
                g,
                horizontal ? 0 : 3,
                horizontal ? 3 : 0,
                horizontal ? length : (bounds.width - 6),
                horizontal ? (bounds.height - 6) : length,
                enabled ? t.c_border : t.c_disable,
                t.c_bg,
                true,
                true,
                horizontal,
                !horizontal,
                true);
        t.paintRect(
                g,
                horizontal ? length : 0,
                horizontal ? 0 : length,
                horizontal ? t.block : bounds.width,
                horizontal ? bounds.height : t.block,
                enabled ? t.c_border : t.c_disable,
                enabled ? t.c_ctrl : t.c_bg,
                true,
                true,
                true,
                true,
                true);
        t.paintRect(
                g,
                horizontal ? (t.block + length) : 3,
                horizontal ? 3 : (t.block + length),
                bounds.width - (horizontal ? (t.block + length) : 6),
                bounds.height - (horizontal ? 6 : (t.block + length)),
                enabled ? t.c_border : t.c_disable,
                t.c_bg,
                horizontal,
                !horizontal,
                true,
                true,
                true);
    }

    /** The 2005 {@code splitpane} paint branch, verbatim (divider strip, grip, focus rect, both panes). */
    static void splitpane(
            Thinlet t,
            Object component,
            Rectangle bounds,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            boolean focus,
            boolean enabled) {
        boolean horizontal = (!Thinlet.is(Thinlet.get(component, "orientation"), "vertical"));
        int divider = t.getInteger(component, "divider", -1);
        t.paintRect(
                g,
                horizontal ? divider : 0,
                horizontal ? 0 : divider,
                horizontal ? 5 : bounds.width,
                horizontal ? bounds.height : 5,
                t.c_border,
                t.c_bg,
                false,
                false,
                false,
                false,
                true);
        if (focus) {
            if (horizontal) {
                t.drawFocus(g, divider, 0, 4, bounds.height - 1);
            } else {
                t.drawFocus(g, 0, divider, bounds.width - 1, 4);
            }
        }
        g.setColor(enabled ? t.c_border : t.c_disable);
        int xy = horizontal ? bounds.height : bounds.width;
        int xy1 = Math.max(0, xy / 2 - 12);
        int xy2 = Math.min(xy / 2 + 12, xy - 1);
        for (int i = divider + 1; i < divider + 4; i += 2) {
            if (horizontal) {
                g.drawLine(i, xy1, i, xy2);
            } else {
                g.drawLine(xy1, i, xy2, i);
            }
        }
        Object comp1 = Thinlet.get(component, ":comp");
        if (comp1 != null) {
            t.paint(g, clipx, clipy, clipwidth, clipheight, comp1, enabled);
            Object comp2 = Thinlet.get(comp1, ":next");
            if (comp2 != null) {
                t.paint(g, clipx, clipy, clipwidth, clipheight, comp2, enabled);
            }
        }
    }

    /** The 2005 {@code panel}/{@code dialog} paint branch, verbatim (title bar + controls, border, children/port). */
    static void container(
            Thinlet t,
            Object component,
            String classname,
            Rectangle bounds,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            boolean pressed,
            boolean inside,
            boolean focus,
            boolean enabled) {
        int titleheight = t.getInteger(component, ":titleheight", 0);
        if (Thinlet.is(classname, "dialog")) {
            t.paint(
                    component,
                    0,
                    0,
                    bounds.width,
                    3 + titleheight,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight,
                    true,
                    true,
                    false,
                    true,
                    1,
                    2,
                    1,
                    2,
                    false,
                    'g',
                    "left",
                    false,
                    false);
            int controlx = bounds.width - titleheight - 1;
            if (t.getBoolean(component, "closable", false)) {
                t.paint(component, g, controlx, 3, titleheight - 2, titleheight - 2, 'c');
                controlx -= titleheight;
            }
            if (t.getBoolean(component, "maximizable", false)) {
                t.paint(component, g, controlx, 3, titleheight - 2, titleheight - 2, 'm');
                controlx -= titleheight;
            }
            if (t.getBoolean(component, "iconifiable", false)) {
                t.paint(component, g, controlx, 3, titleheight - 2, titleheight - 2, 'i');
            }
            t.paintRect(
                    g,
                    0,
                    3 + titleheight,
                    bounds.width,
                    bounds.height - 3 - titleheight,
                    t.c_border,
                    t.c_press,
                    false,
                    true,
                    true,
                    true,
                    true); // lower part excluding titlebar
            t.paint(
                    component, // content area
                    3,
                    3 + titleheight,
                    bounds.width - 6,
                    bounds.height - 6 - titleheight,
                    g,
                    true,
                    true,
                    true,
                    true,
                    'b');
        } else { // panel
            boolean border = t.getBoolean(component, "border", false);
            t.paint(
                    component,
                    0,
                    titleheight / 2,
                    bounds.width,
                    bounds.height - (titleheight / 2),
                    g,
                    border,
                    border,
                    border,
                    border,
                    enabled ? 'e' : 'd');
            t.paint(
                    component,
                    0,
                    0,
                    bounds.width,
                    titleheight, // panel title
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight,
                    false,
                    false,
                    false,
                    false,
                    0,
                    3,
                    0,
                    3,
                    false,
                    enabled ? 'x' : 'd',
                    "left",
                    false,
                    false);
        }

        if (Thinlet.get(component, ":port") != null) {
            scroll(
                    t,
                    component,
                    classname,
                    pressed,
                    inside,
                    focus,
                    false,
                    enabled,
                    g,
                    clipx,
                    clipy,
                    clipwidth,
                    clipheight);
        } else {
            for (Object comp = Thinlet.get(component, ":comp"); comp != null; comp = Thinlet.get(comp, ":next")) {
                t.paint(g, clipx, clipy, clipwidth, clipheight, comp, enabled);
            }
        }
    }

    /** The 2005 {@code spinbox} paint branch, verbatim (field body + up/down arrows). */
    static void spinbox(
            Thinlet t,
            Object component,
            Rectangle bounds,
            Graphics g,
            int clipx,
            int clipy,
            int clipwidth,
            int clipheight,
            boolean pressed,
            boolean inside,
            boolean focus,
            boolean enabled) {
        field(
                t,
                g,
                clipx,
                clipy,
                clipwidth,
                clipheight,
                component,
                bounds.width - t.block,
                bounds.height,
                focus,
                enabled,
                false,
                0);
        arrow(
                t,
                g,
                bounds.width - t.block,
                0,
                t.block,
                bounds.height / 2,
                'N',
                enabled,
                inside,
                pressed,
                "up",
                true,
                false,
                false,
                true,
                true);
        arrow(
                t,
                g,
                bounds.width - t.block,
                bounds.height / 2,
                t.block,
                bounds.height - (bounds.height / 2),
                'S',
                enabled,
                inside,
                pressed,
                "down",
                true,
                false,
                true,
                true,
                true);
    }
}
