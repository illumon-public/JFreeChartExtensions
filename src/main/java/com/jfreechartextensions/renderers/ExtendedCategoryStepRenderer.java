package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.CategoryStepRenderer;
import org.jfree.data.category.CategoryDataset;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * Step Renderer for categorical data that supports AxisTransformations
 */
public class ExtendedCategoryStepRenderer extends CategoryStepRenderer {

    @Override
    public void drawItem(Graphics2D g2, CategoryItemRendererState state,
                         Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis,
                         ValueAxis rangeAxis, CategoryDataset dataset, int row,
                         int column, int pass) {

        // do nothing if item is not visible
        if (!getItemVisible(row, column)) {
            return;
        }

        Number value = dataset.getValue(row, column);
        if (value == null) {
            return;
        }
        PlotOrientation orientation = plot.getOrientation();

        // current data point...
        double x1s = domainAxis.getCategoryStart(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double x1 = domainAxis.getCategoryMiddle(column, getColumnCount(), dataArea, plot.getDomainAxisEdge());
        double x1e = 2 * x1 - x1s; // or: x1s + 2*(x1-x1s)
        double y1 = rangeAxis.valueToJava2D(value.doubleValue(), dataArea, plot.getRangeAxisEdge());
        if (Double.isFinite(y1)) {
            g2.setPaint(getItemPaint(row, column));
            g2.setStroke(getItemStroke(row, column));

            if (column != 0) {
                Number previousValue = dataset.getValue(row, column - 1);

                if (previousValue != null) {
                    // previous data point...
                    double previous = previousValue.doubleValue();

                    double x0s = domainAxis.getCategoryStart(column - 1, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                    double x0 = domainAxis.getCategoryMiddle(column - 1, getColumnCount(), dataArea, plot.getDomainAxisEdge());
                    double x0e = 2 * x0 - x0s; // or: x0s + 2*(x0-x0s)

                    double y0 = rangeAxis.valueToJava2D(previous, dataArea, plot.getRangeAxisEdge());

                    if (getStagger()) {
                        int xStagger = row * STAGGER_WIDTH;
                        if (xStagger > (x1s - x0e)) {
                            xStagger = (int) (x1s - x0e);
                        }
                        x1s = x0e + xStagger;
                    }
                    if (Double.isFinite(x0) && Double.isFinite(y0)) {
                        drawLine(g2, (State) state, orientation, x0e, y0, x1s, y0);
                    }
                    // extend x0's flat bar

                    drawLine(g2, (State) state, orientation, x1s, y0, x1s, y1);
                    // upright bar
                }
            }
            drawLine(g2, (State) state, orientation, x1s, y1, x1e, y1);
            // x1's flat bar

            // draw the item labels if there are any...
            if (isItemLabelVisible(row, column)) {
                drawItemLabel(g2, orientation, dataset, row, column, x1, y1,
                        (value.doubleValue() < 0.0));
            }

            // add an item entity, if this information is being collected
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
                Rectangle2D hotspot = new Rectangle2D.Double();
                if (orientation == PlotOrientation.VERTICAL) {
                    hotspot.setRect(x1s, y1, x1e - x1s, 4.0);
                } else {
                    hotspot.setRect(y1 - 2.0, x1s, 4.0, x1e - x1s);
                }
                addItemEntity(entities, dataset, row, column, hotspot);
            }
        }
    }

}
