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
 * One attribute row of the definition table — the typed form of the 2005
 * {@code {type, name, invalidateMode, default}} tuple (DECISIONS.md D58).
 */
final class AttributeDescriptor {

    final String type;
    // The canonical interned key: the model layer stores and compares by the
    // IDENTITY of this object. pinned: DescriptorContractTest
    final String name;
    final String invalidate;
    final Object defaultValue;

    AttributeDescriptor(String type, String name, String invalidate, Object defaultValue) {
        this.type = type;
        this.name = name;
        this.invalidate = invalidate;
        this.defaultValue = defaultValue;
    }

    // The 2005 method rows are 2-element {type, name} tuples.
    AttributeDescriptor(String type, String name) {
        this(type, name, null, null);
    }
}
