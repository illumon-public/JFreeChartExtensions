package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.XYBarPainter;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public abstract class ExtendedXYStackedBarRenderer extends StackedXYBarRenderer  {
    private static final long serialVersionUID = 439797581034720890L;

    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
        if(this.getItemVisible(series, item)) {
            if(dataset instanceof IntervalXYDataset && dataset instanceof TableXYDataset) {
                IntervalXYDataset var48 = (IntervalXYDataset)dataset;
                double var47 = var48.getYValue(series, item);
                if(!Double.isNaN(var47)) {
                    double total = 0.0D;
                    if(this.getRenderAsPercentages()) {
                        total = DatasetUtilities.calculateStackTotal((TableXYDataset)dataset, item);
                        var47 /= total;
                    }

                    double positiveBase = 0.0D;
                    double negativeBase = 0.0D;

                    for(int translatedBase = 0; translatedBase < series; ++translatedBase) {
                        double v = dataset.getYValue(translatedBase, item);
                        if(!Double.isNaN(v) && this.isSeriesVisible(translatedBase)) {
                            if(this.getRenderAsPercentages()) {
                                v /= total;
                            }

                            if(v > 0.0D) {
                                positiveBase += v;
                            } else {
                                negativeBase += v;
                            }
                        }
                    }

                    RectangleEdge edgeR = plot.getRangeAxisEdge();
                    double translatedValue;
                    double var49;
                    if(var47 > 0.0D) {
                        var49 = rangeAxis.valueToJava2D(positiveBase, dataArea, edgeR);
                        translatedValue = rangeAxis.valueToJava2D(positiveBase + var47, dataArea, edgeR);
                    } else {
                        var49 = rangeAxis.valueToJava2D(negativeBase, dataArea, edgeR);
                        translatedValue = rangeAxis.valueToJava2D(negativeBase + var47, dataArea, edgeR);
                    }

                    RectangleEdge edgeD = plot.getDomainAxisEdge();
                    double startX = var48.getStartXValue(series, item);
                    if(!Double.isNaN(startX)) {
                        double translatedStartX = domainAxis.valueToJava2D(startX, dataArea, edgeD);
                        double endX = var48.getEndXValue(series, item);
                        if(!Double.isNaN(endX)) {
                            double translatedEndX = domainAxis.valueToJava2D(endX, dataArea, edgeD);
                            double translatedWidth = Math.max(1.0D, Math.abs(translatedEndX - translatedStartX));
                            double translatedHeight = Math.abs(translatedValue - var49);
                            if(this.getMargin() > 0.0D) {
                                double bar = translatedWidth * this.getMargin();
                                translatedWidth -= bar;
                                translatedStartX += bar / 2.0D;
                            }

                            java.awt.geom.Rectangle2D.Double var50 = null;
                            PlotOrientation orientation = plot.getOrientation();
                            if(orientation == PlotOrientation.HORIZONTAL) {
                                var50 = new java.awt.geom.Rectangle2D.Double(Math.min(var49, translatedValue), Math.min(translatedEndX, translatedStartX), translatedHeight, translatedWidth);
                            } else {
                                if(orientation != PlotOrientation.VERTICAL) {
                                    throw new IllegalStateException();
                                }

                                var50 = new java.awt.geom.Rectangle2D.Double(Math.min(translatedStartX, translatedEndX), Math.min(var49, translatedValue), translatedWidth, translatedHeight);
                            }

                            boolean positive = var47 > 0.0D;
                            boolean inverted = rangeAxis.isInverted();
                            RectangleEdge barBase;
                            if(orientation == PlotOrientation.HORIZONTAL) {
                                if((!positive || !inverted) && (positive || inverted)) {
                                    barBase = RectangleEdge.LEFT;
                                } else {
                                    barBase = RectangleEdge.RIGHT;
                                }
                            } else if((!positive || inverted) && (positive || !inverted)) {
                                barBase = RectangleEdge.TOP;
                            } else {
                                barBase = RectangleEdge.BOTTOM;
                            }

                            if(pass == 0) {
                                if(this.getShadowsVisible()) {
                                    this.getBarPainter(series).paintBarShadow(g2, this, series, item, var50, barBase, false);
                                }
                            } else if(pass == 1) {
                                this.getBarPainter(series).paintBar(g2, this, series, item, var50, barBase);
                                if(info != null) {
                                    EntityCollection generator = info.getOwner().getEntityCollection();
                                    if(generator != null) {
                                        this.addEntity(generator, var50, dataset, series, item, var50.getCenterX(), var50.getCenterY());
                                    }
                                }
                            } else if(pass == 2 && this.isItemLabelVisible(series, item)) {
                                XYItemLabelGenerator var46 = this.getItemLabelGenerator(series, item);
                                this.drawItemLabel(g2, dataset, series, item, plot, var46, var50, var47 < 0.0D);
                            }

                        }
                    }
                }
            } else {
                String intervalDataset = "dataset (type " + dataset.getClass().getName() + ") has wrong type:";
                boolean value = false;
                if(!IntervalXYDataset.class.isAssignableFrom(dataset.getClass())) {
                    intervalDataset = intervalDataset + " it is no IntervalXYDataset";
                    value = true;
                }

                if(!TableXYDataset.class.isAssignableFrom(dataset.getClass())) {
                    if(value) {
                        intervalDataset = intervalDataset + " and";
                    }

                    intervalDataset = intervalDataset + " it is no TableXYDataset";
                }

                throw new IllegalArgumentException(intervalDataset);
            }
        }
    }

    protected abstract XYBarPainter getBarPainter(final int index);
}
