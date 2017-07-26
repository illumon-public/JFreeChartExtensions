package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.util.ShapeUtilities;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.Stack;

public abstract class ExtendedXYStackedAreaRenderer extends StackedXYAreaRenderer {

    @Override
    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset data, PlotRenderingInfo info) {
        StackedXYAreaRendererState state = new StackedXYAreaRendererState(info);
        state.setProcessVisibleItemsOnly(false);
        return state;
    }

    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
        PlotOrientation orientation = plot.getOrientation();
        //StackedXYAreaRenderer.StackedXYAreaRendererState areaState = (StackedXYAreaRenderer.StackedXYAreaRendererState)state;
        StackedXYAreaRendererState areaState = (StackedXYAreaRendererState)state;
        TableXYDataset tdataset = (TableXYDataset)dataset;
        int itemCount = tdataset.getItemCount();
        double x1 = dataset.getXValue(series, item);
        double y1 = dataset.getYValue(series, item);
        boolean nullPoint = false;
        if(Double.isNaN(y1)) {
            y1 = 0.0D;
            nullPoint = true;
        }

        double ph1 = this.getPreviousHeight(tdataset, series, item);
        double transX1 = domainAxis.valueToJava2D(x1, dataArea, plot.getDomainAxisEdge());
        double transY1 = rangeAxis.valueToJava2D(y1 + ph1, dataArea, plot.getRangeAxisEdge());
        Paint seriesPaint = this.getItemPaint(series, item);
        Paint seriesFillPaint = seriesPaint;
        if(this.getUseFillPaint()) {
            seriesFillPaint = this.getItemFillPaint(series, item);
        }

        Stroke seriesStroke = this.getItemStroke(series, item);
        if(pass == 0) {
            double shape;
            if(item == 0) {
                areaState.setSeriesArea(new Polygon());
                areaState.setLastSeriesPoints(areaState.getCurrentSeriesPoints());
                areaState.setCurrentSeriesPoints(new Stack());
                shape = rangeAxis.valueToJava2D(ph1, dataArea, plot.getRangeAxisEdge());
                if(orientation == PlotOrientation.VERTICAL) {
                    areaState.getSeriesArea().addPoint((int)transX1, (int)shape);
                } else if(orientation == PlotOrientation.HORIZONTAL) {
                    areaState.getSeriesArea().addPoint((int)shape, (int)transX1);
                }
            }

            if(orientation == PlotOrientation.VERTICAL) {
                Point shape1 = new Point((int)transX1, (int)transY1);
                areaState.getSeriesArea().addPoint((int)shape1.getX(), (int)shape1.getY());
                areaState.getCurrentSeriesPoints().push(shape1);
            } else if(orientation == PlotOrientation.HORIZONTAL) {
                areaState.getSeriesArea().addPoint((int)transY1, (int)transX1);
            }

            //if(this.getPlotLines() && item > 0) {
            if(this.getPlotLines(series) && item > 0) {
                shape = dataset.getXValue(series, item - 1);
                double tip = dataset.getYValue(series, item - 1);
                double url = this.getPreviousHeight(tdataset, series, item - 1);
                double transX0 = domainAxis.valueToJava2D(shape, dataArea, plot.getDomainAxisEdge());
                double transY0 = rangeAxis.valueToJava2D(tip + url, dataArea, plot.getRangeAxisEdge());
                if(orientation == PlotOrientation.VERTICAL) {
                    areaState.getLine().setLine(transX0, transY0, transX1, transY1);
                } else if(orientation == PlotOrientation.HORIZONTAL) {
                    areaState.getLine().setLine(transY0, transX0, transY1, transX1);
                }

                g2.setPaint(seriesPaint);
                g2.setStroke(seriesStroke);
                g2.draw(areaState.getLine());
            }

            if(this.getPlotArea() && item > 0 && item == itemCount - 1) {
                shape = rangeAxis.valueToJava2D(ph1, dataArea, plot.getRangeAxisEdge());
                if(orientation == PlotOrientation.VERTICAL) {
                    areaState.getSeriesArea().addPoint((int)transX1, (int)shape);
                } else if(orientation == PlotOrientation.HORIZONTAL) {
                    areaState.getSeriesArea().addPoint((int)shape, (int)transX1);
                }

                if(series != 0) {
                    Stack tip1 = areaState.getLastSeriesPoints();

                    while(!tip1.empty()) {
                        Point generator = (Point)tip1.pop();
                        areaState.getSeriesArea().addPoint((int)generator.getX(), (int)generator.getY());
                    }
                }

                g2.setPaint(seriesFillPaint);
                g2.setStroke(seriesStroke);
                g2.fill(areaState.getSeriesArea());
                if(this.isOutline()) {
                    g2.setStroke(this.lookupSeriesOutlineStroke(series));
                    g2.setPaint(this.lookupSeriesOutlinePaint(series));
                    g2.draw(areaState.getSeriesArea());
                }
            }

            int shape2 = plot.getDomainAxisIndex(domainAxis);
            int entities = plot.getRangeAxisIndex(rangeAxis);
            this.updateCrosshairValues(crosshairState, x1, ph1 + y1, shape2, entities, transX1, transY1, orientation);
        } else if(pass == 1) {
            Object shape3 = null;
            //if(this.getPlotShapes()) {
            if(this.getPlotShapes(series)) {
                shape3 = this.getItemShape(series, item);
                if(plot.getOrientation() == PlotOrientation.VERTICAL) {
                    shape3 = ShapeUtilities.createTranslatedShape((Shape)shape3, transX1, transY1);
                } else if(plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                    shape3 = ShapeUtilities.createTranslatedShape((Shape)shape3, transY1, transX1);
                }

                if(!nullPoint) {
                    if(this.getShapePaint() != null) {
                        g2.setPaint(this.getShapePaint());
                    } else {
                        g2.setPaint(seriesPaint);
                    }

                    if(this.getShapeStroke() != null) {
                        g2.setStroke(this.getShapeStroke());
                    } else {
                        g2.setStroke(seriesStroke);
                    }

                    g2.draw((Shape)shape3);
                }
            } else if(plot.getOrientation() == PlotOrientation.VERTICAL) {
                shape3 = new Rectangle2D.Double(transX1 - 3.0D, transY1 - 3.0D, 6.0D, 6.0D);
            } else if(plot.getOrientation() == PlotOrientation.HORIZONTAL) {
                shape3 = new Rectangle2D.Double(transY1 - 3.0D, transX1 - 3.0D, 6.0D, 6.0D);
            }

            if(state.getInfo() != null) {
                EntityCollection entities1 = state.getEntityCollection();
                if(entities1 != null && shape3 != null && !nullPoint) {
                    String tip2 = null;
                    XYToolTipGenerator generator1 = this.getToolTipGenerator(series, item);
                    if(generator1 != null) {
                        tip2 = generator1.generateToolTip(dataset, series, item);
                    }

                    String url1 = null;
                    if(this.getURLGenerator() != null) {
                        url1 = this.getURLGenerator().generateURL(dataset, series, item);
                    }

                    XYItemEntity entity = new XYItemEntity((Shape)shape3, dataset, series, item, tip2, url1);
                    entities1.add(entity);
                }
            }
        }

    }

    protected abstract boolean getPlotShapes(final int seriesNum);

    protected abstract boolean getPlotLines(final int seriesNum);

    protected static class StackedXYAreaRendererState extends XYItemRendererState {
        private Polygon seriesArea = null;
        private Line2D line = new Line2D.Double();
        private Stack lastSeriesPoints = new Stack();
        private Stack currentSeriesPoints = new Stack();

        public StackedXYAreaRendererState(PlotRenderingInfo info) {
            super(info);
        }

        public Polygon getSeriesArea() {
            return this.seriesArea;
        }

        public void setSeriesArea(Polygon area) {
            this.seriesArea = area;
        }

        public Line2D getLine() {
            return this.line;
        }

        public Stack getCurrentSeriesPoints() {
            return this.currentSeriesPoints;
        }

        public void setCurrentSeriesPoints(Stack points) {
            this.currentSeriesPoints = points;
        }

        public Stack getLastSeriesPoints() {
            return this.lastSeriesPoints;
        }

        public void setLastSeriesPoints(Stack points) {
            this.lastSeriesPoints = points;
        }
    }

}
