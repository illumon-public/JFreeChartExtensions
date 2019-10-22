package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.AreaRendererEndType;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * Area rendered that supports AxisTransformations
 */
public class ExtendedAreaRenderer extends AreaRenderer {

    @Override
    public void drawItem(Graphics2D g2, CategoryItemRendererState state,
                         Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis,
                         ValueAxis rangeAxis, CategoryDataset dataset, int row, int column,
                         int pass) {

        // do nothing if item is not visible or null
        if (!getItemVisible(row, column)) {
            return;
        }
        Number value = dataset.getValue(row, column);
        if (value == null) {
            return;
        }

        double yy1 = value.doubleValue();

        RectangleEdge edge = plot.getRangeAxisEdge();
        double y1 = rangeAxis.valueToJava2D(yy1, dataArea, edge);

        //Render only if current point is visible
        if (Double.isFinite(y1)) {
            PlotOrientation orientation = plot.getOrientation();
            RectangleEdge axisEdge = plot.getDomainAxisEdge();
            int count = dataset.getColumnCount();
            double x0 = domainAxis.getCategoryStart(column, count, dataArea, axisEdge);
            double x1 = domainAxis.getCategoryMiddle(column, count, dataArea, axisEdge);
            double x2 = domainAxis.getCategoryEnd(column, count, dataArea, axisEdge);

            x0 = Math.round(x0);
            x1 = Math.round(x1);
            x2 = Math.round(x2);

            if   (this.getEndType() == AreaRendererEndType.TRUNCATE) {
                if (column == 0) {
                    x0 = x1;
                } else if (column == getColumnCount() - 1) {
                    x2 = x1;
                }
            }

            double yy0 = 0.0;
            boolean previousPointVisible = true;
            if (this.getEndType() == AreaRendererEndType.LEVEL) {
                yy0 = yy1;
            }
            if (column > 0) {
                Number n0 = dataset.getValue(row, column - 1);
                if (n0 != null) {
                    double translatedPrevious = rangeAxis.valueToJava2D(n0.doubleValue(), dataArea, edge);
                    if (!Double.isFinite(translatedPrevious)) {
                        previousPointVisible = false;
                    } else {
                        yy0 = (n0.doubleValue() + yy1) / 2.0;
                    }
                }
            }

            double yy2 = 0.0;
            boolean nextPointVisible = true;
            if (column < dataset.getColumnCount() - 1) {
                Number n2 = dataset.getValue(row, column + 1);
                if (n2 != null) {
                    double translatedNext = rangeAxis.valueToJava2D(n2.doubleValue(), dataArea, edge);
                    if (!Double.isFinite(translatedNext)) {
                        nextPointVisible = false;
                    } else {
                        yy2 = (n2.doubleValue() + yy1) / 2.0;
                    }
                }
            } else if (this.getEndType() == AreaRendererEndType.LEVEL) {
                yy2 = yy1;
            }


            double yz = rangeAxis.valueToJava2D(0.0, dataArea, edge);
            yz = Double.isFinite(yz) ? yz : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge);

            double y0 = previousPointVisible ? rangeAxis.valueToJava2D(yy0, dataArea, edge) : y1;

            double y2 = nextPointVisible ? rangeAxis.valueToJava2D(yy2, dataArea, edge) : y1;

            double labelXX = x1;
            double labelYY = y1;
            g2.setPaint(getItemPaint(row, column));
            g2.setStroke(getItemStroke(row, column));

            GeneralPath area = new GeneralPath();

            if (orientation == PlotOrientation.VERTICAL) {
                area.moveTo(x0, yz);
                area.lineTo(x0, y0);
                area.lineTo(x1, y1);
                area.lineTo(x2, y2);
                area.lineTo(x2, yz);
            } else if (orientation == PlotOrientation.HORIZONTAL) {
                area.moveTo(yz, x0);
                area.lineTo(y0, x0);
                area.lineTo(y1, x1);
                area.lineTo(y2, x2);
                area.lineTo(yz, x2);
                double temp = labelXX;
                labelXX = labelYY;
                labelYY = temp;
            }
            area.closePath();

            g2.setPaint(getItemPaint(row, column));
            g2.fill(area);

            // draw the item labels if there are any...
            if (isItemLabelVisible(row, column)) {
                drawItemLabel(g2, orientation, dataset, row, column, labelXX,
                        labelYY, (value.doubleValue() < 0.0));
            }

            // submit the current data point as a crosshair candidate
            int datasetIndex = plot.indexOf(dataset);
            updateCrosshairValues(state.getCrosshairState(),
                    dataset.getRowKey(row), dataset.getColumnKey(column), yy1,
                    datasetIndex, x1, y1, orientation);

            // add an item entity, if this information is being collected
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
                addItemEntity(entities, dataset, row, column, area);
            }
        }
    }
}
