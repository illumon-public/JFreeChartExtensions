package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * Step renderer for XY data that supports AxisTransformations
 */
public class ExtendedXYStepRenderer extends XYStepRenderer {

    /**
     * Stores the index of last visible point for every series
     */
    private int[] lastVisibles;

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset data, PlotRenderingInfo info) {
        final XYItemRendererState state = super.initialise(g2, dataArea, plot, data, info);
        lastVisibles = new int[data.getSeriesCount()];
        for (int i = 0; i < data.getSeriesCount(); i++) {
            lastVisibles[i] = -1;
        }
        return state;
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state,
                         Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                         ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
                         int series, int item, CrosshairState crosshairState, int pass) {

        // do nothing if item is not visible
        if (!getItemVisible(series, item)) {
            return;
        }

        PlotOrientation orientation = plot.getOrientation();

        Paint seriesPaint = getItemPaint(series, item);
        Stroke seriesStroke = getItemStroke(series, item);
        g2.setPaint(seriesPaint);
        g2.setStroke(seriesStroke);

        // get the data point...
        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);

        RectangleEdge xAxisLocation = plot.getDomainAxisEdge();
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();

        boolean currentPointVisible = true;
        final double transX1 = domainAxis.valueToJava2D(x1, dataArea, xAxisLocation);
        if (!Double.isFinite(transX1)) {
            currentPointVisible = false;
        }
        final double transY1 = (Double.isNaN(y1) ? Double.NaN : rangeAxis.valueToJava2D(y1, dataArea, yAxisLocation));
        if (!Double.isFinite(transY1)) {
            currentPointVisible = false;
        }

        if (currentPointVisible) {
            if (pass == 0) {
                if (item > 0) {
                    // get the previous data point...
                    int previouslyVisibleIndex = lastVisibles[series] == -1 ? item : lastVisibles[series];
                    double x0 = dataset.getXValue(series, previouslyVisibleIndex);
                    double y0 = dataset.getYValue(series, previouslyVisibleIndex);
                    double transX0 = domainAxis.valueToJava2D(x0, dataArea, xAxisLocation);
                    double transY0 = (Double.isNaN(y0) ? Double.NaN : rangeAxis.valueToJava2D(y0, dataArea, yAxisLocation));

                    //set current index as last visible index
                    lastVisibles[series] = item;

                    if (orientation == PlotOrientation.HORIZONTAL) {
                        if (transY0 == transY1) {
                            // this represents the situation
                            // for drawing a horizontal bar.
                            drawLine(g2, state.workingLine, transY0, transX0, transY1,
                                    transX1);
                        } else {  //this handles the need to perform a 'step'.

                            // calculate the step point
                            double transXs = transX0 + (getStepPoint()
                                    * (transX1 - transX0));
                            drawLine(g2, state.workingLine, transY0, transX0, transY0,
                                    transXs);
                            drawLine(g2, state.workingLine, transY0, transXs, transY1,
                                    transXs);
                            drawLine(g2, state.workingLine, transY1, transXs, transY1,
                                    transX1);
                        }
                    } else if (orientation == PlotOrientation.VERTICAL) {
                        if (transY0 == transY1) { // this represents the situation
                            // for drawing a horizontal bar.
                            drawLine(g2, state.workingLine, transX0, transY0, transX1,
                                    transY1);
                        } else {  //this handles the need to perform a 'step'.
                            // calculate the step point
                            double transXs = transX0 + (getStepPoint()
                                    * (transX1 - transX0));
                            drawLine(g2, state.workingLine, transX0, transY0, transXs,
                                    transY0);
                            drawLine(g2, state.workingLine, transXs, transY0, transXs,
                                    transY1);
                            drawLine(g2, state.workingLine, transXs, transY1, transX1,
                                    transY1);
                        }
                    }

                    // submit this data item as a candidate for the crosshair point
                    int datasetIndex = plot.indexOf(dataset);
                    updateCrosshairValues(crosshairState, x1, y1, datasetIndex, transX1, transY1, orientation);
                } else {
                    //set current index as last visible index
                    lastVisibles[series] = item;
                }
                // collect entity and tool tip information...
                EntityCollection entities = state.getEntityCollection();
                if (entities != null) {
                    addEntity(entities, null, dataset, series, item, transX1, transY1);
                }
            }

            if (pass == 1) {
                // draw the item label if there is one...
                if (isItemLabelVisible(series, item)) {
                    double xx = transX1;
                    double yy = transY1;
                    if (orientation == PlotOrientation.HORIZONTAL) {
                        xx = transY1;
                        yy = transX1;
                    }
                    drawItemLabel(g2, orientation, dataset, series, item, xx, yy, (y1 < 0.0));
                }
            }
        }
    }

    private void drawLine(Graphics2D g2, Line2D line, double x0, double y0,
                          double x1, double y1) {
        if (Double.isNaN(x0) || Double.isNaN(x1) || Double.isNaN(y0)
                || Double.isNaN(y1)) {
            return;
        }
        line.setLine(x0, y0, x1, y1);
        g2.draw(line);
    }
}
