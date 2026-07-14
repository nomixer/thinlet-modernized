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
 * The 2005 widget/attribute definition table, typed (Cut 3, DECISIONS.md D58);
 * the data is relocated 2005 source. Read by {@code Thinlet.create}/{@code
 * instance}/{@code getDefinition}.
 */
final class DescriptorTable {

    private DescriptorTable() {}

    // Attribute vocabulary (checkable below): types "string" "boolean" "integer"
    // "choice" "icon" "method" "component" "property" "font" "color" "keystroke"
    // "bean"; invalidate modes "validate" "paint" "layout" "parent" or null. For
    // "choice" rows defaultValue holds the allowed String[] (default = its first
    // element). pinned: DescriptorContractTest
    static final WidgetDescriptor[] WIDGETS;

    static {
        Integer integer_1 = new Integer(-1);
        Integer integer0 = new Integer(0);
        Integer integer1 = new Integer(1);
        String[] orientation = {"horizontal", "vertical"};
        String[] leftcenterright = {"left", "center", "right"};
        String[] selections = {"single", "interval", "multiple"}; // +none
        WIDGETS = new WidgetDescriptor[] {
            new WidgetDescriptor("component", null, new AttributeDescriptor[] {
                new AttributeDescriptor("string", "name", null, null),
                new AttributeDescriptor("boolean", "enabled", "paint", Boolean.TRUE),
                new AttributeDescriptor("boolean", "visible", "parent", Boolean.TRUE),
                new AttributeDescriptor("string", "tooltip", null, null),
                new AttributeDescriptor("font", "font", "validate", null),
                new AttributeDescriptor("color", "foreground", "paint", null),
                new AttributeDescriptor("color", "background", "paint", null),
                new AttributeDescriptor("integer", "width", "validate", integer0),
                new AttributeDescriptor("integer", "height", "validate", integer0),
                new AttributeDescriptor("integer", "colspan", "validate", integer1),
                new AttributeDescriptor("integer", "rowspan", "validate", integer1),
                new AttributeDescriptor("integer", "weightx", "validate", integer0),
                new AttributeDescriptor("integer", "weighty", "validate", integer0),
                new AttributeDescriptor(
                        "choice", "halign", "validate", new String[] {"fill", "center", "left", "right"}),
                new AttributeDescriptor(
                        "choice", "valign", "validate", new String[] {"fill", "center", "top", "bottom"}),
                // component class String null*
                // parent Object null
                // (bounds) Rectangle 0 0 0 0
                new AttributeDescriptor("property", "property", null, null),
                new AttributeDescriptor("method", "init"),
                new AttributeDescriptor("method", "focuslost"),
                new AttributeDescriptor("method", "focusgained")
            }),
            new WidgetDescriptor("label", "component", new AttributeDescriptor[] {
                new AttributeDescriptor("string", "text", "validate", null),
                new AttributeDescriptor("icon", "icon", "validate", null),
                new AttributeDescriptor("choice", "alignment", "validate", leftcenterright),
                new AttributeDescriptor("integer", "mnemonic", "paint", integer_1),
                new AttributeDescriptor("component", "for", null, null)
            }),
            new WidgetDescriptor("button", "label", new AttributeDescriptor[] {
                new AttributeDescriptor("choice", "alignment", "validate", new String[] {"center", "left", "right"}),
                new AttributeDescriptor("method", "action"),
                new AttributeDescriptor("choice", "type", "paint", new String[] {"normal", "default", "cancel", "link"})
            }),
            new WidgetDescriptor("checkbox", "label", new AttributeDescriptor[] {
                new AttributeDescriptor("boolean", "selected", "paint", Boolean.FALSE), // ...group
                new AttributeDescriptor("string", "group", "paint", null), // ...group
                new AttributeDescriptor("method", "action")
            }),
            new WidgetDescriptor("togglebutton", "checkbox", null),
            new WidgetDescriptor("combobox", "textfield", new AttributeDescriptor[] {
                new AttributeDescriptor("icon", "icon", "validate", null),
                new AttributeDescriptor("integer", "selected", "layout", integer_1)
            }),
            new WidgetDescriptor("choice", null, new AttributeDescriptor[] {
                new AttributeDescriptor("string", "name", null, null),
                new AttributeDescriptor("boolean", "enabled", "paint", Boolean.TRUE),
                new AttributeDescriptor("string", "text", "parent", null),
                new AttributeDescriptor("icon", "icon", "parent", null),
                new AttributeDescriptor("choice", "alignment", "parent", leftcenterright),
                new AttributeDescriptor("string", "tooltip", null, null),
                new AttributeDescriptor("font", "font", "validate", null),
                new AttributeDescriptor("color", "foreground", "paint", null),
                new AttributeDescriptor("color", "background", "paint", null),
                new AttributeDescriptor("property", "property", null, null)
            }),
            new WidgetDescriptor("textfield", "component", new AttributeDescriptor[] {
                new AttributeDescriptor("string", "text", "layout", ""),
                new AttributeDescriptor("integer", "columns", "validate", integer0),
                new AttributeDescriptor("boolean", "editable", "paint", Boolean.TRUE),
                new AttributeDescriptor("choice", "alignment", "validate", leftcenterright),
                new AttributeDescriptor("integer", "start", "layout", integer0),
                new AttributeDescriptor("integer", "end", "layout", integer0),
                new AttributeDescriptor("method", "action"),
                new AttributeDescriptor("method", "insert"),
                new AttributeDescriptor("method", "remove"),
                new AttributeDescriptor("method", "caret"),
                new AttributeDescriptor("method", "perform")
            }),
            new WidgetDescriptor("passwordfield", "textfield", null),
            new WidgetDescriptor("textarea", "textfield", new AttributeDescriptor[] {
                new AttributeDescriptor("integer", "rows", "validate", integer0),
                new AttributeDescriptor("boolean", "border", "validate", Boolean.TRUE),
                new AttributeDescriptor("boolean", "wrap", "layout", Boolean.FALSE)
            }),
            new WidgetDescriptor("tabbedpane", "component", new AttributeDescriptor[] {
                new AttributeDescriptor(
                        "choice", "placement", "validate", new String[] {"top", "left", "bottom", "right", "stacked"}),
                new AttributeDescriptor("integer", "selected", "paint", integer0),
                new AttributeDescriptor("method", "action")
            }), // ...focus
            new WidgetDescriptor("tab", "choice", new AttributeDescriptor[] {
                new AttributeDescriptor("integer", "mnemonic", "paint", integer_1)
            }),
            new WidgetDescriptor("panel", "component", new AttributeDescriptor[] {
                new AttributeDescriptor("integer", "columns", "validate", integer0),
                new AttributeDescriptor("integer", "top", "validate", integer0),
                new AttributeDescriptor("integer", "left", "validate", integer0),
                new AttributeDescriptor("integer", "bottom", "validate", integer0),
                new AttributeDescriptor("integer", "right", "validate", integer0),
                new AttributeDescriptor("integer", "gap", "validate", integer0),
                new AttributeDescriptor("string", "text", "validate", null),
                new AttributeDescriptor("icon", "icon", "validate", null),
                new AttributeDescriptor("boolean", "border", "validate", Boolean.FALSE),
                new AttributeDescriptor("boolean", "scrollable", "validate", Boolean.FALSE)
            }),
            new WidgetDescriptor("desktop", "component", null),
            new WidgetDescriptor("dialog", "panel", new AttributeDescriptor[] {
                new AttributeDescriptor("boolean", "modal", null, Boolean.FALSE),
                new AttributeDescriptor("boolean", "resizable", null, Boolean.FALSE),
                new AttributeDescriptor("boolean", "closable", "paint", Boolean.FALSE),
                new AttributeDescriptor("boolean", "maximizable", "paint", Boolean.FALSE),
                new AttributeDescriptor("boolean", "iconifiable", "paint", Boolean.FALSE)
            }),
            new WidgetDescriptor("spinbox", "textfield", new AttributeDescriptor[] {
                new AttributeDescriptor("integer", "minimum", null, new Integer(Integer.MIN_VALUE)),
                new AttributeDescriptor("integer", "maximum", null, new Integer(Integer.MAX_VALUE)),
                new AttributeDescriptor("integer", "step", null, integer1),
                new AttributeDescriptor("integer", "value", null, integer0)
            }), // == text? deprecated
            new WidgetDescriptor("progressbar", "component", new AttributeDescriptor[] {
                new AttributeDescriptor("choice", "orientation", "validate", orientation),
                new AttributeDescriptor("integer", "minimum", "paint", integer0), // ...checkvalue
                new AttributeDescriptor("integer", "maximum", "paint", new Integer(100)),
                new AttributeDescriptor("integer", "value", "paint", integer0)
            }),
            // change stringpainted
            new WidgetDescriptor("slider", "progressbar", new AttributeDescriptor[] {
                new AttributeDescriptor("integer", "unit", null, new Integer(5)),
                new AttributeDescriptor("integer", "block", null, new Integer(25)),
                new AttributeDescriptor("method", "action")
            }),
            // minor/majortickspacing
            // inverted
            // labelincrement labelstart
            new WidgetDescriptor("splitpane", "component", new AttributeDescriptor[] {
                new AttributeDescriptor("choice", "orientation", "validate", orientation),
                new AttributeDescriptor("integer", "divider", "layout", integer_1)
            }),
            new WidgetDescriptor("list", "component", new AttributeDescriptor[] {
                new AttributeDescriptor("choice", "selection", "paint", selections),
                new AttributeDescriptor("method", "action"),
                new AttributeDescriptor("method", "perform"),
                new AttributeDescriptor("boolean", "line", "validate", Boolean.TRUE)
            }),
            new WidgetDescriptor("item", "choice", new AttributeDescriptor[] {
                new AttributeDescriptor("boolean", "selected", null, Boolean.FALSE)
            }),
            new WidgetDescriptor(
                    "table",
                    "list",
                    new AttributeDescriptor[] {
                        /*{ "choice", "selection",
                        new String[] { "singlerow", "rowinterval", "multiplerow",
                        	"cell", "cellinterval",
                        	"singlecolumn", "columninterval", "multiplecolumn" } }*/
                    }),
            new WidgetDescriptor("header", null, null),
            // reordering allowed
            // autoresize mode: off next (column boundries) subsequents last all columns
            // column row selection
            // selection row column cell
            // editing row/column
            new WidgetDescriptor("column", "choice", new AttributeDescriptor[] {
                new AttributeDescriptor("integer", "width", null, new Integer(80)),
                new AttributeDescriptor("choice", "sort", null, new String[] {"none", "ascent", "descent"})
            }),
            new WidgetDescriptor("row", null, new AttributeDescriptor[] {
                new AttributeDescriptor("boolean", "selected", null, Boolean.FALSE)
            }),
            new WidgetDescriptor("cell", "choice", null),
            new WidgetDescriptor("tree", "list", new AttributeDescriptor[] {
                new AttributeDescriptor("boolean", "angle", null, Boolean.FALSE),
                new AttributeDescriptor("method", "expand"),
                new AttributeDescriptor("method", "collapse")
            }),
            new WidgetDescriptor("node", "choice", new AttributeDescriptor[] {
                new AttributeDescriptor("boolean", "selected", null, Boolean.FALSE),
                new AttributeDescriptor("boolean", "expanded", null, Boolean.TRUE)
            }),
            new WidgetDescriptor("separator", "component", null),
            new WidgetDescriptor("menubar", "component", null),
            new WidgetDescriptor("menu", "choice", new AttributeDescriptor[] {
                new AttributeDescriptor("integer", "mnemonic", "paint", integer_1)
            }),
            new WidgetDescriptor("menuitem", "choice", new AttributeDescriptor[] {
                new AttributeDescriptor("keystroke", "accelerator", null, null),
                new AttributeDescriptor("method", "action"),
                new AttributeDescriptor("integer", "mnemonic", "paint", integer_1)
            }),
            new WidgetDescriptor("checkboxmenuitem", "menuitem", new AttributeDescriptor[] {
                new AttributeDescriptor("boolean", "selected", "paint", Boolean.FALSE), // ...group
                new AttributeDescriptor("string", "group", "paint", null)
            }), // ...group
            new WidgetDescriptor(
                    "popupmenu",
                    "component",
                    new AttributeDescriptor[] {new AttributeDescriptor("method", "menushown")}), // Post menu: Shift+F10
            new WidgetDescriptor(
                    "bean", "component", new AttributeDescriptor[] {new AttributeDescriptor("bean", "bean", null, null)
                    })
        };
    }
}
