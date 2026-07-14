/* Thinlet GUI toolkit - www.thinlet.com
 * Copyright (C) 2002-2005 Robert Bajzat (rbajzat@freemail.hu)
 *
 * This file is part of the Thinlet modernization fork; its definition table is
 * relocated verbatim from the 2005 Thinlet.java and remains under the original
 * license and copyright.
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
 * The 2005 widget/attribute definition table, relocated verbatim from {@code Thinlet}
 * (Cut 3, DECISIONS.md D58). Read by {@code Thinlet.create}/{@code instance}/{@code getDefinition}.
 */
final class DescriptorTable {

    private DescriptorTable() {}

    static final Object[] DTD;

    static {
        Integer integer_1 = new Integer(-1);
        Integer integer0 = new Integer(0);
        Integer integer1 = new Integer(1);
        String[] orientation = {"horizontal", "vertical"};
        String[] leftcenterright = {"left", "center", "right"};
        String[] selections = {"single", "interval", "multiple"}; // +none
        DTD = new Object[] {
            "component", null,
                    new Object[][] {
                        {"string", "name", null, null},
                        {"boolean", "enabled", "paint", Boolean.TRUE},
                        {"boolean", "visible", "parent", Boolean.TRUE},
                        {"string", "tooltip", null, null},
                        {"font", "font", "validate", null},
                        {"color", "foreground", "paint", null},
                        {"color", "background", "paint", null},
                        {"integer", "width", "validate", integer0},
                        {"integer", "height", "validate", integer0},
                        {"integer", "colspan", "validate", integer1},
                        {"integer", "rowspan", "validate", integer1},
                        {"integer", "weightx", "validate", integer0},
                        {"integer", "weighty", "validate", integer0},
                        {"choice", "halign", "validate", new String[] {"fill", "center", "left", "right"}},
                        {"choice", "valign", "validate", new String[] {"fill", "center", "top", "bottom"}},
                        // component class String null*
                        // parent Object null
                        // (bounds) Rectangle 0 0 0 0
                        {"property", "property", null, null},
                        {"method", "init"},
                        {"method", "focuslost"},
                        {"method", "focusgained"}
                    },
            "label", "component",
                    new Object[][] {
                        {"string", "text", "validate", null},
                        {"icon", "icon", "validate", null},
                        {"choice", "alignment", "validate", leftcenterright},
                        {"integer", "mnemonic", "paint", integer_1},
                        {"component", "for", null, null}
                    },
            "button", "label",
                    new Object[][] {
                        {"choice", "alignment", "validate", new String[] {"center", "left", "right"}},
                        {"method", "action"},
                        {"choice", "type", "paint", new String[] {"normal", "default", "cancel", "link"}}
                    },
            "checkbox", "label",
                    new Object[][] {
                        {"boolean", "selected", "paint", Boolean.FALSE}, // ...group
                        {"string", "group", "paint", null}, // ...group
                        {"method", "action"}
                    },
            "togglebutton", "checkbox", null,
            "combobox", "textfield",
                    new Object[][] {
                        {"icon", "icon", "validate", null},
                        {"integer", "selected", "layout", integer_1}
                    },
            "choice", null,
                    new Object[][] {
                        {"string", "name", null, null},
                        {"boolean", "enabled", "paint", Boolean.TRUE},
                        {"string", "text", "parent", null},
                        {"icon", "icon", "parent", null},
                        {"choice", "alignment", "parent", leftcenterright},
                        {"string", "tooltip", null, null},
                        {"font", "font", "validate", null},
                        {"color", "foreground", "paint", null},
                        {"color", "background", "paint", null},
                        {"property", "property", null, null}
                    },
            "textfield", "component",
                    new Object[][] {
                        {"string", "text", "layout", ""},
                        {"integer", "columns", "validate", integer0},
                        {"boolean", "editable", "paint", Boolean.TRUE},
                        {"choice", "alignment", "validate", leftcenterright},
                        {"integer", "start", "layout", integer0},
                        {"integer", "end", "layout", integer0},
                        {"method", "action"},
                        {"method", "insert"},
                        {"method", "remove"},
                        {"method", "caret"},
                        {"method", "perform"}
                    },
            "passwordfield", "textfield", null,
            "textarea", "textfield",
                    new Object[][] {
                        {"integer", "rows", "validate", integer0},
                        {"boolean", "border", "validate", Boolean.TRUE},
                        {"boolean", "wrap", "layout", Boolean.FALSE}
                    },
            "tabbedpane", "component",
                    new Object[][] {
                        {"choice", "placement", "validate", new String[] {"top", "left", "bottom", "right", "stacked"}},
                        {"integer", "selected", "paint", integer0},
                        {"method", "action"}
                    }, // ...focus
            "tab", "choice", new Object[][] {{"integer", "mnemonic", "paint", integer_1}},
            "panel", "component",
                    new Object[][] {
                        {"integer", "columns", "validate", integer0},
                        {"integer", "top", "validate", integer0},
                        {"integer", "left", "validate", integer0},
                        {"integer", "bottom", "validate", integer0},
                        {"integer", "right", "validate", integer0},
                        {"integer", "gap", "validate", integer0},
                        {"string", "text", "validate", null},
                        {"icon", "icon", "validate", null},
                        {"boolean", "border", "validate", Boolean.FALSE},
                        {"boolean", "scrollable", "validate", Boolean.FALSE}
                    },
            "desktop", "component", null,
            "dialog", "panel",
                    new Object[][] {
                        {"boolean", "modal", null, Boolean.FALSE},
                        {"boolean", "resizable", null, Boolean.FALSE},
                        {"boolean", "closable", "paint", Boolean.FALSE},
                        {"boolean", "maximizable", "paint", Boolean.FALSE},
                        {"boolean", "iconifiable", "paint", Boolean.FALSE}
                    },
            "spinbox", "textfield",
                    new Object[][] {
                        {"integer", "minimum", null, new Integer(Integer.MIN_VALUE)},
                        {"integer", "maximum", null, new Integer(Integer.MAX_VALUE)},
                        {"integer", "step", null, integer1},
                        {"integer", "value", null, integer0}
                    }, // == text? deprecated
            "progressbar", "component",
                    new Object[][] {
                        {"choice", "orientation", "validate", orientation},
                        {"integer", "minimum", "paint", integer0}, // ...checkvalue
                        {"integer", "maximum", "paint", new Integer(100)},
                        {"integer", "value", "paint", integer0}
                    },
            // change stringpainted
            "slider", "progressbar",
                    new Object[][] {
                        {"integer", "unit", null, new Integer(5)},
                        {"integer", "block", null, new Integer(25)},
                        {"method", "action"}
                    },
            // minor/majortickspacing
            // inverted
            // labelincrement labelstart
            "splitpane", "component",
                    new Object[][] {
                        {"choice", "orientation", "validate", orientation},
                        {"integer", "divider", "layout", integer_1}
                    },
            "list", "component",
                    new Object[][] {
                        {"choice", "selection", "paint", selections},
                        {"method", "action"},
                        {"method", "perform"},
                        {"boolean", "line", "validate", Boolean.TRUE}
                    },
            "item", "choice", new Object[][] {{"boolean", "selected", null, Boolean.FALSE}},
            "table", "list", new Object[][] {
                        /*{ "choice", "selection",
                        new String[] { "singlerow", "rowinterval", "multiplerow",
                        	"cell", "cellinterval",
                        	"singlecolumn", "columninterval", "multiplecolumn" } }*/
                    },
            "header", null, null,
            // reordering allowed
            // autoresize mode: off next (column boundries) subsequents last all columns
            // column row selection
            // selection row column cell
            // editing row/column
            "column", "choice",
                    new Object[][] {
                        {"integer", "width", null, new Integer(80)},
                        {"choice", "sort", null, new String[] {"none", "ascent", "descent"}}
                    },
            "row", null, new Object[][] {{"boolean", "selected", null, Boolean.FALSE}},
            "cell", "choice", null,
            "tree", "list",
                    new Object[][] {
                        {"boolean", "angle", null, Boolean.FALSE},
                        {"method", "expand"},
                        {"method", "collapse"}
                    },
            "node", "choice",
                    new Object[][] {
                        {"boolean", "selected", null, Boolean.FALSE},
                        {"boolean", "expanded", null, Boolean.TRUE}
                    },
            "separator", "component", null,
            "menubar", "component", null,
            "menu", "choice", new Object[][] {{"integer", "mnemonic", "paint", integer_1}},
            "menuitem", "choice",
                    new Object[][] {
                        {"keystroke", "accelerator", null, null},
                        {"method", "action"},
                        {"integer", "mnemonic", "paint", integer_1}
                    },
            "checkboxmenuitem", "menuitem",
                    new Object[][] {
                        {"boolean", "selected", "paint", Boolean.FALSE}, // ...group
                        {"string", "group", "paint", null}
                    }, // ...group
            "popupmenu", "component", new Object[][] {{"method", "menushown"}}, // Post menu: Shift+F10
            "bean", "component", new Object[][] {{"bean", "bean", null, null}}
        };
    }
}
