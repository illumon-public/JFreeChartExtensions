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

    /**
     * Stores the index of last visible point for every series
     */
    private int[] lastVisibleIndex;

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset data, PlotRenderingInfo info) {
        XYAreaRendererState state = new XYAreaRendererState(info);
        state.setProcessVisibleItemsOnly(false);

        final int seriesCount = data.getSeriesCount();
        lastVisibleIndex = new int[seriesCount];

        //fill every item with -1, which indicates that it hasn't been written yet
        for (int i = 0; i < seriesCount; i++) {
            lastVisibleIndex[i] = -1;
        }
        return state;
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
        if (this.getItemVisible(series, item)) {

            //XYAreaRenderer.XYAreaRendererState areaState = (XYAreaRenderer.XYAreaRendererState)state;
            XYAreaRendererState areaState = (XYAreaRendererState) state;
            double x1 = dataset.getXValue(series, item);
            double y1 = dataset.getYValue(series, item);
            if (Double.isNaN(y1)) {
                y1 = 0.0D;
            }

            boolean currentPointVisible = true;
            double transX1 = domainAxis.valueToJava2D(x1, dataArea, plot.getDomainAxisEdge());
            double transY1 = rangeAxis.valueToJava2D(y1, dataArea, plot.getRangeAxisEdge());

            if (!Double.isFinite(transX1) || !Double.isFinite(transY1)) {
                currentPointVisible = false;
            } else {
                if(item == 0 || lastVisibleIndex[series] == -1){
                    lastVisibleIndex[series] = item;
                }
            }

            int itemCount = dataset.getItemCount(series);

            int previousVisibleIndex = Math.max(item - 1, 0);
            double x0 = dataset.getXValue(series, previousVisibleIndex);
            double y0 = dataset.getYValue(series, previousVisibleIndex);
            if (Double.isNaN(y0)) {
                y0 = 0.0D;
            }

            double transX0 = domainAxis.valueToJava2D(x0, dataArea, plot.getDomainAxisEdge());
            double transY0 = rangeAxis.valueToJava2D(y0, dataArea, plot.getRangeAxisEdge());

            if (!Double.isFinite(transX0) || !Double.isFinite(transY0)) {
                //find previous visible point
                previousVisibleIndex = lastVisibleIndex[series];
                transX0 = domainAxis.valueToJava2D(dataset.getXValue(series, previousVisibleIndex), dataArea, plot.getDomainAxisEdge());
                transY0 = rangeAxis.valueToJava2D(dataset.getYValue(series, previousVisibleIndex), dataArea, plot.getRangeAxisEdge());
            }

            if (currentPointVisible && series < lastVisibleIndex.length) {
                //update the previous visible point for series
                lastVisibleIndex[series] = item;
            }

            int nextVisibleIndex = Math.min(item + 1, itemCount - 1);
            double x2 = dataset.getXValue(series, nextVisibleIndex);
            double y2 = dataset.getYValue(series, nextVisibleIndex);
            if (Double.isNaN(y2)) {
                y2 = 0.0D;
            }

            double transX2 = domainAxis.valueToJava2D(x2, dataArea, plot.getDomainAxisEdge());
            double transY2 = rangeAxis.valueToJava2D(y2, dataArea, plot.getRangeAxisEdge());

            double transZero = rangeAxis.valueToJava2D(0.0D, dataArea, plot.getRangeAxisEdge());
            //If transformation is not finite at 0, then take minimum value of range (For LOG transformation, min of rangeAxis will be >0)
            transZero = !Double.isFinite(transZero) ? rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, plot.getRangeAxisEdge()) : transZero;
            GeneralPath hotspot = null;
            if (currentPointVisible) {
                hotspot = new GeneralPath();
                if (Double.isFinite(transX0) && Double.isFinite(transY0) && Double.isFinite(transX2) && Double.isFinite(transY2)) {
                    //all three points are visible, hence plot area for all three points (0, 1, 2)
                    if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                        moveTo(hotspot, transZero, (transX0 + transX1) / 2.0D);
                        lineTo(hotspot, (transY0 + transY1) / 2.0D, (transX0 + transX1) / 2.0D);
                        lineTo(hotspot, transY1, transX1);
                        lineTo(hotspot, (transY1 + transY2) / 2.0D, (transX1 + transX2) / 2.0D);
                        lineTo(hotspot, transZero, (transX1 + transX2) / 2.0D);
                    } else {
                        moveTo(hotspot, (transX0 + transX1) / 2.0D, transZero);
                        lineTo(hotspot, (transX0 + transX1) / 2.0D, (transY0 + transY1) / 2.0D);
                        lineTo(hotspot, transX1, transY1);
                        lineTo(hotspot, (transX1 + transX2) / 2.0D, (transY1 + transY2) / 2.0D);
                        lineTo(hotspot, (transX1 + transX2) / 2.0D, transZero);
                    }
                } else if (Double.isFinite(transX0) && Double.isFinite(transY0) && (!Double.isFinite(transX2) || !Double.isFinite(transY2))) {
                    //points 0 and 1 are visible, hence plot area for (0,1)
                    if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                        moveTo(hotspot, transZero, (transX0 + transX1) / 2.0D);
                        lineTo(hotspot, (transY0 + transY1) / 2.0D, (transX0 + transX1) / 2.0D);
                        lineTo(hotspot, transY1, transX1);
                        lineTo(hotspot, transZero, transX1);
                    } else {
                        moveTo(hotspot, (transX0 + transX1) / 2.0D, transZero);
                        lineTo(hotspot, (transX0 + transX1) / 2.0D, (transY0 + transY1) / 2.0D);
                        lineTo(hotspot, transX1, transY1);
                        lineTo(hotspot, transX1, transZero);
                    }
                } else if ((!Double.isFinite(transX0) || !Double.isFinite(transY0)) && Double.isFinite(transX2) && Double.isFinite(transY2)) {
                    //points 1 and 2 are visible, hence plot area for (1, 2)
                    if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                        moveTo(hotspot, transZero, transX1);
                        lineTo(hotspot, (transY1 + transY2) / 2.0D, transX1);
                        lineTo(hotspot, (transY1 + transY2) / 2.0D, (transX1 + transX2) / 2.0D);
                        lineTo(hotspot, transZero, (transX1 + transX2) / 2.0D);
                    } else {
                        moveTo(hotspot, transX1, transZero);
                        lineTo(hotspot, transX1, (transY1 + transY2) / 2.0D);
                        lineTo(hotspot, (transX1 + transX2) / 2.0D, (transY1 + transY2) / 2.0D);
                        lineTo(hotspot, (transX1 + transX2) / 2.0D, transZero);
                    }
                } else if ((!Double.isFinite(transX0) || !Double.isFinite(transY0)) && (!Double.isFinite(transX2) || !Double.isFinite(transY2))) {
                    //Just point 1 is visible, so draw a line instead of area
                    if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                        moveTo(hotspot, transZero, transX1);
                        lineTo(hotspot, transY1, transX1);
                    } else {
                        moveTo(hotspot, transX1, transZero);
                        lineTo(hotspot, transX1, transY1);
                    }
                }

                hotspot.closePath();
            }
            if (item == 0 || areaState.area.getCurrentPoint() == null) {
                areaState.area = new GeneralPath();
                double orientation = rangeAxis.valueToJava2D(0.0D, dataArea, plot.getRangeAxisEdge());
                orientation = Double.isFinite(orientation) ? orientation : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, plot.getRangeAxisEdge());

                if (currentPointVisible) {
                    if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                        moveTo(areaState.area, transX1, orientation);
                    } else if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                        moveTo(areaState.area, orientation, transX1);
                    }
                }
            }

            if (currentPointVisible) {
                if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                    lineTo(areaState.area, transX1, transY1);
                } else if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    lineTo(areaState.area, transY1, transX1);
                }
            }

            PlotOrientation orientation1 = plot.getOrientation();
            Paint paint = this.getItemPaint(series, item);
            Stroke stroke = this.getItemStroke(series, item);
            g2.setPaint(paint);
            g2.setStroke(stroke);
            //if(this.getPlotShapes()) {
            if (this.getPlotShapes(series) && currentPointVisible) {
                Shape shape = this.getItemShape(series, item);
                if (orientation1 == PlotOrientation.VERTICAL) {
                    shape = ShapeUtilities.createTranslatedShape(shape, transX1, transY1);
                } else if (orientation1 == PlotOrientation.HORIZONTAL) {
                    shape = ShapeUtilities.createTranslatedShape(shape, transY1, transX1);
                }
                //adding point to the collection to show tooltip when hover
                final EntityCollection entityCollection = state.getEntityCollection();
                if (entityCollection != null) {
                    this.addEntity(entityCollection, shape, dataset, series, item, 0.0D, 0.0D);
                }
                g2.draw(shape);
            }

            //if(this.getPlotLines() && item > 0) {
            if (this.getPlotLines(series) && item > 0 && currentPointVisible) {
                if (plot.getOrientation() == PlotOrientation.VERTICAL) {
                    areaState.line.setLine(transX0, transY0, transX1, transY1);
                } else if (plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    areaState.line.setLine(transY0, transX0, transY1, transX1);
                }

                g2.draw(areaState.line);
            }

            if (this.getPlotArea() && item > 0 && item == itemCount - 1) {
                if (currentPointVisible) {
                    if (orientation1 == PlotOrientation.VERTICAL) {
                        lineTo(areaState.area, transX1, transZero);
                        areaState.area.closePath();
                    } else if (orientation1 == PlotOrientation.HORIZONTAL) {
                        lineTo(areaState.area, transZero, transX1);
                        areaState.area.closePath();
                    }
                } else {
                    //If currentPoint is not visible, then close path after the last visible point
                    if (orientation1 == PlotOrientation.VERTICAL) {
                        lineTo(areaState.area, transX0, transZero);
                        areaState.area.closePath();
                    } else if (orientation1 == PlotOrientation.HORIZONTAL) {
                        lineTo(areaState.area, transZero, transX0);
                        areaState.area.closePath();
                    }
                }


                if (this.getUseFillPaint()) {
                    paint = this.lookupSeriesFillPaint(series);
                }

                if (paint instanceof GradientPaint) {
                    GradientPaint domainAxisIndex = (GradientPaint) paint;
                    GradientPaint rangeAxisIndex = this.getGradientTransformer().transform(domainAxisIndex, dataArea);
                    g2.setPaint(rangeAxisIndex);
                }

                g2.fill(areaState.area);
                if (this.isOutline()) {
                    Object domainAxisIndex1 = areaState.area;
                    Stroke rangeAxisIndex1 = this.lookupSeriesOutlineStroke(series);
                    if (rangeAxisIndex1 instanceof BasicStroke) {
                        BasicStroke entities = (BasicStroke) rangeAxisIndex1;
                        if (entities.getDashArray() != null) {
                            Area poly = new Area(areaState.area);
                            Area clip = new Area(new Rectangle2D.Double(dataArea.getX() - 5.0D, dataArea.getY() - 5.0D, dataArea.getWidth() + 10.0D, dataArea.getHeight() + 10.0D));
                            poly.intersect(clip);
                            domainAxisIndex1 = poly;
                        }
                    }

                    g2.setStroke(rangeAxisIndex1);
                    g2.setPaint(this.lookupSeriesOutlinePaint(series));
                    g2.draw((Shape) domainAxisIndex1);
                }
            }

            PlotOrientation orientation = plot.getOrientation();
            int datasetIndex = plot.indexOf(dataset);
            updateCrosshairValues(crosshairState, x1, y1, datasetIndex,
                    transX1, transY1, orientation);

            EntityCollection entities1 = state.getEntityCollection();
            if (entities1 != null) {
                this.addEntity(entities1, hotspot, dataset, series, item, 0.0D, 0.0D);
            }

        }
    }

    protected abstract boolean getPlotShapes(final int seriesNum);

    protected abstract boolean getPlotLines(final int seriesNum);

    static class XYAreaRendererState extends XYItemRendererState {

        /**
         * Working storage for the area under one series.
         */
        public GeneralPath area;

        /**
         * Working line that can be recycled.
         */
        public Line2D line;

        /**
         * Creates a new state.
         *
         * @param info the plot rendering info.
         */
        public XYAreaRendererState(PlotRenderingInfo info) {
            super(info);
            this.area = new GeneralPath();
            this.line = new Line2D.Double();
        }
    }
}
