package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.ShapeUtilities;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Stack;

public abstract class ExtendedXYStackedAreaRenderer extends StackedXYAreaRenderer {

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset data, PlotRenderingInfo info) {
        StackedXYAreaRendererState state = new StackedXYAreaRendererState(info);
        state.setProcessVisibleItemsOnly(false);
        return state;
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
        PlotOrientation orientation = plot.getOrientation();
        StackedXYAreaRendererState areaState
                = (StackedXYAreaRendererState) state;
        // Get the item count for the series, so that we can know which is the
        // end of the series.
        TableXYDataset tdataset = (TableXYDataset) dataset;
        int itemCount = tdataset.getItemCount();

        // get the data point...
        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);
        boolean nullPoint = false;
        if (Double.isNaN(y1)) {
            y1 = 0.0;
            nullPoint = true;
        }

        //  Get height adjustment based on stack and translate to Java2D values
        double ph1 = getPreviousHeight(tdataset, series, item);
        double transX1 = domainAxis.valueToJava2D(x1, dataArea,
                plot.getDomainAxisEdge());
        double transY1 = rangeAxis.valueToJava2D(y1 + ph1, dataArea,
                plot.getRangeAxisEdge());

        //  Get series Paint and Stroke
        Paint seriesPaint = getItemPaint(series, item);
        Paint seriesFillPaint = seriesPaint;
        if (getUseFillPaint()) {
            seriesFillPaint = getItemFillPaint(series, item);
        }
        Stroke seriesStroke = getItemStroke(series, item);

        if (pass == 0) {
            //  On first pass render the areas, line and outlines

            if (item == 0) {
                // Create a new Area for the series
                areaState.setSeriesArea(new Polygon());
                areaState.setLastSeriesPoints(
                        areaState.getCurrentSeriesPoints());
                areaState.setCurrentSeriesPoints(new Stack());

                // start from previous height (ph1)
                double transY2 = rangeAxis.valueToJava2D(ph1, dataArea,
                        plot.getRangeAxisEdge());

                // The first point is (x, 0)
                if (orientation == PlotOrientation.VERTICAL) {
                    areaState.getSeriesArea().addPoint((int) transX1,
                            (int) transY2);
                }
                else if (orientation == PlotOrientation.HORIZONTAL) {
                    areaState.getSeriesArea().addPoint((int) transY2,
                            (int) transX1);
                }
            }

            // Add each point to Area (x, y)
            if (orientation == PlotOrientation.VERTICAL) {
                Point point = new Point((int) transX1, (int) transY1);
                areaState.getSeriesArea().addPoint((int) point.getX(),
                        (int) point.getY());
                areaState.getCurrentSeriesPoints().push(point);
            }
            else if (orientation == PlotOrientation.HORIZONTAL) {
                areaState.getSeriesArea().addPoint((int) transY1,
                        (int) transX1);
            }

            if (getPlotLines(series)) {
                if (item > 0) {
                    // get the previous data point...
                    double x0 = dataset.getXValue(series, item - 1);
                    double y0 = dataset.getYValue(series, item - 1);
                    double ph0 = getPreviousHeight(tdataset, series, item - 1);
                    double transX0 = domainAxis.valueToJava2D(x0, dataArea,
                            plot.getDomainAxisEdge());
                    double transY0 = rangeAxis.valueToJava2D(y0 + ph0,
                            dataArea, plot.getRangeAxisEdge());

                    if (orientation == PlotOrientation.VERTICAL) {
                        areaState.getLine().setLine(transX0, transY0, transX1,
                                transY1);
                    }
                    else if (orientation == PlotOrientation.HORIZONTAL) {
                        areaState.getLine().setLine(transY0, transX0, transY1,
                                transX1);
                    }
                    g2.setPaint(seriesPaint);
                    g2.setStroke(seriesStroke);
                    g2.draw(areaState.getLine());
                }
            }

            // Check if the item is the last item for the series and number of
            // items > 0.  We can't draw an area for a single point.
            if (getPlotArea() && item > 0 && item == (itemCount - 1)) {

                double transY2 = rangeAxis.valueToJava2D(ph1, dataArea,
                        plot.getRangeAxisEdge());

                if (orientation == PlotOrientation.VERTICAL) {
                    // Add the last point (x,0)
                    areaState.getSeriesArea().addPoint((int) transX1,
                            (int) transY2);
                }
                else if (orientation == PlotOrientation.HORIZONTAL) {
                    // Add the last point (x,0)
                    areaState.getSeriesArea().addPoint((int) transY2,
                            (int) transX1);
                }

                // Add points from last series to complete the base of the
                // polygon
                if (series != 0) {
                    Stack points = areaState.getLastSeriesPoints();
                    while (!points.empty()) {
                        Point point = (Point) points.pop();
                        areaState.getSeriesArea().addPoint((int) point.getX(),
                                (int) point.getY());
                    }
                }

                //  Fill the polygon
                g2.setPaint(seriesFillPaint);
                g2.setStroke(seriesStroke);
                g2.fill(areaState.getSeriesArea());

                //  Draw an outline around the Area.
                if (isOutline()) {
                    g2.setStroke(lookupSeriesOutlineStroke(series));
                    g2.setPaint(lookupSeriesOutlinePaint(series));
                    g2.draw(areaState.getSeriesArea());
                }
            }

            int datasetIndex = plot.indexOf(dataset);
            updateCrosshairValues(crosshairState, x1, ph1 + y1, datasetIndex,
                    transX1, transY1, orientation);

        }
        else if (pass == 1) {
            // On second pass render shapes and collect entity and tooltip
            // information

            Shape shape = null;
            if (getPlotShapes(series)) {
                shape = getItemShape(series, item);
                if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                    shape = ShapeUtilities.createTranslatedShape(shape,
                            transX1, transY1);
                }
                else if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    shape = ShapeUtilities.createTranslatedShape(shape,
                            transY1, transX1);
                }
                if (!nullPoint) {
                    if (getShapePaint() != null) {
                        g2.setPaint(getShapePaint());
                    }
                    else {
                        g2.setPaint(seriesPaint);
                    }
                    if (getShapeStroke() != null) {
                        g2.setStroke(getShapeStroke());
                    }
                    else {
                        g2.setStroke(seriesStroke);
                    }
                    g2.draw(shape);
                }
            }
            else {
                if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                    shape = new Rectangle2D.Double(transX1 - 3, transY1 - 3,
                            6.0, 6.0);
                }
                else if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    shape = new Rectangle2D.Double(transY1 - 3, transX1 - 3,
                            6.0, 6.0);
                }
            }

            // collect entity and tool tip information...
            if (state.getInfo() != null) {
                EntityCollection entities = state.getEntityCollection();
                if (entities != null && shape != null && !nullPoint) {
                    // limit the entity hotspot area to the data area
                    Area dataAreaHotspot = new Area(shape);
                    dataAreaHotspot.intersect(new Area(dataArea));
                    if (!dataAreaHotspot.isEmpty()) {
                        String tip = null;
                        XYToolTipGenerator generator = getToolTipGenerator(
                                series, item);
                        if (generator != null) {
                            tip = generator.generateToolTip(dataset, series,
                                    item);
                        }
                        String url = null;
                        if (getURLGenerator() != null) {
                            url = getURLGenerator().generateURL(dataset, series,
                                    item);
                        }
                        XYItemEntity entity = new XYItemEntity(dataAreaHotspot,
                                dataset, series, item, tip, url);
                        entities.add(entity);
                    }
                }
            }

        }

    }

    protected abstract boolean getPlotShapes(final int seriesNum);

    protected abstract boolean getPlotLines(final int seriesNum);

    /**
     * A state object for use by this renderer.
     */
    protected static class StackedXYAreaRendererState extends XYItemRendererState {

        /** The area for the current series. */
        private Polygon seriesArea;

        /** The line. */
        private Line2D line;

        /** The points from the last series. */
        private Stack lastSeriesPoints;

        /** The points for the current series. */
        private Stack currentSeriesPoints;

        /**
         * Creates a new state for the renderer.
         *
         * @param info  the plot rendering info.
         */
        public StackedXYAreaRendererState(PlotRenderingInfo info) {
            super(info);
            this.seriesArea = null;
            this.line = new Line2D.Double();
            this.lastSeriesPoints = new Stack();
            this.currentSeriesPoints = new Stack();
        }

        /**
         * Returns the series area.
         *
         * @return The series area.
         */
        public Polygon getSeriesArea() {
            return this.seriesArea;
        }

        /**
         * Sets the series area.
         *
         * @param area  the area.
         */
        public void setSeriesArea(Polygon area) {
            this.seriesArea = area;
        }

        /**
         * Returns the working line.
         *
         * @return The working line.
         */
        public Line2D getLine() {
            return this.line;
        }

        /**
         * Returns the current series points.
         *
         * @return The current series points.
         */
        public Stack getCurrentSeriesPoints() {
            return this.currentSeriesPoints;
        }

        /**
         * Sets the current series points.
         *
         * @param points  the points.
         */
        public void setCurrentSeriesPoints(Stack points) {
            this.currentSeriesPoints = points;
        }

        /**
         * Returns the last series points.
         *
         * @return The last series points.
         */
        public Stack getLastSeriesPoints() {
            return this.lastSeriesPoints;
        }

        /**
         * Sets the last series points.
         *
         * @param points  the points.
         */
        public void setLastSeriesPoints(Stack points) {
            this.lastSeriesPoints = points;
        }

    }

}
