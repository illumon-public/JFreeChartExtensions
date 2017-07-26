package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.ShapeUtilities;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

public abstract class ExtendedXYAreaRenderer extends XYAreaRenderer {

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset data, PlotRenderingInfo info) {
        XYAreaRendererState state = new XYAreaRendererState(info);
        state.setProcessVisibleItemsOnly(false);
        return state;
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
        if(this.getItemVisible(series, item)) {
            //XYAreaRenderer.XYAreaRendererState areaState = (XYAreaRenderer.XYAreaRendererState)state;
            XYAreaRendererState areaState = (XYAreaRendererState)state;
            double x1 = dataset.getXValue(series, item);
            double y1 = dataset.getYValue(series, item);
            if(Double.isNaN(y1)) {
                y1 = 0.0D;
            }

            double transX1 = domainAxis.valueToJava2D(x1, dataArea, plot.getDomainAxisEdge());
            double transY1 = rangeAxis.valueToJava2D(y1, dataArea, plot.getRangeAxisEdge());
            int itemCount = dataset.getItemCount(series);
            double x0 = dataset.getXValue(series, Math.max(item - 1, 0));
            double y0 = dataset.getYValue(series, Math.max(item - 1, 0));
            if(Double.isNaN(y0)) {
                y0 = 0.0D;
            }

            double transX0 = domainAxis.valueToJava2D(x0, dataArea, plot.getDomainAxisEdge());
            double transY0 = rangeAxis.valueToJava2D(y0, dataArea, plot.getRangeAxisEdge());
            double x2 = dataset.getXValue(series, Math.min(item + 1, itemCount - 1));
            double y2 = dataset.getYValue(series, Math.min(item + 1, itemCount - 1));
            if(Double.isNaN(y2)) {
                y2 = 0.0D;
            }

            double transX2 = domainAxis.valueToJava2D(x2, dataArea, plot.getDomainAxisEdge());
            double transY2 = rangeAxis.valueToJava2D(y2, dataArea, plot.getRangeAxisEdge());
            double transZero = rangeAxis.valueToJava2D(0.0D, dataArea, plot.getRangeAxisEdge());
            GeneralPath hotspot = new GeneralPath();
            if(plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                moveTo(hotspot, transZero, (transX0 + transX1) / 2.0D);
                lineTo(hotspot, (transY0 + transY1) / 2.0D, (transX0 + transX1) / 2.0D);
                lineTo(hotspot, transY1, transX1);
                lineTo(hotspot, (transY1 + transY2) / 2.0D, (transX1 + transX2) / 2.0D);
                lineTo(hotspot, transZero, (transX1 + transX2) / 2.0D);
            } else {
                moveTo(hotspot, (transX0 + transX1) / 2.0D, transZero);
                lineTo(hotspot, (transX0 + transX1) / 2.0D, (transY0 + transY1) / 2.0D);
                lineTo(hotspot, transX1, transY1);
                lineTo(hotspot, (transX1 + transX2) / 2.0D, (transY1 + transY2) / 2.0D);
                lineTo(hotspot, (transX1 + transX2) / 2.0D, transZero);
            }

            hotspot.closePath();
            if(item == 0) {
                areaState.area = new GeneralPath();
                double orientation = rangeAxis.valueToJava2D(0.0D, dataArea, plot.getRangeAxisEdge());
                if(plot.getOrientation() == PlotOrientation.VERTICAL) {
                    moveTo(areaState.area, transX1, orientation);
                } else if(plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    moveTo(areaState.area, orientation, transX1);
                }
            }

            if(plot.getOrientation() == PlotOrientation.VERTICAL) {
                lineTo(areaState.area, transX1, transY1);
            } else if(plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                lineTo(areaState.area, transY1, transX1);
            }

            PlotOrientation orientation1 = plot.getOrientation();
            Paint paint = this.getItemPaint(series, item);
            Stroke stroke = this.getItemStroke(series, item);
            g2.setPaint(paint);
            g2.setStroke(stroke);
            //if(this.getPlotShapes()) {
            if(this.getPlotShapes(series)) {
                Shape shape = this.getItemShape(series, item);
                if(orientation1 == PlotOrientation.VERTICAL) {
                    shape = ShapeUtilities.createTranslatedShape(shape, transX1, transY1);
                } else if(orientation1 == PlotOrientation.HORIZONTAL) {
                    shape = ShapeUtilities.createTranslatedShape(shape, transY1, transX1);
                }

                g2.draw(shape);
            }

            //if(this.getPlotLines() && item > 0) {
            if(this.getPlotLines(series) && item > 0) {
                if(plot.getOrientation() == PlotOrientation.VERTICAL) {
                    areaState.line.setLine(transX0, transY0, transX1, transY1);
                } else if(plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    areaState.line.setLine(transY0, transX0, transY1, transX1);
                }

                g2.draw(areaState.line);
            }

            if(this.getPlotArea() && item > 0 && item == itemCount - 1) {
                if(orientation1 == PlotOrientation.VERTICAL) {
                    lineTo(areaState.area, transX1, transZero);
                    areaState.area.closePath();
                } else if(orientation1 == PlotOrientation.HORIZONTAL) {
                    lineTo(areaState.area, transZero, transX1);
                    areaState.area.closePath();
                }

                if(this.getUseFillPaint()) {
                    paint = this.lookupSeriesFillPaint(series);
                }

                if(paint instanceof GradientPaint) {
                    GradientPaint domainAxisIndex = (GradientPaint)paint;
                    GradientPaint rangeAxisIndex = this.getGradientTransformer().transform(domainAxisIndex, dataArea);
                    g2.setPaint(rangeAxisIndex);
                }

                g2.fill(areaState.area);
                if(this.isOutline()) {
                    Object domainAxisIndex1 = areaState.area;
                    Stroke rangeAxisIndex1 = this.lookupSeriesOutlineStroke(series);
                    if(rangeAxisIndex1 instanceof BasicStroke) {
                        BasicStroke entities = (BasicStroke)rangeAxisIndex1;
                        if(entities.getDashArray() != null) {
                            Area poly = new Area(areaState.area);
                            Area clip = new Area(new Rectangle2D.Double(dataArea.getX() - 5.0D, dataArea.getY() - 5.0D, dataArea.getWidth() + 10.0D, dataArea.getHeight() + 10.0D));
                            poly.intersect(clip);
                            domainAxisIndex1 = poly;
                        }
                    }

                    g2.setStroke(rangeAxisIndex1);
                    g2.setPaint(this.lookupSeriesOutlinePaint(series));
                    g2.draw((Shape)domainAxisIndex1);
                }
            }

            int domainAxisIndex2 = plot.getDomainAxisIndex(domainAxis);
            int rangeAxisIndex2 = plot.getRangeAxisIndex(rangeAxis);
            this.updateCrosshairValues(crosshairState, x1, y1, domainAxisIndex2, rangeAxisIndex2, transX1, transY1, orientation1);
            EntityCollection entities1 = state.getEntityCollection();
            if(entities1 != null) {
                this.addEntity(entities1, hotspot, dataset, series, item, 0.0D, 0.0D);
            }

        }
    }

    protected abstract boolean getPlotShapes(final int seriesNum);

    protected abstract boolean getPlotLines(final int seriesNum);

    static class XYAreaRendererState extends XYItemRendererState {
        public GeneralPath area = new GeneralPath();
        public Line2D line = new Line2D.Double();

        XYAreaRendererState(PlotRenderingInfo info) {
            super(info);
        }
    }
}
