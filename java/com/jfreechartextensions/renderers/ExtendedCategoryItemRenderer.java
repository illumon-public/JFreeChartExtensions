/*
 * Copyright (c) 2016-2018 Illumon and Patent Pending
 */

package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public interface ExtendedCategoryItemRenderer extends CategoryItemRenderer {

    void drawErrorBars(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, double x, Paint errorBarColor);

    default void drawErrorBars(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, double x, Number startY, Number endY, Paint errorBarColor) {
        PlotOrientation orientation = plot.getOrientation();
        double y0;
        double y1;
        RectangleEdge edge;
        double yy0;
        double yy1;
        Line2D.Double line;
        Line2D.Double cap1;
        Line2D.Double cap2;
        double adj;
        double capLength = 4.0D;
        y0 = startY == null ? Double.NaN : startY.doubleValue();
        y1 = endY == null ? Double.NaN : endY.doubleValue();
        int visibleRow = state.getVisibleSeriesIndex(row);
        if (visibleRow >= 0) {
            edge = plot.getRangeAxisEdge();
            yy0 = rangeAxis.valueToJava2D(y0, dataArea, edge);
            yy1 = rangeAxis.valueToJava2D(y1, dataArea, edge);
            adj = capLength / 2.0D;
            if (orientation == PlotOrientation.VERTICAL) {
                line = new Line2D.Double(x, yy0, x, yy1);
                cap1 = new Line2D.Double(x - adj, yy0, x + adj, yy0);
                cap2 = new Line2D.Double(x - adj, yy1, x + adj, yy1);
            } else {
                //noinspection SuspiciousNameCombination this is horizontal plot, so x and y are flipped
                line = new Line2D.Double(yy0, x, yy1, x);
                cap1 = new Line2D.Double(yy0, x - adj, yy0, x + adj);
                cap2 = new Line2D.Double(yy1, x - adj, yy1, x + adj);
            }

            g2.setPaint(errorBarColor);
            g2.setStroke(this.getItemStroke(row, column));

            g2.draw(line);
            g2.draw(cap1);
            g2.draw(cap2);
        }
    }
}
