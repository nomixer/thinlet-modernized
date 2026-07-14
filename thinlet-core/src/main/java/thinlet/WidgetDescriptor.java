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
 * One widget class of the definition table — the typed form of a 2005
 * {@code {classname, parentClassname, attributes}} triple (DECISIONS.md D58).
 */
final class WidgetDescriptor {

    // The canonical interned classname literal; ":class" holds this object.
    final String name;
    // Kept as a NAME re-looked-up by identity each hop (not a direct reference):
    // transliterates the 2005 parent walk exactly, quirk included (D58).
    final String parent;
    final AttributeDescriptor[] attributes; // null where the 2005 table had null

    WidgetDescriptor(String name, String parent, AttributeDescriptor[] attributes) {
        this.name = name;
        this.parent = parent;
        this.attributes = attributes;
    }
}
