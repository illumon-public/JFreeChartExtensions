package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.util.ShapeUtilities;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public abstract class ExtendedLineAndShapeRenderer extends LineAndShapeRenderer {

    public ExtendedLineAndShapeRenderer(boolean lines, boolean shapes) {
        super(lines, shapes);
    }

    protected abstract Paint getLinePaint(final int row, final int col);

    @Override
    public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, int pass) {
        if(this.getItemVisible(row, column)) {
            if(this.getItemLineVisible(row, column) || this.getItemShapeVisible(row, column)) {
                Number v = dataset.getValue(row, column);
                if(v != null) {
                    int visibleRow = state.getVisibleSeriesIndex(row);
                    if(visibleRow >= 0) {
                        int visibleRowCount = state.getVisibleSeriesCount();
                        PlotOrientation orientation = plot.getOrientation();
                        double x1;
                        if(this.getUseSeriesOffset()) {
                            x1 = domainAxis.getCategorySeriesMiddle(column, dataset.getColumnCount(), visibleRow, visibleRowCount, this.getItemMargin(), dataArea, plot.getDomainAxisEdge());
                        } else {
                            x1 = domainAxis.getCategoryMiddle(column, this.getColumnCount(), dataArea, plot.getDomainAxisEdge());
                        }

                        double value = v.doubleValue();
                        double y1 = rangeAxis.valueToJava2D(value, dataArea, plot.getRangeAxisEdge());
                        //return if current point is not visible
                        if(Double.isNaN(y1)){
                            return;
                        }
                        if(pass == 0 && this.getItemLineVisible(row, column) && column != 0) {
                            Number shape = dataset.getValue(row, column - 1);
                            if(shape != null) {
                                double datasetIndex = shape.doubleValue();
                                double x0;
                                if(this.getUseSeriesOffset()) {
                                    x0 = domainAxis.getCategorySeriesMiddle(column - 1, dataset.getColumnCount(), visibleRow, visibleRowCount, this.getItemMargin(), dataArea, plot.getDomainAxisEdge());
                                } else {
                                    x0 = domainAxis.getCategoryMiddle(column - 1, this.getColumnCount(), dataArea, plot.getDomainAxisEdge());
                                }

                                double y0 = rangeAxis.valueToJava2D(datasetIndex, dataArea, plot.getRangeAxisEdge());
                                //return if previous point is not visible.
                                // With this implementation, if there is any invisible point between two visible points, then there WON'T be line drawn between visible points.
                                if(Double.isNaN(y0)){
                                    return;
                                }
                                Line2D.Double line = null;
                                if(orientation == PlotOrientation.HORIZONTAL) {
                                    line = new Line2D.Double(y0, x0, y1, x1);
                                } else if(orientation == PlotOrientation.VERTICAL) {
                                    line = new Line2D.Double(x0, y0, x1, y1);
                                }

                                //g2.setPaint(this.getItemPaint(row, column));
                                //changed to allow only one line color per series
                                g2.setPaint(this.getLinePaint(row, column));
                                g2.setStroke(this.getItemStroke(row, column));
                                g2.draw(line);
                            }
                        }

                        if(pass == 1) {
                            Shape shape1 = this.getItemShape(row, column);
                            if(orientation == PlotOrientation.HORIZONTAL) {
                                shape1 = ShapeUtilities.createTranslatedShape(shape1, y1, x1);
                            } else if(orientation == PlotOrientation.VERTICAL) {
                                shape1 = ShapeUtilities.createTranslatedShape(shape1, x1, y1);
                            }

                            if(this.getItemShapeVisible(row, column)) {
                                if(this.getItemShapeFilled(row, column)) {
                                    if(this.getUseFillPaint()) {
                                        g2.setPaint(this.getItemFillPaint(row, column));
                                    } else {
                                        g2.setPaint(this.getItemPaint(row, column));
                                    }

                                    g2.fill(shape1);
                                }

                                if(this.getDrawOutlines()) {
                                    if(this.getUseOutlinePaint()) {
                                        g2.setPaint(this.getItemOutlinePaint(row, column));
                                    } else {
                                        g2.setPaint(this.getItemPaint(row, column));
                                    }

                                    g2.setStroke(this.getItemOutlineStroke(row, column));
                                    g2.draw(shape1);
                                }
                            }

                            if(this.isItemLabelVisible(row, column)) {
                                if(orientation == PlotOrientation.HORIZONTAL) {
                                    this.drawItemLabel(g2, orientation, dataset, row, column, y1, x1, value < 0.0D);
                                } else if(orientation == PlotOrientation.VERTICAL) {
                                    this.drawItemLabel(g2, orientation, dataset, row, column, x1, y1, value < 0.0D);
                                }
                            }

                            int datasetIndex1 = plot.indexOf(dataset);
                            this.updateCrosshairValues(state.getCrosshairState(), dataset.getRowKey(row), dataset.getColumnKey(column), value, datasetIndex1, x1, y1, orientation);
                            EntityCollection entities = state.getEntityCollection();
                            if(entities != null) {
                                this.addItemEntity(entities, dataset, row, column, shape1);
                            }
                        }

                    }
                }
            }
        }
    }

}
