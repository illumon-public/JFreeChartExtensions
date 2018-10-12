package com.jfreechartextensions.renderers;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.data.DataUtilities;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;

/**
 * StackedAreaRendered that supports AxisTransformations
 */
public class ExtendedStackedAreaRenderer extends StackedAreaRenderer {

    /**
     * Stores the index of last visible point for every series
     */
    private boolean[][] visibles;

    /**
     * Data structure to store stack mid values for left polygon
     */
    private double[] stackLeftMid = new double[2];

    /**
     * Data structure to store stack mid values for right polygon
     */
    private double[] stackRightMid = new double[2];

    @Override
    public CategoryItemRendererState initialise(Graphics2D g2,
                                                Rectangle2D dataArea, CategoryPlot plot, int rendererIndex,
                                                PlotRenderingInfo info) {
        final CategoryItemRendererState state = super.initialise(g2, dataArea, plot, rendererIndex, info);
        visibles = new boolean[getRowCount()][];
        for (int i = 0; i < getRowCount(); i++) {
            visibles[i] = new boolean[getColumnCount()];
        }

        return state;
    }

    @Override
    public void drawItem(Graphics2D g2, CategoryItemRendererState state,
                         Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis,
                         ValueAxis rangeAxis, CategoryDataset dataset, int row, int column,
                         int pass) {

        if (row == 0 && column == 0) {
            visibles = new boolean[getRowCount()][getColumnCount()];
            stackLeftMid = new double[2];
            stackRightMid = new double[2];
        }

        if (!isSeriesVisible(row)) {
            return;
        }

        // setup for collecting optional entity info...
        Shape entityArea;
        EntityCollection entities = state.getEntityCollection();
        RectangleEdge edge1 = plot.getRangeAxisEdge();

        double y1 = 0.0;
        Number n = dataset.getValue(row, column);
        if (n != null && Double.isFinite(n.doubleValue())) {
            y1 = n.doubleValue();
            if (this.getRenderAsPercentages()) {
                double total = DataUtilities.calculateColumnTotal(dataset,
                        column, state.getVisibleSeriesArray());
                y1 = y1 / total;
            }
        }

        double tY1 = rangeAxis.valueToJava2D(y1, dataArea, edge1);

        if (Double.isFinite(tY1)) {
            visibles[row][column] = true;
        }

        double[] stack1 = getStackValues(dataset, row, column,
                state.getVisibleSeriesArray(), visibles);


        // leave the y values (y1, y0) untranslated as it is going to be be
        // stacked up later by previous series values, after this it will be
        // translated.
        double xx1 = domainAxis.getCategoryMiddle(column, getColumnCount(),
                dataArea, plot.getDomainAxisEdge());


        // get the previous point and the next point so we can calculate a
        // "hot spot" for the area (used by the chart entity)...
        double y0 = 0.0;
        int prevColumn = Math.max(column - 1, 0);
        n = dataset.getValue(row, prevColumn);
        if (n != null && Double.isFinite(n.doubleValue())) {
            y0 = n.doubleValue();
            if (this.getRenderAsPercentages()) {
                double total = DataUtilities.calculateColumnTotal(dataset, prevColumn, state.getVisibleSeriesArray());
                y0 = y0 / total;
            }
        }

        double tY0 = rangeAxis.valueToJava2D(y0, dataArea, edge1);
        boolean previousPointVisible = false;
        if (Double.isFinite(tY0)) {
            previousPointVisible = true;
        }
        double[] stack0 = getStackValues(dataset, row, prevColumn,
                state.getVisibleSeriesArray(), visibles);

        // FIXME: calculate xx0
        double xx0 = domainAxis.getCategoryStart(column, getColumnCount(),
                dataArea, plot.getDomainAxisEdge());

        int itemCount = dataset.getColumnCount();

        double y2 = 0.0;
        int nextColumn = Math.min(column + 1, itemCount - 1);
        n = dataset.getValue(row, nextColumn);
        if (n != null && Double.isFinite(n.doubleValue())) {
            y2 = n.doubleValue();
            if (this.getRenderAsPercentages()) {
                double total = DataUtilities.calculateColumnTotal(dataset,
                        nextColumn,
                        state.getVisibleSeriesArray());
                y2 = y2 / total;
            }
        }

        double tY2 = rangeAxis.valueToJava2D(y2, dataArea, edge1);
        boolean nextPointVisible = false;

        //update the visibility for the next point in the series
        if (Double.isFinite(tY2)) {
            visibles[row][nextColumn] = true;
            nextPointVisible = true;
        }
        double[] stack2 = getStackValues(dataset, row, nextColumn, state.getVisibleSeriesArray(), visibles);

        double xx2 = domainAxis.getCategoryEnd(column, getColumnCount(),
                dataArea, plot.getDomainAxisEdge());

        // FIXME: calculate xxLeft and xxRight
        double xxLeft = xx0;
        double xxRight = xx2;

        double[] stackLeft = averageStackValues(stack0, stack1);
        double[] stackRight = averageStackValues(stack1, stack2);
        double[] adjStackLeft = adjustedStackValues(stack0, stack1);
        double[] adjStackRight = adjustedStackValues(stack1, stack2);

        double transY1;

        GeneralPath left = new GeneralPath();
        GeneralPath right = new GeneralPath();

        final PlotOrientation orientation = plot.getOrientation();
        if (y1 >= 0.0) {  // handle positive value
            transY1 = rangeAxis.valueToJava2D(y1 + stack1[1], dataArea, edge1);

            //If the stack[1] is not visible (mostly for LOG transformation), then use the range min
            double transStack1 = rangeAxis.valueToJava2D(stack1[1], dataArea, edge1);
            transStack1 = Double.isFinite(transStack1) ? transStack1 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

            //stack-value for left half is previous series' yleft, which is stored in stackMidLeft
            double transStackLeft = rangeAxis.valueToJava2D(stackLeftMid[1], dataArea, edge1);
            transStackLeft = Double.isFinite(transStackLeft) ? transStackLeft : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

            // LEFT POLYGON
            double yleft = !previousPointVisible ? y1 / 2 + stackLeft[1] : (y0 + y1) / 2.0 + stackLeft[1];

            double transYLeft = rangeAxis.valueToJava2D(yleft, dataArea, edge1);
            if (y0 >= 0.0) {
                if (orientation == PlotOrientation.VERTICAL) {
                    left.moveTo(xx1, transY1);
                    left.lineTo(xx1, transStack1);
                    left.lineTo(xxLeft, transStackLeft);
                    left.lineTo(xxLeft, transYLeft);
                } else {
                    left.moveTo(transY1, xx1);
                    left.lineTo(transStack1, xx1);
                    left.lineTo(transStackLeft, xxLeft);
                    left.lineTo(transYLeft, xxLeft);
                }
                left.closePath();
                //update stack values whenever yleft is used.
                if (yleft >= 0) {
                    stackLeftMid[1] = yleft;
                } else {
                    stackLeftMid[0] = yleft;
                }
            } else {
                if (orientation == PlotOrientation.VERTICAL) {
                    left.moveTo(xx1, transStack1);
                    left.lineTo(xx1, transY1);
                    if (!previousPointVisible) {
                        left.lineTo(xxLeft, transYLeft);
                        //update stack values whenever yleft is used.
                        if (yleft >= 0) {
                            stackLeftMid[1] = yleft;
                        } else {
                            stackLeftMid[0] = yleft;
                        }
                    }
                    left.lineTo(xxLeft, transStackLeft);
                } else {
                    left.moveTo(transStack1, xx1);
                    left.lineTo(transY1, xx1);
                    if (!previousPointVisible) {
                        left.lineTo(transYLeft, xxLeft);
                        //update stack values whenever yleft is used.
                        if (yleft >= 0) {
                            stackLeftMid[1] = yleft;
                        } else {
                            stackLeftMid[0] = yleft;
                        }
                    }
                    left.lineTo(transStackLeft, xxLeft);
                }
                left.closePath();
            }

            //stack-value for right half is previous series' yright, which is stored in stackMidRight
            double transStackRight = rangeAxis.valueToJava2D(stackRightMid[1], dataArea, edge1);
            transStackRight = Double.isFinite(transStackRight) ? transStackRight : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

            double yright = !nextPointVisible ? y1 / 2 + stackRight[1] : (y1 + y2) / 2.0 + stackRight[1];
            double transYRight = rangeAxis.valueToJava2D(yright, dataArea, edge1);

            // RIGHT POLYGON
            if (y2 >= 0.0) {
                if (orientation == PlotOrientation.VERTICAL) {
                    right.moveTo(xx1, transStack1);
                    right.lineTo(xx1, transY1);
                    right.lineTo(xxRight, transYRight);
                    right.lineTo(xxRight, transStackRight);
                } else {
                    right.moveTo(transStack1, xx1);
                    right.lineTo(transY1, xx1);
                    right.lineTo(transYRight, xxRight);
                    right.lineTo(transStackRight, xxRight);
                }
                right.closePath();
                //update stack values whenever yleft is used.
                if (yright >= 0) {
                    stackRightMid[1] = yright;
                } else {
                    stackRightMid[0] = yright;
                }

            } else {
                if (orientation == PlotOrientation.VERTICAL) {
                    right.moveTo(xx1, transStack1);
                    right.lineTo(xx1, transY1);
                    if (!nextPointVisible) {
                        right.lineTo(xxRight, transYRight);
                        //update stack values whenever yleft is used.
                        if (yright >= 0) {
                            stackRightMid[1] = yright;
                        } else {
                            stackRightMid[0] = yright;
                        }
                    }
                    right.lineTo(xxRight, transStackRight);
                } else {
                    right.moveTo(transStack1, xx1);
                    right.lineTo(transY1, xx1);
                    if (!nextPointVisible) {
                        right.lineTo(transYRight, xxRight);
                        //update stack values whenever yleft is used.
                        if (yright >= 0) {
                            stackRightMid[1] = yright;
                        } else {
                            stackRightMid[0] = yright;
                        }
                    }
                    right.lineTo(transStackRight, xxRight);
                }
                right.closePath();
            }

        } else {  // handle negative value
            transY1 = rangeAxis.valueToJava2D(y1 + stack1[0], dataArea, edge1);
            double transStack1 = rangeAxis.valueToJava2D(stack1[0], dataArea, edge1);
            transStack1 = Double.isFinite(transStack1) ? transStack1 : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

            //stack-value for right half is previous series' yleft, which is stored in stackMidRight
            double transStackLeft = rangeAxis.valueToJava2D(stackLeftMid[0], dataArea, edge1);
            transStackLeft = Double.isFinite(transStackLeft) ? transStackLeft : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

            double yleft = !previousPointVisible ? y1 / 2 + stackLeft[0] : (y0 + y1) / 2.0 + stackLeft[0];
            double transYLeft = rangeAxis.valueToJava2D(yleft,
                    dataArea, edge1);
            // LEFT POLYGON
            if (y0 >= 0.0) {
                if (orientation == PlotOrientation.VERTICAL) {
                    left.moveTo(xx1, transStack1);
                    left.lineTo(xx1, transY1);
                    if (!previousPointVisible) {
                        left.lineTo(xxLeft, transYLeft);
                        //update stack values whenever yleft is used.
                        if (yleft >= 0) {
                            stackLeftMid[1] = yleft;
                        } else {
                            stackLeftMid[0] = yleft;
                        }
                    }
                    left.lineTo(xxLeft, transStackLeft);
                } else {
                    left.moveTo(transStack1, xx1);
                    left.lineTo(transY1, xx1);
                    if (!previousPointVisible) {
                        left.lineTo(transYLeft, xxLeft);
                        //update stack values whenever yleft is used.
                        if (yleft >= 0) {
                            stackLeftMid[1] = yleft;
                        } else {
                            stackLeftMid[0] = yleft;
                        }
                    }
                    left.lineTo(transStackLeft, xxLeft);
                }
                left.clone();
            } else {
                if (orientation == PlotOrientation.VERTICAL) {
                    left.moveTo(xx1, transY1);
                    left.lineTo(xx1, transStack1);
                    left.lineTo(xxLeft, transStackLeft);
                    left.lineTo(xxLeft, transYLeft);
                } else {
                    left.moveTo(transY1, xx1);
                    left.lineTo(transStack1, xx1);
                    left.lineTo(transStackLeft, xxLeft);
                    left.lineTo(transYLeft, xxLeft);
                }
                left.closePath();
                //update stack values whenever yleft is used.
                if (yleft >= 0) {
                    stackLeftMid[1] = yleft;
                } else {
                    stackLeftMid[0] = yleft;
                }
            }

            //stack-value for right half is previous series' yright, which is stored in stackMidRight
            double transStackRight = rangeAxis.valueToJava2D(stackRightMid[0], dataArea, edge1);
            transStackRight = Double.isFinite(transStackRight) ? transStackRight : rangeAxis.valueToJava2D(rangeAxis.getLowerBound(), dataArea, edge1);

            double yright = !nextPointVisible ? y1 / 2 + stackRight[0] : (y1 + y2) / 2.0 + stackRight[0];
            double transYRight = rangeAxis.valueToJava2D(yright, dataArea, edge1);


            // RIGHT POLYGON
            if (y2 >= 0.0) {
                if (orientation == PlotOrientation.VERTICAL) {
                    right.moveTo(xx1, transStack1);
                    right.lineTo(xx1, transY1);
                    if (!nextPointVisible) {
                        right.lineTo(xxRight, transYRight);
                        //update stack values whenever yright is used.
                        if (yright >= 0) {
                            stackRightMid[1] = yright;
                        } else {
                            stackRightMid[0] = yright;
                        }
                    }
                    right.lineTo(xxRight, transStackRight);
                } else {
                    right.moveTo(transStack1, xx1);
                    right.lineTo(transY1, xx1);
                    if (!nextPointVisible) {
                        right.lineTo(transYRight, xxRight);
                        //update stack values whenever yright is used.
                        if (yright >= 0) {
                            stackRightMid[1] = yright;
                        } else {
                            stackRightMid[0] = yright;
                        }
                    }
                    right.lineTo(transStackRight, xxRight);
                }
                right.closePath();
            } else {
                if (orientation == PlotOrientation.VERTICAL) {
                    right.moveTo(xx1, transStack1);
                    right.lineTo(xx1, transY1);
                    right.lineTo(xxRight, transYRight);
                    right.lineTo(xxRight, transStackRight);
                } else {
                    right.moveTo(transStack1, xx1);
                    right.lineTo(transY1, xx1);
                    right.lineTo(transYRight, xxRight);
                    right.lineTo(transStackRight, xxRight);
                }
                right.closePath();
                //update stack values whenever yright is used.
                if (yright >= 0) {
                    stackRightMid[1] = yright;
                } else {
                    stackRightMid[0] = yright;
                }
            }
        }

        //Reset data structures
        if (row == getRowCount() - 1) {
            stackRightMid = new double[2];
            stackLeftMid = new double[2];
        }

        if (pass == 0) {
            Paint itemPaint = getItemPaint(row, column);
            g2.setPaint(itemPaint);
            g2.fill(left);
            g2.fill(right);

            // add an entity for the item...
            if (entities != null) {
                GeneralPath gp = new GeneralPath(left);
                gp.append(right, false);
                entityArea = gp;
                addItemEntity(entities, dataset, row, column, entityArea);
            }
        } else if (pass == 1) {
            drawItemLabel(g2, orientation, dataset, row, column,
                    xx1, transY1, y1 < 0.0);
        }
    }

