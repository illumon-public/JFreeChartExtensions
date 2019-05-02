package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarPainter;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.data.KeyToGroupMap;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public abstract class ExtendedGroupedBarRenderer extends GroupedStackedBarRenderer {
    private static final long serialVersionUID = 4734227195046920808L;

    private KeyToGroupMap seriesToGroupMap = new KeyToGroupMap();

    public void setSeriesToGroupMap(KeyToGroupMap map) {
        super.setSeriesToGroupMap(map);
        this.seriesToGroupMap = map;
    }

    public KeyToGroupMap getSeriesToGroupMap() {
        return seriesToGroupMap;
    }

    public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset, int row, int column, int pass) {
        Number dataValue = dataset.getValue(row, column);
        if(dataValue != null) {
            RectangleEdge location = plot.getRangeAxisEdge();
            double value = dataValue.doubleValue();
            //return if value is invisible
            if(!Double.isFinite(rangeAxis.valueToJava2D(value, dataArea, location))){
                return;
            }
            Comparable group = this.seriesToGroupMap.getGroup(dataset.getRowKey(row));
            PlotOrientation orientation = plot.getOrientation();
            double barW0 = this.calculateBarW0(plot, orientation, dataArea, domainAxis, state, row, column);

            double positiveBase = 0.0D;
            double negativeBase = 0.0D;

            double translatedValue;
            for(int translatedBase = 0; translatedBase < row; ++translatedBase) {
                if(group.equals(this.seriesToGroupMap.getGroup(dataset.getRowKey(translatedBase)))) {
                    Number v = dataset.getValue(translatedBase, column);
                    if(v != null) {
                        translatedValue = v.doubleValue();
                        if(translatedValue > 0.0D) {
                            positiveBase += translatedValue;
                        } else {
                            negativeBase += translatedValue;
                        }
                    }
                }
            }

            boolean positive = value > 0.0D;
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

            double var37;
            if(value > 0.0D) {
                var37 = rangeAxis.valueToJava2D(positiveBase, dataArea, location);
                //To prevent log transformation to process 0 as bar base
                var37 = Double.isFinite(var37) ? var37 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, location);

                translatedValue = rangeAxis.valueToJava2D(positiveBase + value, dataArea, location);
            } else {
                var37 = rangeAxis.valueToJava2D(negativeBase, dataArea, location);
                translatedValue = rangeAxis.valueToJava2D(negativeBase + value, dataArea, location);
            }

            double barL0 = Math.min(var37, translatedValue);
            double barLength = Math.max(Math.abs(translatedValue - var37), this.getMinimumBarLength());
            Rectangle2D.Double bar;
            if(orientation == PlotOrientation.HORIZONTAL) {
                bar = new Rectangle2D.Double(barL0, barW0, barLength, state.getBarWidth());
            } else {
                bar = new Rectangle2D.Double(barW0, barL0, state.getBarWidth(), barLength);
            }

            this.getBarPainter(row).paintBar(g2, this, row, column, bar, barBase);
            CategoryItemLabelGenerator generator = this.getItemLabelGenerator(row, column);
            if(generator != null && this.isItemLabelVisible(row, column)) {
                this.drawItemLabel(g2, dataset, row, column, plot, generator, bar, value < 0.0D);
            }

            if(state.getInfo() != null) {
                EntityCollection entities = state.getEntityCollection();
                if(entities != null) {
                    this.addItemEntity(entities, dataset, row, column, bar);
                }
            }

        }
    }

    protected abstract BarPainter getBarPainter(final int index);
}
