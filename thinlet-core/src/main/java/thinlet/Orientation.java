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
 * The {@code orientation} choice vocabulary of the 2005 DTD (DECISIONS.md D74).
 * pinned: PublicVocabularyContractTest
 */
public enum Orientation {
    HORIZONTAL("horizontal"),
    VERTICAL("vertical");

    /** The DTD attribute name this vocabulary belongs to. */
    public static final String KEY = "orientation";

    private final String token;

    Orientation(String token) {
        this.token = token;
    }

    /** The DTD token for this constant. */
    public String token() {
        return token;
    }

    /** The constant whose token equals {@code token}; anything else throws {@link IllegalArgumentException}. */
    public static Orientation fromToken(String token) {
        for (Orientation value : values()) {
            if (value.token.equals(token)) {
                return value;
            }
        }
        throw new IllegalArgumentException("unknown " + token + " for " + KEY);
    }
}
