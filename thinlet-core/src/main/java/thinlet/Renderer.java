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

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Per-widget paint branches extracted from {@code Thinlet}'s recursive paint —
 * the Cut 2 "typed Renderer" seam (DECISIONS.md D42/D48), grown one widget
 * branch at a time. Behavior-preserving by contract: each method body is the
 * 2005 branch moved verbatim, guarded by the golden + interaction-golden net.
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
}
