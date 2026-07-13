/* Thinlet GUI toolkit (modernization fork) — new code, not relocated 2005 source.
 *
 * This file is part of the Thinlet modernization fork of the Thinlet GUI
 * toolkit (Copyright (C) 2002-2005 Robert Bajzat, www.thinlet.com) and is
 * distributed under the library's license.
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

/**
 * Typed parameter object for the icon+text {@code paint} dispatcher
 * ({@code Thinlet.paint(Object, Graphics, IconTextSpec)}) — the
 * drawing-vocabulary typing slice (DECISIONS.md D56). Replaces the 2005
 * 23-parameter positional signature with named groups: required box + state
 * {@code mode} (constructor), then clip, border edges, padding, focus,
 * alignment, mnemonic, underline (fluent, all defaulted).
 *
 * <p>A fluent mutable spec — the builder is its own product (Java 8; one
 * allocation per call). <b>Create a fresh instance at every call site; never
 * cache or reuse one</b>: a reused mutable spec is exactly the shared-state
 * hazard the D48 stateless seam style exists to avoid. This is a transient
 * data carrier, not a stateful subsystem, so it is D48-compatible; it stays
 * package-private through 3a (D43) and is japicmp-invisible.
 *
 * <p>{@code mode} keeps the 2005 single-char state vocabulary ({@code 'e'}
 * enabled, {@code 'l'} link, {@code 'd'} disabled, {@code 'g'}/{@code 'r'}
 * gradient, {@code 'b'}/{@code 'i'}/{@code 'x'} background variants,
 * {@code 'h'} hover, {@code 'p'} pressed, {@code 't'} textbg, {@code 's'}
 * selected) — an enum would rewrite the verbatim 2005 switch bodies, out of
 * scope for this slice (D56).
 */
final class IconTextSpec {

    final int x;
    final int y;
    final int width;
    final int height;
    final char mode;

    int clipx;
    int clipy;
    int clipwidth;
    int clipheight;
    boolean top;
    boolean left;
    boolean bottom;
    boolean right;
    int toppadding;
    int leftpadding;
    int bottompadding;
    int rightpadding;
    boolean focus;
    String alignment = "left";
    boolean mnemonic;
    boolean underline;

    IconTextSpec(int x, int y, int width, int height, char mode) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.mode = mode;
    }

    /** The clip rectangle restored after any content clipping. */
    IconTextSpec clip(int clipx, int clipy, int clipwidth, int clipheight) {
        this.clipx = clipx;
        this.clipy = clipy;
        this.clipwidth = clipwidth;
        this.clipheight = clipheight;
        return this;
    }

    /** Which of the four border edges to draw (each drawn edge shrinks the content box by 1px). */
    IconTextSpec border(boolean top, boolean left, boolean bottom, boolean right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        return this;
    }

    /** Content padding inside the (border-shrunk) box. */
    IconTextSpec padding(int top, int left, int bottom, int right) {
        this.toppadding = top;
        this.leftpadding = left;
        this.bottompadding = bottom;
        this.rightpadding = right;
        return this;
    }

    /** Draw the dotted focus rectangle. */
    IconTextSpec focus(boolean focus) {
        this.focus = focus;
        return this;
    }

    /**
     * Default text alignment ({@code "left"}/{@code "center"}/{@code "right"}); the component's own
     * {@code alignment} property overrides it at paint time.
     */
    IconTextSpec align(String alignment) {
        this.alignment = alignment;
        return this;
    }

    /** Underline the component's mnemonic character (when a {@code mnemonic} attribute is set). */
    IconTextSpec mnemonic(boolean mnemonic) {
        this.mnemonic = mnemonic;
        return this;
    }

    /** Underline the whole text (hovered link-button style). */
    IconTextSpec underline(boolean underline) {
        this.underline = underline;
        return this;
    }
}
