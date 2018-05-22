package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarPainter;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public abstract class ExtendedBarRenderer extends BarRenderer implements ExtendedCategoryItemRenderer{

    protected abstract BarPainter getBarPainter(final int index);

    @Override
    public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, int pass) {
        int visibleRow = state.getVisibleSeriesIndex(row);
        if (visibleRow >= 0) {
            Number dataValue = dataset.getValue(row, column);
            if (dataValue != null) {
                double value = dataValue.doubleValue();
                PlotOrientation orientation = plot.getOrientation();
                double barW0 = this.calculateBarW0(plot, orientation, dataArea, domainAxis, state, visibleRow, column);
                double[] barL0L1 = this.calculateBarL0L1(value);
                if (barL0L1 != null) {
                    RectangleEdge edge = plot.getRangeAxisEdge();
                    double transL0 = rangeAxis.valueToJava2D(barL0L1[0], dataArea, edge);
                    double transL1 = rangeAxis.valueToJava2D(barL0L1[1], dataArea, edge);
                    boolean positive = value >= this.getBase();
                    boolean inverted = rangeAxis.isInverted();
                    double barL0 = Math.min(transL0, transL1);
                    double barLength = Math.abs(transL1 - transL0);
                    double barLengthAdj = 0.0D;
                    if (barLength > 0.0D && barLength < this.getMinimumBarLength()) {
                        barLengthAdj = this.getMinimumBarLength() - barLength;
                    }

                    double barL0Adj = 0.0D;
                    RectangleEdge barBase;
                    if (orientation == PlotOrientation.HORIZONTAL) {
                        if ((!positive || !inverted) && (positive || inverted)) {
                            barBase = RectangleEdge.LEFT;
                        } else {
                            barL0Adj = barLengthAdj;
                            barBase = RectangleEdge.RIGHT;
                        }
                    } else if ((!positive || inverted) && (positive || !inverted)) {
                        barBase = RectangleEdge.TOP;
                    } else {
                        barL0Adj = barLengthAdj;
                        barBase = RectangleEdge.BOTTOM;
                    }

                    Rectangle2D.Double bar;
                    //getBarPainter() calls replaced with getBarPainter(series) calls
                    if (orientation == PlotOrientation.HORIZONTAL) {
                        bar = new Rectangle2D.Double(barL0 - barL0Adj, barW0, barLength + barLengthAdj, state.getBarWidth());
                    } else {
                        bar = new Rectangle2D.Double(barW0, barL0 - barL0Adj, state.getBarWidth(), barLength + barLengthAdj);
                    }

                    if (this.getShadowsVisible()) {
                        //this.getBarPainter().paintBarShadow(g2, this, row, column, bar, barBase, true);
                        this.getBarPainter(row).paintBarShadow(g2, this, row, column, bar, barBase, true);
                    }

                    double x = barW0 + (state.getBarWidth() / 2);
                    drawErrorBars(g2, state, dataArea, plot, rangeAxis, dataset, row, column, x, Color.BLACK);

                    //this.getBarPainter().paintBar(g2, this, row, column, bar, barBase);
                    this.getBarPainter(row).paintBar(g2, this, row, column, bar, barBase);
                    CategoryItemLabelGenerator generator = this.getItemLabelGenerator(row, column);
                    if (generator != null && this.isItemLabelVisible(row, column)) {
                        this.drawItemLabel(g2, dataset, row, column, plot, generator, bar, value < 0.0D);
                    }

                    int datasetIndex = plot.indexOf(dataset);
                    this.updateCrosshairValues(state.getCrosshairState(), dataset.getRowKey(row), dataset.getColumnKey(column), value, datasetIndex, barW0, barL0, orientation);
                    EntityCollection entities = state.getEntityCollection();
                    if (entities != null) {
                        this.addItemEntity(entities, dataset, row, column, bar);
                    }

                }
            }
        }
    }

}
