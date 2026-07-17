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
 * The event vocabulary — the name of every {@code method}-typed attribute row of the
 * definition table (DECISIONS.md D74). pinned: PublicVocabularyContractTest
 */
public final class EventNames {

    public static final String INIT = "init";
    public static final String FOCUS_LOST = "focuslost";
    public static final String FOCUS_GAINED = "focusgained";
    public static final String ACTION = "action";
    public static final String INSERT = "insert";
    public static final String REMOVE = "remove";
    public static final String CARET = "caret";
    public static final String PERFORM = "perform";
    public static final String EXPAND = "expand";
    public static final String COLLAPSE = "collapse";
    public static final String MENU_SHOWN = "menushown";

    private EventNames() {}
}