    /**
     * Returns a pair of "stack" values calculated as the mean of the two
     * specified stack value pairs.
     *
     * @param stack1 the first stack pair.
     * @param stack2 the second stack pair.
     * @return A pair of average stack values.
     */
    private double[] averageStackValues(double[] stack1, double[] stack2) {
        double[] result = new double[2];
        result[0] = (stack1[0] + stack2[0]) / 2.0;
        result[1] = (stack1[1] + stack2[1]) / 2.0;
        return result;
    }

    /**
     * Calculates adjusted stack values from the supplied values.  The value is
     * the mean of the supplied values, unless either of the supplied values
     * is zero, in which case the adjusted value is zero also.
     *
     * @param stack1 the first stack pair.
     * @param stack2 the second stack pair.
     * @return A pair of average stack values.
     */
    private double[] adjustedStackValues(double[] stack1, double[] stack2) {
        double[] result = new double[2];
        if (stack1[0] == 0.0 || stack2[0] == 0.0) {
            result[0] = 0.0;
        } else {
            result[0] = (stack1[0] + stack2[0]) / 2.0;
        }
        if (stack1[1] == 0.0 || stack2[1] == 0.0) {
            result[1] = 0.0;
        } else {
            result[1] = (stack1[1] + stack2[1]) / 2.0;
        }
        return result;
    }

    private double[] getStackValues(CategoryDataset dataset, int series, int index, int[] validRows, boolean[][] visibles) {
        double[] result = new double[2];
        double total = 0.0;
        if (this.getRenderAsPercentages()) {
            total = DataUtilities.calculateColumnTotal(dataset, index, validRows);
        }
        for (int i = 0; i < series; i++) {
            if (isSeriesVisible(i) && visibles[i][index]) {
                double v = 0.0;
                Number n = dataset.getValue(i, index);
                if (n != null) {
                    v = n.doubleValue();
                    if (this.getRenderAsPercentages()) {
                        v = v / total;
                    }
                }
                if (!Double.isNaN(v)) {
                    if (v >= 0.0) {
                        result[1] += v;
                    } else {
                        result[0] += v;
                    }
                }
            }
        }
        return result;
    }
}
