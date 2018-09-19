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
        if (!getItemVisible(series, item)) {
            return;
        }
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
        if (Double.isNaN(value0) || Double.isNaN(value1)) {
            return;
        }
        if (value0 <= value1) {
            if (!rangeAxis.getRange().intersects(value0, value1)) {
                return;
            }
        } else {
            if (!rangeAxis.getRange().intersects(value1, value0)) {
                return;
            }
        }

        double translatedValue0 = rangeAxis.valueToJava2D(value0, dataArea,
                plot.getRangeAxisEdge());
        double translatedValue1 = rangeAxis.valueToJava2D(value1, dataArea,
                plot.getRangeAxisEdge());
        double bottom = Math.min(translatedValue0, translatedValue1);
        double top = Math.max(translatedValue0, translatedValue1);

        double startX = intervalDataset.getStartXValue(series, item);
        if (Double.isNaN(startX)) {
            return;
        }
        double endX = intervalDataset.getEndXValue(series, item);
        if (Double.isNaN(endX)) {
            return;
        }
        if (startX <= endX) {
            if (!domainAxis.getRange().intersects(startX, endX)) {
                return;
            }
        } else {
            if (!domainAxis.getRange().intersects(endX, startX)) {
                return;
            }
        }

        // is there an alignment adjustment to be made?
        if (this.getBarAlignmentFactor() >= 0.0 && this.getBarAlignmentFactor() <= 1.0) {
            double x = intervalDataset.getXValue(series, item);
            double interval = endX - startX;
            startX = x - interval * this.getBarAlignmentFactor();
            endX = startX + interval;
        }

        RectangleEdge location = plot.getDomainAxisEdge();
        double translatedStartX = domainAxis.valueToJava2D(startX, dataArea,
                location);
        double translatedEndX = domainAxis.valueToJava2D(endX, dataArea,
                location);

        double translatedWidth = Math.max(1, Math.abs(translatedEndX
                - translatedStartX));

        double left = Math.min(translatedStartX, translatedEndX);
        if (getMargin() > 0.0) {
            double cut = translatedWidth * getMargin();
            translatedWidth = translatedWidth - cut;
            left = left + cut / 2;
        }

        Rectangle2D bar = null;
        PlotOrientation orientation = plot.getOrientation();
        if (orientation.isHorizontal()) {
            // clip left and right bounds to data area
            bottom = Math.max(bottom, dataArea.getMinX());
            top = Math.min(top, dataArea.getMaxX());
            bar = new Rectangle2D.Double(
                    bottom, left, top - bottom, translatedWidth);
        } else if (orientation.isVertical()) {
            // clip top and bottom bounds to data area
            bottom = Math.max(bottom, dataArea.getMinY());
            top = Math.min(top, dataArea.getMaxY());
            bar = new Rectangle2D.Double(left, bottom, translatedWidth,
                    top - bottom);
        }

        boolean positive = (value1 > 0.0);
        boolean inverted = rangeAxis.isInverted();
        RectangleEdge barBase;
        if (orientation.isHorizontal()) {
            if (positive && inverted || !positive && !inverted) {
                barBase = RectangleEdge.RIGHT;
            } else {
                barBase = RectangleEdge.LEFT;
            }
        } else {
            if (positive && !inverted || !positive && inverted) {
                barBase = RectangleEdge.BOTTOM;
            } else {
                barBase = RectangleEdge.TOP;
            }
        }

        if (state.getElementHinting()) {
            beginElementGroup(g2, dataset.getSeriesKey(series), item);
        }
        if (getShadowsVisible()) {
            this.getBarPainter(series).paintBarShadow(g2, this, series, item, bar, barBase,
                    !this.getUseYInterval());
        }
        this.getBarPainter(series).paintBar(g2, this, series, item, bar, barBase);
        if (state.getElementHinting()) {
            endElementGroup(g2);
        }

        if (isItemLabelVisible(series, item)) {
            XYItemLabelGenerator generator = getItemLabelGenerator(series,
                    item);
            drawItemLabel(g2, dataset, series, item, plot, generator, bar,
                    value1 < 0.0);
        }

        // update the crosshair point
        double x1 = (startX + endX) / 2.0;
        double y1 = dataset.getYValue(series, item);
        double transX1 = domainAxis.valueToJava2D(x1, dataArea, location);
        double transY1 = rangeAxis.valueToJava2D(y1, dataArea,
                plot.getRangeAxisEdge());
        int datasetIndex = plot.indexOf(dataset);
        updateCrosshairValues(crosshairState, x1, y1, datasetIndex,
                transX1, transY1, plot.getOrientation());

        EntityCollection entities = state.getEntityCollection();
        if (entities != null) {
            addEntity(entities, bar, dataset, series, item, 0.0, 0.0);
        }

    }

    protected abstract XYBarPainter getBarPainter(final int index);

}
