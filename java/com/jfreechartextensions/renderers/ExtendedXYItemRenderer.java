/*
 * Copyright (c) 2016-2018 Illumon and Patent Pending
 */

package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public interface ExtendedXYItemRenderer extends XYItemRenderer {

    default void drawErrorBars(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataSeries, int series, int item, int pass, double seriesX, double startX, double endX, double seriesY, double startY, double endY, boolean drawXError, boolean drawYError, Paint color) {
        if (pass == 0 && this.getItemVisible(series, item)) {
            PlotOrientation orientation = plot.getOrientation();
            double y0;
            double y1;
            double x;
            RectangleEdge edge;
            double yy0;
            double yy1;
            double xx;
            Line2D.Double line;
            Line2D.Double cap1;
            Line2D.Double cap2;
            double adj;
            double capLength = 4.0D;
            if (drawXError) {
                y0 = startX;
                y1 = endX;
                x = seriesY;
                edge = plot.getDomainAxisEdge();
                yy0 = domainAxis.valueToJava2D(y0, dataArea, edge);
                yy1 = domainAxis.valueToJava2D(y1, dataArea, edge);
                xx = rangeAxis.valueToJava2D(x, dataArea, plot.getRangeAxisEdge());
                adj = capLength / 2.0D;
                if (orientation == PlotOrientation.VERTICAL) {
                    line = new Line2D.Double(yy0, xx, yy1, xx);
                    cap1 = new Line2D.Double(yy0, xx - adj, yy0, xx + adj);
                    cap2 = new Line2D.Double(yy1, xx - adj, yy1, xx + adj);
                } else {
                    line = new Line2D.Double(xx, yy0, xx, yy1);
                    cap1 = new Line2D.Double(xx - adj, yy0, xx + adj, yy0);
                    cap2 = new Line2D.Double(xx - adj, yy1, xx + adj, yy1);
                }

                g2.setPaint(color);
                g2.setStroke(this.getItemStroke(series, item));

                g2.draw(line);
                g2.draw(cap1);
                g2.draw(cap2);
            }

            if (drawYError) {
                y0 = startY;
                y1 = endY;
                x = seriesX;
                edge = plot.getRangeAxisEdge();
                yy0 = rangeAxis.valueToJava2D(y0, dataArea, edge);
                yy1 = rangeAxis.valueToJava2D(y1, dataArea, edge);
                xx = domainAxis.valueToJava2D(x, dataArea, plot.getDomainAxisEdge());
                adj = capLength / 2.0D;
                if (orientation == PlotOrientation.VERTICAL) {
                    line = new Line2D.Double(xx, yy0, xx, yy1);
                    cap1 = new Line2D.Double(xx - adj, yy0, xx + adj, yy0);
                    cap2 = new Line2D.Double(xx - adj, yy1, xx + adj, yy1);
                } else {
                    line = new Line2D.Double(yy0, xx, yy1, xx);
                    cap1 = new Line2D.Double(yy0, xx - adj, yy0, xx + adj);
                    cap2 = new Line2D.Double(yy1, xx - adj, yy1, xx + adj);
                }

                g2.setPaint(color);
                g2.setStroke(this.getItemStroke(series, item));

                g2.draw(line);
                g2.draw(cap1);
                g2.draw(cap2);
            }
        }
    }
}
