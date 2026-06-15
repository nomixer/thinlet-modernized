/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic JSON serializer for {@link Trace} plus a minimal hand-rolled
 * reader for the constrained subset we emit. Hand-rolled on purpose: {@code
 * thinlet-core} stays runtime-dependency-free, and the test harness adds no
 * third-party JSON library (DECISIONS.md D4). Output is stable (fixed key order,
 * one element per line) so golden files diff cleanly. Sidecar metadata
 * (timestamps, call sites) is intentionally omitted per D7.
 */
final class TraceJson {

    private TraceJson() {}

    static String write(Trace trace) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"calls\": [");
        for (int i = 0; i < trace.calls.size(); i++) {
            TraceCall c = trace.calls.get(i);
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("    {\"op\": ").append(quote(c.op)).append(", \"cat\": ");
            writeStrings(sb, c.cat);
            sb.append(", \"num\": ");
            writeNums(sb, c.num);
            sb.append("}");
        }
        sb.append(trace.calls.isEmpty() ? "" : "\n  ").append("],\n  \"layout\": [");
        for (int i = 0; i < trace.layout.size(); i++) {
            LayoutNode n = trace.layout.get(i);
            sb.append(i == 0 ? "\n" : ",\n");
            sb.append("    {\"class\": ").append(quote(n.className));
            sb.append(", \"x\": ").append(n.x);
            sb.append(", \"y\": ").append(n.y);
            sb.append(", \"w\": ").append(n.w);
            sb.append(", \"h\": ").append(n.h).append("}");
        }
        sb.append(trace.layout.isEmpty() ? "" : "\n  ").append("]\n}\n");
        return sb.toString();
    }

    private static void writeStrings(StringBuilder sb, List<String> values) {
        sb.append("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(quote(values.get(i)));
        }
        sb.append("]");
    }

    private static void writeNums(StringBuilder sb, List<Double> values) {
        sb.append("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(num(values.get(i)));
        }
        sb.append("]");
    }

    private static String num(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    static Trace read(String json) {
        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) parseValue(json);
        List<TraceCall> calls = new ArrayList<>();
        for (Object o : asList(root.get("calls"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) o;
            List<String> cat = new ArrayList<>();
            for (Object v : asList(m.get("cat"))) {
                cat.add((String) v);
            }
            List<Double> nums = new ArrayList<>();
            for (Object v : asList(m.get("num"))) {
                nums.add(((Number) v).doubleValue());
            }
            calls.add(new TraceCall((String) m.get("op"), cat, nums));
        }
        List<LayoutNode> layout = new ArrayList<>();
        for (Object o : asList(root.get("layout"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) o;
            layout.add(new LayoutNode(
                    (String) m.get("class"), intOf(m, "x"), intOf(m, "y"), intOf(m, "w"), intOf(m, "h")));
        }
        return new Trace(calls, layout);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        return (List<Object>) o;
    }

    private static int intOf(Map<String, Object> m, String key) {
        return (int) ((Number) m.get(key)).doubleValue();
    }

    /** Parses our own JSON subset into nested {@code Map}/{@code List}/{@code String}/{@code Double}. */
    static Object parseValue(String json) {
        return new Parser(json).parse();
    }

    private static final class Parser {

        private final String s;
        private int i;

        Parser(String s) {
            this.s = s;
        }

        Object parse() {
            skip();
            char c = s.charAt(i);
            if (c == '{') {
                return object();
            }
            if (c == '[') {
                return array();
            }
            if (c == '"') {
                return string();
            }
            return number();
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // consume '{'
            skip();
            if (s.charAt(i) == '}') {
                i++;
                return m;
            }
            while (true) {
                skip();
                String key = string();
                skip();
                i++; // consume ':'
                m.put(key, parse());
                skip();
                char c = s.charAt(i++);
                if (c == '}') {
                    break;
                }
            }
            return m;
        }

        private List<Object> array() {
            List<Object> l = new ArrayList<>();
            i++; // consume '['
            skip();
            if (s.charAt(i) == ']') {
                i++;
                return l;
            }
            while (true) {
                l.add(parse());
                skip();
                char c = s.charAt(i++);
                if (c == ']') {
                    break;
                }
            }
            return l;
        }

        private String string() {
            skip();
            i++; // consume opening quote
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') {
                    break;
                }
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n':
                            b.append('\n');
                            break;
                        case 'r':
                            b.append('\r');
                            break;
                        case 't':
                            b.append('\t');
                            break;
                        case 'u':
                            b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                            break;
                        default:
                            b.append(e);
                    }
                } else {
                    b.append(c);
                }
            }
            return b.toString();
        }

        private Double number() {
            int start = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ',' || c == ']' || c == '}' || c == ' ' || c == '\n' || c == '\t' || c == '\r') {
                    break;
                }
                i++;
            }
            return Double.valueOf(s.substring(start, i));
        }

        private void skip() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\n' || c == '\t' || c == '\r') {
                    i++;
                } else {
                    break;
                }
            }
        }
    }
}
