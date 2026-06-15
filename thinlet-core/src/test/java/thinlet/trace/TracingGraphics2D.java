/* Thinlet (modernized) — Phase 1 golden-trace harness (test scope). */
package thinlet.trace;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A {@link Graphics2D} that delegates everything to a backing graphics while
 * recording the drawing primitives Thinlet uses (see
 * {@code .claude/paint-pipeline-map.md}). Wrapping the {@code Graphics} handed to
 * Thinlet's public {@code paint(Graphics)} captures the whole draw stream with
 * zero edits to {@code Thinlet.java}.
 *
 * <p>Per DECISIONS.md D7, {@code getFontMetrics} delegates but is NOT recorded —
 * its JDK-to-JDK variance is absorbed by the ±2 px tolerance on the recorded
 * coordinates instead of being asserted directly.
 */
public final class TracingGraphics2D extends Graphics2D {

    private final Graphics2D d;
    private final List<TraceCall> sink;

    public TracingGraphics2D(Graphics2D delegate, List<TraceCall> sink) {
        this.d = delegate;
        this.sink = sink;
    }

    // ---- recording helpers ----

    private void rec(String op, List<String> cat, double... nums) {
        List<Double> values = new ArrayList<>(nums.length);
        for (double v : nums) {
            values.add(v);
        }
        sink.add(new TraceCall(op, cat, values));
    }

    private void recPoly(String op, int[] xs, int[] ys, int nPoints) {
        List<Double> values = new ArrayList<>(1 + 2 * nPoints);
        values.add((double) nPoints);
        for (int k = 0; k < nPoints; k++) {
            values.add((double) xs[k]);
            values.add((double) ys[k]);
        }
        sink.add(new TraceCall(op, none(), values));
    }

    private void recShape(String op, Shape s) {
        Rectangle b = s.getBounds();
        rec(op, cat(s.getClass().getSimpleName()), b.x, b.y, b.width, b.height);
    }

    private void recImage(int x, int y, int w, int h) {
        rec("drawImage", none(), x, y, w, h);
    }

    private static List<String> none() {
        return Collections.emptyList();
    }

    private static List<String> cat(String... s) {
        return Arrays.asList(s);
    }

