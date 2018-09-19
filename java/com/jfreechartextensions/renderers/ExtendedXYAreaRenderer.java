package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.ShapeUtilities;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public abstract class ExtendedXYAreaRenderer extends XYAreaRenderer {

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset data, PlotRenderingInfo info) {
        XYAreaRendererState state = new XYAreaRendererState(info, data);
        state.setProcessVisibleItemsOnly(false);
        return state;
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
        if (!getItemVisible(series, item)) {
            return;
        }
        XYAreaRendererState areaState = (XYAreaRendererState) state;

        // get the data point...
        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);
        if (Double.isNaN(y1)) {
            y1 = 0.0;
        }
        double transX1 = domainAxis.valueToJava2D(x1, dataArea,
                plot.getDomainAxisEdge());
        double transY1 = rangeAxis.valueToJava2D(y1, dataArea,
                plot.getRangeAxisEdge());

        // get the previous point and the next point so we can calculate a
        // "hot spot" for the area (used by the chart entity)...
        int itemCount = dataset.getItemCount(series);
        double x0 = dataset.getXValue(series, Math.max(item - 1, 0));
        double y0 = dataset.getYValue(series, Math.max(item - 1, 0));
        if (Double.isNaN(y0)) {
            y0 = 0.0;
        }
        double transX0 = domainAxis.valueToJava2D(x0, dataArea,
                plot.getDomainAxisEdge());
        double transY0 = rangeAxis.valueToJava2D(y0, dataArea,
                plot.getRangeAxisEdge());

        double x2 = dataset.getXValue(series, Math.min(item + 1,
                itemCount - 1));
        double y2 = dataset.getYValue(series, Math.min(item + 1,
                itemCount - 1));
        if (Double.isNaN(y2)) {
            y2 = 0.0;
        }
        double transX2 = domainAxis.valueToJava2D(x2, dataArea,
                plot.getDomainAxisEdge());
        double transY2 = rangeAxis.valueToJava2D(y2, dataArea,
                plot.getRangeAxisEdge());

        double transZero = rangeAxis.valueToJava2D(0.0, dataArea,
                plot.getRangeAxisEdge());

        if (item == 0) {  // create a new area polygon for the series
            areaState.area = new GeneralPath();
            // the first point is (x, 0)
            double zero = rangeAxis.valueToJava2D(0.0, dataArea,
                    plot.getRangeAxisEdge());
            if (plot.getOrientation().isVertical()) {
                moveTo(areaState.area, transX1, zero);
            } else if (plot.getOrientation().isHorizontal()) {
                moveTo(areaState.area, zero, transX1);
            }
        }

        // Add each point to Area (x, y)
        if (plot.getOrientation().isVertical()) {
            lineTo(areaState.area, transX1, transY1);
        } else if (plot.getOrientation().isHorizontal()) {
            lineTo(areaState.area, transY1, transX1);
        }

        PlotOrientation orientation = plot.getOrientation();
        Paint paint = getItemPaint(series, item);
        Stroke stroke = getItemStroke(series, item);
        g2.setPaint(paint);
        g2.setStroke(stroke);

        Shape shape;
        if (getPlotShapes(series)) {
            shape = getItemShape(series, item);
            if (orientation == PlotOrientation.VERTICAL) {
                shape = ShapeUtilities.createTranslatedShape(shape, transX1,
                        transY1);
            } else if (orientation == PlotOrientation.HORIZONTAL) {
                shape = ShapeUtilities.createTranslatedShape(shape, transY1,
                        transX1);
            }
            g2.draw(shape);
        }

        if (getPlotLines(series)) {
            if (item > 0) {
                if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                    areaState.line.setLine(transX0, transY0, transX1, transY1);
                } else if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    areaState.line.setLine(transY0, transX0, transY1, transX1);
                }
                g2.draw(areaState.line);
            }
        }

        // Check if the item is the last item for the series.
        // and number of items > 0.  We can't draw an area for a single point.
        if (getPlotArea() && item > 0 && item == (itemCount - 1)) {

            if (orientation == PlotOrientation.VERTICAL) {
                // Add the last point (x,0)
                lineTo(areaState.area, transX1, transZero);
                areaState.area.closePath();
            } else if (orientation == PlotOrientation.HORIZONTAL) {
                // Add the last point (x,0)
                lineTo(areaState.area, transZero, transX1);
                areaState.area.closePath();
            }

            if (this.getUseFillPaint()) {
                paint = lookupSeriesFillPaint(series);
            }
            if (paint instanceof GradientPaint) {
                GradientPaint gp = (GradientPaint) paint;
                GradientPaint adjGP = this.getGradientTransformer().transform(gp,
                        dataArea);
                g2.setPaint(adjGP);
            }
            g2.fill(areaState.area);

            // draw an outline around the Area.
            if (isOutline()) {
                Shape area = areaState.area;

                // Java2D has some issues drawing dashed lines around "large"
                // geometrical shapes - for example, see bug 6620013 in the
                // Java bug database.  So, we'll check if the outline is
                // dashed and, if it is, do our own clipping before drawing
                // the outline...
                Stroke outlineStroke = lookupSeriesOutlineStroke(series);
                if (outlineStroke instanceof BasicStroke) {
                    BasicStroke bs = (BasicStroke) outlineStroke;
                    if (bs.getDashArray() != null) {
                        Area poly = new Area(areaState.area);
                        // we make the clip region slightly larger than the
                        // dataArea so that the clipped edges don't show lines
                        // on the chart
                        Area clip = new Area(new Rectangle2D.Double(
                                dataArea.getX() - 5.0, dataArea.getY() - 5.0,
                                dataArea.getWidth() + 10.0,
                                dataArea.getHeight() + 10.0));
                        poly.intersect(clip);
                        area = poly;
                    }
                } // end of workaround

                g2.setStroke(outlineStroke);
                g2.setPaint(lookupSeriesOutlinePaint(series));
                g2.draw(area);
            }
        }

        int datasetIndex = plot.indexOf(dataset);
        updateCrosshairValues(crosshairState, x1, y1, datasetIndex,
                transX1, transY1, orientation);

        // collect entity and tool tip information...
        EntityCollection entities = state.getEntityCollection();
        if (entities != null) {
            GeneralPath hotspot = new GeneralPath();
            if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                moveTo(hotspot, transZero, ((transX0 + transX1) / 2.0));
                lineTo(hotspot, ((transY0 + transY1) / 2.0), ((transX0 + transX1) / 2.0));
                lineTo(hotspot, transY1, transX1);
                lineTo(hotspot, ((transY1 + transY2) / 2.0), ((transX1 + transX2) / 2.0));
                lineTo(hotspot, transZero, ((transX1 + transX2) / 2.0));
            } else { // vertical orientation
                moveTo(hotspot, ((transX0 + transX1) / 2.0), transZero);
                lineTo(hotspot, ((transX0 + transX1) / 2.0), ((transY0 + transY1) / 2.0));
                lineTo(hotspot, transX1, transY1);
                lineTo(hotspot, ((transX1 + transX2) / 2.0), ((transY1 + transY2) / 2.0));
                lineTo(hotspot, ((transX1 + transX2) / 2.0), transZero);
            }
            hotspot.closePath();

            // limit the entity hotspot area to the data area
            Area dataAreaHotspot = new Area(hotspot);
            dataAreaHotspot.intersect(new Area(dataArea));

            if (dataAreaHotspot.isEmpty() == false) {
                addEntity(entities, dataAreaHotspot, dataset, series, item,
                        0.0, 0.0);
            }
        }

    }

    protected abstract boolean getPlotShapes(final int seriesNum);

    protected abstract boolean getPlotLines(final int seriesNum);

    static class XYAreaRendererState extends XYItemRendererState {

        /** Working storage for the area under one series. */
        public GeneralPath area;

        /** Working line that can be recycled. */
        public Line2D line;

        /**
         * Creates a new state.
         *
         * @param info  the plot rendering info.
         */
        public XYAreaRendererState(PlotRenderingInfo info, XYDataset dataset) {
            super(info, dataset);
            this.area = new GeneralPath();
            this.line = new Line2D.Double();
        }

    }
}
