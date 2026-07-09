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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
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
 * surface until the later new-API phase. Dispatch (the classname chain) stays
 * in {@code Thinlet.paint}.
 */
final class Renderer {

    private Renderer() {}

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
}
