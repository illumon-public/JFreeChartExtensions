package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public abstract class ExtendedXYBarRenderer extends XYBarRenderer {

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
        if (this.getItemVisible(series, item)) {
            IntervalXYDataset intervalDataset = (IntervalXYDataset) dataset;
            double value0;
            double value1;
            if (this.getUseYInterval()) {
                value0 = intervalDataset.getStartYValue(series, item);
                value1 = intervalDataset.getEndYValue(series, item);
            } else {
                value0 = this.getBase();
                value1 = intervalDataset.getYValue(series, item);
            }

            if (!Double.isNaN(value0) && !Double.isNaN(value1)) {
                if (value0 <= value1) {
                    if (!rangeAxis.getRange().intersects(value0, value1)) {
                        return;
                    }
                } else if (!rangeAxis.getRange().intersects(value1, value0)) {
                    return;
                }

                double translatedValue0 = rangeAxis.valueToJava2D(value0, dataArea, plot.getRangeAxisEdge());
                if (!Double.isFinite(translatedValue0)) {
                    //If value0 is not visible then it's either this.base (by default 0) as many transformations are not defined at 0, or it is genuinely not visible.
                    //According to the above assumption, we're checking if data's/axis range's lowerbound is visible, then it will be new base
                    translatedValue0 = rangeAxis.valueToJava2D(rangeAxis.getLowerBound() - rangeAxis.getLowerBound() * rangeAxis.getLowerMargin(), dataArea, plot.getRangeAxisEdge());
                }
                double translatedValue1 = rangeAxis.valueToJava2D(value1, dataArea, plot.getRangeAxisEdge());
                //return if y-value is invisible
                if (!Double.isFinite(translatedValue1)) {
                    return;
                }
                double bottom = Math.min(translatedValue0, translatedValue1);
                double top = Math.max(translatedValue0, translatedValue1);

                double xValue = intervalDataset.getXValue(series, item);
                //return if x-value is invisible
                if (!Double.isFinite(xValue)) {
                    return;
                }

                double startX = intervalDataset.getStartXValue(series, item);
                if (!Double.isNaN(startX)) {
                    double endX = intervalDataset.getEndXValue(series, item);
                    if (!Double.isNaN(endX)) {
                        if (startX <= endX) {
                            if (!domainAxis.getRange().intersects(startX, endX)) {
                                return;
                            }
                        } else if (!domainAxis.getRange().intersects(endX, startX)) {
                            return;
                        }

                        if (this.getBarAlignmentFactor() >= 0.0D && this.getBarAlignmentFactor() <= 1.0D) {
                            double location = intervalDataset.getXValue(series, item);
                            double interval = endX - startX;
                            startX = location - interval * this.getBarAlignmentFactor();
                            endX = startX + interval;
                        }

                        RectangleEdge location1 = plot.getDomainAxisEdge();
                        double translatedStartX = domainAxis.valueToJava2D(startX, dataArea, location1);
                        //If startX is invisible, and execution has come to this point which means that actual x-value is visible
                        translatedStartX = !Double.isFinite(translatedStartX) ? domainAxis.valueToJava2D(xValue, dataArea, location1) : translatedStartX;
                        double translatedEndX = domainAxis.valueToJava2D(endX, dataArea, location1);
                        //If endX is invisible, and execution has come to this point which means that actual x-value is visible
                        translatedEndX = !Double.isFinite(translatedEndX) ? domainAxis.valueToJava2D(xValue, dataArea, location1) : translatedEndX;
                        double translatedWidth = Math.max(1.0D, Math.abs(translatedEndX - translatedStartX));
                        double left = Math.min(translatedStartX, translatedEndX);
                        if (this.getMargin() > 0.0D) {
                            double bar = translatedWidth * this.getMargin();
                            translatedWidth -= bar;
                            left += bar / 2.0D;
                        }

                        Rectangle2D.Double bar1 = null;
                        PlotOrientation orientation = plot.getOrientation();
                        if (orientation == PlotOrientation.HORIZONTAL) {
                            bottom = Math.max(bottom, dataArea.getMinX());
                            top = Math.min(top, dataArea.getMaxX());
                            bar1 = new Rectangle2D.Double(bottom, left, top - bottom, translatedWidth);
                        } else if (orientation == PlotOrientation.VERTICAL) {
                            bottom = Math.max(bottom, dataArea.getMinY());
                            top = Math.min(top, dataArea.getMaxY());
                            bar1 = new Rectangle2D.Double(left, bottom, translatedWidth, top - bottom);
                        }

                        boolean positive = value1 > 0.0D;
                        boolean inverted = rangeAxis.isInverted();
                        RectangleEdge barBase;
                        if (orientation == PlotOrientation.HORIZONTAL) {
                            if ((!positive || !inverted) && (positive || inverted)) {
                                barBase = RectangleEdge.LEFT;
                            } else {
                                barBase = RectangleEdge.RIGHT;
                            }
                        } else if ((!positive || inverted) && (positive || !inverted)) {
                            barBase = RectangleEdge.TOP;
                        } else {
                            barBase = RectangleEdge.BOTTOM;
                        }

                        //getBarPainter() calls replaced with getBarPainter(series) calls
                        if (this.getShadowsVisible()) {
                            //this.getBarPainter().paintBarShadow(g2, this, series, item, bar1, barBase, !this.getUseYInterval());
                            this.getBarPainter(series).paintBarShadow(g2, this, series, item, bar1, barBase, !this.getUseYInterval());
                        }

                        //this.getBarPainter().paintBar(g2, this, series, item, bar1, barBase);
                        this.getBarPainter(series).paintBar(g2, this, series, item, bar1, barBase);
                        if (this.isItemLabelVisible(series, item)) {
                            XYItemLabelGenerator x1 = this.getItemLabelGenerator(series, item);
                            this.drawItemLabel(g2, dataset, series, item, plot, x1, bar1, value1 < 0.0D);
                        }

                        double x11 = (startX + endX) / 2.0D;
                        double y1 = dataset.getYValue(series, item);
                        double transX1 = domainAxis.valueToJava2D(x11, dataArea, location1);
                        //If transX1 is invisible (which would be very rare as x11 is most of the times x-Value, and execution has come to this point which means that actual x-value is visible
                        transX1 = Double.isNaN(transX1) ? domainAxis.valueToJava2D(xValue, dataArea, location1) : transX1;

                        double transY1 = rangeAxis.valueToJava2D(y1, dataArea, plot.getRangeAxisEdge());
                        //return if y-value is invisible
                        if (Double.isNaN(transY1)) {
                            return;
                        }
                        int domainAxisIndex = plot.getDomainAxisIndex(domainAxis);
                        int rangeAxisIndex = plot.getRangeAxisIndex(rangeAxis);
                        this.updateCrosshairValues(crosshairState, x11, y1, domainAxisIndex, rangeAxisIndex, transX1, transY1, plot.getOrientation());
                        EntityCollection entities = state.getEntityCollection();
                        if (entities != null) {
                            this.addEntity(entities, bar1, dataset, series, item, 0.0D, 0.0D);
                        }

                    }
                }
            }
        }
    }

    protected abstract XYBarPainter getBarPainter(final int index);

}
