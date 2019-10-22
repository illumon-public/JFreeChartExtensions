package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * StackedAreaRendered for XY plots that supports AxisTransformations
 */
public class ExtendedXYStackedAreaRenderer2 extends StackedXYAreaRenderer2 {

    /**
     * Stores the index of last visible point for every series
     */
    private boolean[][] isVisible;

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset data, PlotRenderingInfo info) {
        final XYItemRendererState state = super.initialise(g2, dataArea, plot, data, info);

        final int seriesCount = data.getSeriesCount();

        isVisible = new boolean[seriesCount][];
        for (int i = 0; i < seriesCount; i++) {
            final int itemCount = data.getItemCount(i);
            isVisible[i] = new boolean[itemCount];
        }
        return state;
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state,
                         Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
                         ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
                         int series, int item, CrosshairState crosshairState, int pass) {

// setup for collecting optional entity info...
        Shape entityArea;
        EntityCollection entities = null;
        if (info != null) {
            entities = info.getOwner().getEntityCollection();
        }

        TableXYDataset tdataset = (TableXYDataset) dataset;
        PlotOrientation orientation = plot.getOrientation();

        RectangleEdge edge0 = plot.getDomainAxisEdge();
        RectangleEdge edge1 = plot.getRangeAxisEdge();

        // get the data point...
        boolean currentPointVisible = true;
        double x1 = dataset.getXValue(series, item);
        double transX1 = domainAxis.valueToJava2D(x1, dataArea, edge0);


        if (!Double.isFinite(transX1)) {
            currentPointVisible = false;
        }
        double y1 = dataset.getYValue(series, item);
        double tY1 = rangeAxis.valueToJava2D(y1, dataArea, edge1);

        if (!Double.isFinite(tY1)) {
            currentPointVisible = false;
        }

        //Render for visible points only
        if (currentPointVisible && series < isVisible.length && item < isVisible[series].length) {
            isVisible[series][item] = true;

            double[] stack1 = getStackValues(tdataset, series, item);

            // get the previous point and the next point so we can calculate a
            // "hot spot" for the area (used by the chart entity)...

            //Get the previously visible point
            int previousVisiblePoint = findPreviousVisiblePoint(series, Math.max(item, 0));
            previousVisiblePoint = previousVisiblePoint == -1 ? item : previousVisiblePoint;
            double x0 = dataset.getXValue(series, previousVisiblePoint);
            double y0 = dataset.getYValue(series, previousVisiblePoint);

            double[] stack0 = getStackValues(tdataset, series, previousVisiblePoint);
            double transX0 = domainAxis.valueToJava2D(x0, dataArea, edge0);

            double xMid = (x0 + x1) / 2.0;
            double[] adjStackMid = adjustedStackValues(stack0, stack1);

            double transXMid = domainAxis.valueToJava2D(xMid, dataArea, edge0);

            if (this.getRoundXCoordinates()) {
                transX1 = Math.round(transX1);
                transXMid = Math.round(transXMid);
            }
            double transY1;

            final GeneralPath left = new GeneralPath();
            final GeneralPath right = new GeneralPath();
            if (y1 >= 0.0) {  // handle positive value
                transY1 = rangeAxis.valueToJava2D(y1 + stack1[1], dataArea,
                        edge1);
                double transStack1 = rangeAxis.valueToJava2D(stack1[1], dataArea, edge1);
                //If stack1[1] is not visible (mostly got LOG transformation), then transform the min of range axis
                transStack1 = Double.isFinite(transStack1) ? transStack1 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                // LEFT POLYGON
                if (y0 >= 0.0) {
                    double transY0 = rangeAxis.valueToJava2D(y0 + stack0[1], dataArea, edge1);

                    double transStack0 = rangeAxis.valueToJava2D(stack0[1], dataArea, edge1);
                    //If stack0[1] is not visible (mostly got LOG transformation), then transform the min of range axis
                    transStack0 = Double.isFinite(transStack0) ? transStack0 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    //If y0 and y1 has different signs (one is positive and the other is negative), then adjStackMid is {0, 0}. If it's non-transformed case, then use valueToJava2D, otherwise avg
                    // of transY0 and transY1 (Same logic for all the other cases)
                    double transStackMid = adjStackMid[1] == 0 ? rangeAxis.valueToJava2D(adjStackMid[1], dataArea, edge1) : (transStack0 + transStack1) / 2;
                    transStackMid = Double.isFinite(transStackMid) ? transStackMid : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transYMid = (transY0 + transY1) / 2;
                    if (orientation == PlotOrientation.VERTICAL) {
                        left.moveTo(transXMid, transYMid);
                        left.lineTo(transXMid, transStackMid);
                        left.lineTo(transX0, transStack0);
                        left.lineTo(transX0, transY0);
                    } else {
                        left.moveTo(transYMid, transXMid);
                        left.lineTo(transStackMid, transXMid);
                        left.lineTo(transStack0, transX0);
                        left.lineTo(transY0, transX0);
                    }
                    left.closePath();
                } else {
                    double transY0 = rangeAxis.valueToJava2D(y0 + stack0[0], dataArea, edge1);
                    double transStack0 = rangeAxis.valueToJava2D(stack0[0], dataArea, edge1);
                    transStack0 = Double.isFinite(transStack0) ? transStack0 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transStackMid = adjStackMid[1] == 0 ? rangeAxis.valueToJava2D(adjStackMid[1], dataArea, edge1) : (transStack0 + transStack1) / 2;
                    transStackMid = Double.isFinite(transStackMid) ? transStackMid : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    if (orientation == PlotOrientation.VERTICAL) {
                        left.moveTo(transX0, transStack0);
                        left.lineTo(transX0, transY0);
                        left.lineTo(transXMid, transStackMid);
                    } else {
                        left.moveTo(transStack0, transX0);
                        left.lineTo(transY0, transX0);
                        left.lineTo(transStackMid, transXMid);
                    }
                    left.closePath();
                }

                // RIGHT POLYGON
                if (y0 >= 0.0) {
                    double transY0 = rangeAxis.valueToJava2D(y0 + stack0[1], dataArea, edge1);
                    double transStack0 = rangeAxis.valueToJava2D(stack0[1], dataArea, edge1);
                    transStack0 = Double.isFinite(transStack0) ? transStack0 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transStackMid = adjStackMid[1] == 0 ? rangeAxis.valueToJava2D(adjStackMid[1], dataArea, edge1) : (transStack0 + transStack1) / 2;
                    transStackMid = Double.isFinite(transStackMid) ? transStackMid : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transYMid = (transY0 + transY1) / 2;
                    if (orientation == PlotOrientation.VERTICAL) {
                        right.moveTo(transX1, transY1);
                        right.lineTo(transX1, transStack1);
                        right.lineTo(transXMid, transStackMid);
                        right.lineTo(transXMid, transYMid);
                    } else {
                        right.moveTo(transY1, transX1);
                        right.lineTo(transStack1, transX1);
                        right.lineTo(transStackMid, transXMid);
                        right.lineTo(transYMid, transXMid);
                    }
                    right.closePath();
                } else {
                    double transStack0 = rangeAxis.valueToJava2D(stack0[0], dataArea, edge1);
                    transStack0 = Double.isFinite(transStack0) ? transStack0 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transStackMid = adjStackMid[1] == 0 ? rangeAxis.valueToJava2D(adjStackMid[1], dataArea, edge1) : (transStack0 + transStack1) / 2;
                    transStackMid = Double.isFinite(transStackMid) ? transStackMid : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    if (orientation == PlotOrientation.VERTICAL) {
                        right.moveTo(transXMid, transStackMid);
                        right.lineTo(transX1, transStack1);
                        right.lineTo(transX1, transY1);
                    } else {
                        right.moveTo(transStackMid, transXMid);
                        right.lineTo(transStack1, transX1);
                        right.lineTo(transY1, transX1);
                    }
                    right.closePath();
                }

            } else {  // handle negative value
                transY1 = rangeAxis.valueToJava2D(y1 + stack1[0], dataArea,
                        edge1);
                double transStack1 = rangeAxis.valueToJava2D(stack1[0],
                        dataArea, edge1);
                transStack1 = Double.isFinite(transStack1) ? transStack1 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                // LEFT POLYGON
                if (y0 >= 0.0) {
                    double transY0 = rangeAxis.valueToJava2D(y0 + stack0[1], dataArea, edge1);
                    double transStack0 = rangeAxis.valueToJava2D(stack0[1], dataArea, edge1);
                    transStack0 = Double.isFinite(transStack0) ? transStack0 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transStackMid = adjStackMid[0] == 0 ? rangeAxis.valueToJava2D(adjStackMid[0], dataArea, edge1) : (transStack0 + transStack1) / 2;
                    transStackMid = Double.isFinite(transStackMid) ? transStackMid : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    if (orientation == PlotOrientation.VERTICAL) {
                        left.moveTo(transX0, transStack0);
                        left.lineTo(transX0, transY0);
                        left.lineTo(transXMid, transStackMid);
                    } else {
                        left.moveTo(transStack0, transX0);
                        left.lineTo(transY0, transX0);
                        left.lineTo(transStackMid, transXMid);
                    }
                    left.closePath();
                } else {
                    double transY0 = rangeAxis.valueToJava2D(y0 + stack0[0], dataArea, edge1);
                    double transStack0 = rangeAxis.valueToJava2D(stack0[0], dataArea, edge1);
                    transStack0 = Double.isFinite(transStack0) ? transStack0 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transStackMid = adjStackMid[0] == 0 ? rangeAxis.valueToJava2D(adjStackMid[0], dataArea, edge1) : (transStack0 + transStack1) / 2;
                    transStackMid = Double.isFinite(transStackMid) ? transStackMid : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transYMid = (transY0 + transY1) / 2;

                    if (orientation == PlotOrientation.VERTICAL) {
                        left.moveTo(transXMid, transYMid);
                        left.lineTo(transXMid, transStackMid);
                        left.lineTo(transX0, transStack0);
                        left.lineTo(transX0, transY0);
                    } else {
                        left.moveTo(transYMid, transXMid);
                        left.lineTo(transStackMid, transXMid);
                        left.lineTo(transStack0, transX0);
                        left.lineTo(transY0, transX0);
                    }
                    left.closePath();
                }

                // RIGHT POLYGON
                if (y0 >= 0.0) {
                    double transStack0 = rangeAxis.valueToJava2D(stack0[1], dataArea, edge1);
                    transStack0 = Double.isFinite(transStack0) ? transStack0 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transStackMid = adjStackMid[0] == 0 ? rangeAxis.valueToJava2D(adjStackMid[0], dataArea, edge1) : (transStack0 + transStack1) / 2;
                    transStackMid = Double.isFinite(transStackMid) ? transStackMid : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    if (orientation == PlotOrientation.VERTICAL) {
                        right.moveTo(transXMid, transStackMid);
                        right.lineTo(transX1, transStack1);
                        right.lineTo(transX1, transY1);
                    } else {
                        right.moveTo(transStackMid, transXMid);
                        right.lineTo(transStack1, transX1);
                        right.lineTo(transY1, transX1);
                    }
                    right.closePath();
                } else {
                    double transY0 = rangeAxis.valueToJava2D(y0 + stack0[0], dataArea, edge1);
                    double transStack0 = rangeAxis.valueToJava2D(stack0[1], dataArea, edge1);
                    transStack0 = Double.isFinite(transStack0) ? transStack0 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transStackMid = adjStackMid[0] == 0 ? rangeAxis.valueToJava2D(adjStackMid[0], dataArea, edge1) : (transStack0 + transStack1) / 2;
                    transStackMid = Double.isFinite(transStackMid) ? transStackMid : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

                    double transYMid = (transY0 + transY1) / 2;
                    if (orientation == PlotOrientation.VERTICAL) {
                        right.moveTo(transX1, transY1);
                        right.lineTo(transX1, transStack1);
                        right.lineTo(transXMid, transStackMid);
                        right.lineTo(transXMid, transYMid);
                    } else {
                        right.moveTo(transY1, transX1);
                        right.lineTo(transStack1, transX1);
                        right.lineTo(transStackMid, transXMid);
                        right.lineTo(transYMid, transXMid);
                    }
                    right.closePath();
                }
            }

            //  Get series Paint and Stroke
            Paint previousItemPaint = getItemPaint(series, previousVisiblePoint);
            Paint itemPaint = getItemPaint(series, item);
            if (pass == 0) {
                g2.setPaint(previousItemPaint);
                g2.fill(left);
                g2.setPaint(itemPaint);
                g2.fill(right);
            }

            // add an entity for the item...
            if (entities != null) {
                GeneralPath gp = new GeneralPath(left);
                entityArea = gp;
                addEntity(entities, entityArea, dataset, series, previousVisiblePoint, transX1, transY1);

                gp = new GeneralPath(right);
                entityArea = gp;
                addEntity(entities, entityArea, dataset, series, item, transX1, transY1);
            }
        }
    }

    private double[] getStackValues(TableXYDataset dataset,
                                    int series, int index) {
        double[] result = new double[2];
        for (int i = 0; i < series; i++) {
            if (isVisible[series][index] && isSeriesVisible(i)) {
                double v = dataset.getYValue(i, index);
                if (!Double.isNaN(v)) {
                    if (v >= 0.0) {
                        result[1] += v;
                    } else {
                        result[0] += v;
                    }
                }
            }
        }
        return result;
    }

    private double[] adjustedStackValues(double[] stack1, double[] stack2) {
        double[] result = new double[2];
        if (stack1[0] == 0.0 || stack2[0] == 0.0) {
            result[0] = 0.0;
        } else {
            result[0] = (stack1[0] + stack2[0]) / 2.0;
        }
        if (stack1[1] == 0.0 || stack2[1] == 0.0) {
            result[1] = 0.0;
        } else {
            result[1] = (stack1[1] + stack2[1]) / 2.0;
        }
        return result;
    }

    private int findPreviousVisiblePoint(final int series, final int index) {
        if(isSeriesVisible(series)) {
            final boolean[] visibles = isVisible[series];
            for (int i = index - 1; i >= 0; i--) {
                if (visibles[i]) {
                    return i;
                }
            }
        }
        return -1;
    }
}