    private static String hex(Color c) {
        return String.format("#%02X%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    private static String fontStr(Font f) {
        return f.getFamily() + "|" + f.getStyle() + "|" + f.getSize();
    }

    // ---- java.awt.Graphics ----

    @Override
    public Graphics create() {
        return new TracingGraphics2D((Graphics2D) d.create(), sink);
    }

    @Override
    public Graphics create(int x, int y, int width, int height) {
        return new TracingGraphics2D((Graphics2D) d.create(x, y, width, height), sink);
    }

    @Override
    public void translate(int x, int y) {
        rec("translate", none(), x, y);
        d.translate(x, y);
    }

    @Override
    public Color getColor() {
        return d.getColor();
    }

    @Override
    public void setColor(Color c) {
        rec("setColor", cat(c == null ? "null" : hex(c)));
        d.setColor(c);
    }

    @Override
    public void setPaintMode() {
        d.setPaintMode();
    }

    @Override
    public void setXORMode(Color c1) {
        d.setXORMode(c1);
    }

    @Override
    public Font getFont() {
        return d.getFont();
    }

    @Override
    public void setFont(Font font) {
        rec("setFont", cat(fontStr(font)));
        d.setFont(font);
    }

    @Override
    public FontMetrics getFontMetrics(Font f) {
        return d.getFontMetrics(f);
    }

    @Override
    public Rectangle getClipBounds() {
        return d.getClipBounds();
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        rec("clipRect", none(), x, y, width, height);
        d.clipRect(x, y, width, height);
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        rec("setClip", none(), x, y, width, height);
        d.setClip(x, y, width, height);
    }

    @Override
    public Shape getClip() {
        return d.getClip();
    }

    @Override
    public void setClip(Shape clip) {
        if (clip == null) {
            rec("setClip", cat("null"));
        } else {
            Rectangle b = clip.getBounds();
            rec("setClip", cat(clip.getClass().getSimpleName()), b.x, b.y, b.width, b.height);
        }
        d.setClip(clip);
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        d.copyArea(x, y, width, height, dx, dy);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        rec("drawLine", none(), x1, y1, x2, y2);
        d.drawLine(x1, y1, x2, y2);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        rec("fillRect", none(), x, y, width, height);
        d.fillRect(x, y, width, height);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        rec("drawRect", none(), x, y, width, height);
        d.drawRect(x, y, width, height);
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        rec("clearRect", none(), x, y, width, height);
        d.clearRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        rec("drawRoundRect", none(), x, y, width, height, arcWidth, arcHeight);
        d.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        rec("fillRoundRect", none(), x, y, width, height, arcWidth, arcHeight);
        d.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        rec("drawOval", none(), x, y, width, height);
        d.drawOval(x, y, width, height);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        rec("fillOval", none(), x, y, width, height);
        d.fillOval(x, y, width, height);
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        rec("drawArc", none(), x, y, width, height, startAngle, arcAngle);
        d.drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        rec("fillArc", none(), x, y, width, height, startAngle, arcAngle);
        d.fillArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        recPoly("drawPolyline", xPoints, yPoints, nPoints);
        d.drawPolyline(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        recPoly("drawPolygon", xPoints, yPoints, nPoints);
        d.drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        recPoly("fillPolygon", xPoints, yPoints, nPoints);
        d.fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawString(String str, int x, int y) {
        rec("drawString", cat(str), x, y);
        d.drawString(str, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        d.drawString(iterator, x, y);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        recImage(x, y, img == null ? -1 : img.getWidth(observer), img == null ? -1 : img.getHeight(observer));
        return d.drawImage(img, x, y, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        recImage(x, y, width, height);
        return d.drawImage(img, x, y, width, height, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
        recImage(x, y, img == null ? -1 : img.getWidth(observer), img == null ? -1 : img.getHeight(observer));
        return d.drawImage(img, x, y, bgcolor, observer);
    }

    @Override
    public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
        recImage(x, y, width, height);
        return d.drawImage(img, x, y, width, height, bgcolor, observer);
    }

    @Override
    public boolean drawImage(
            Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
        recImage(dx1, dy1, dx2 - dx1, dy2 - dy1);
        return d.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    @Override
    public boolean drawImage(
            Image img,
            int dx1,
            int dy1,
            int dx2,
            int dy2,
            int sx1,
            int sy1,
            int sx2,
            int sy2,
            Color bgcolor,
            ImageObserver observer) {
        recImage(dx1, dy1, dx2 - dx1, dy2 - dy1);
        return d.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
    }

    @Override
    public void dispose() {
        d.dispose();
    }

    // ---- java.awt.Graphics2D ----

    @Override
    public void draw(Shape s) {
        recShape("draw", s);
        d.draw(s);
    }

    @Override
    public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return d.drawImage(img, xform, obs);
    }

    @Override
    public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        d.drawImage(img, op, x, y);
    }

    @Override
    public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        d.drawRenderedImage(img, xform);
    }

    @Override
    public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        d.drawRenderableImage(img, xform);
    }

    @Override
    public void drawString(String str, float x, float y) {
        rec("drawString", cat(str), x, y);
        d.drawString(str, x, y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        d.drawString(iterator, x, y);
    }

    @Override
    public void drawGlyphVector(GlyphVector g, float x, float y) {
        d.drawGlyphVector(g, x, y);
    }

    @Override
    public void fill(Shape s) {
        recShape("fill", s);
        d.fill(s);
    }

    @Override
    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
        return d.hit(rect, s, onStroke);
    }

    @Override
    public GraphicsConfiguration getDeviceConfiguration() {
        return d.getDeviceConfiguration();
    }

    @Override
    public void setComposite(Composite comp) {
        d.setComposite(comp);
    }

    @Override
    public void setPaint(Paint paint) {
        d.setPaint(paint);
    }

    @Override
    public void setStroke(Stroke s) {
        d.setStroke(s);
    }

    @Override
    public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        d.setRenderingHint(hintKey, hintValue);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key hintKey) {
        return d.getRenderingHint(hintKey);
    }

    @Override
    public void setRenderingHints(Map<?, ?> hints) {
        d.setRenderingHints(hints);
    }

    @Override
    public void addRenderingHints(Map<?, ?> hints) {
        d.addRenderingHints(hints);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return d.getRenderingHints();
    }

    @Override
    public void translate(double tx, double ty) {
        d.translate(tx, ty);
    }

    @Override
    public void rotate(double theta) {
        d.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        d.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        d.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy) {
        d.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform tx) {
        d.transform(tx);
    }

    @Override
    public void setTransform(AffineTransform tx) {
        d.setTransform(tx);
    }

    @Override
    public AffineTransform getTransform() {
        return d.getTransform();
    }

    @Override
    public Paint getPaint() {
        return d.getPaint();
    }

    @Override
    public Composite getComposite() {
        return d.getComposite();
    }

    @Override
    public void setBackground(Color color) {
        d.setBackground(color);
    }

    @Override
    public Color getBackground() {
        return d.getBackground();
    }

    @Override
    public Stroke getStroke() {
        return d.getStroke();
    }

    @Override
    public void clip(Shape s) {
        d.clip(s);
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return d.getFontRenderContext();
    }
}
