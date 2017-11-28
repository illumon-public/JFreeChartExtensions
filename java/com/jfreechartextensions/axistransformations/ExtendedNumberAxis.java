package com.jfreechartextensions.axistransformations;

import org.jfree.chart.axis.*;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.ValueAxisPlot;
import org.jfree.data.Range;
import org.jfree.data.RangeType;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public abstract class ExtendedNumberAxis extends NumberAxis {

    private static final long serialVersionUID = 3021139144251111579L;
    private BasicAxisTransform transform;
    private double[] majorTicks;
    private double tickAngle = Double.NaN;
    private double minRange = Double.NaN;
    private double maxRange = Double.NaN;
    private static final double SMALLEST_DOUBLE = 1E-11;

    public ExtendedNumberAxis(final BasicAxisTransform transform) {
        this.transform = transform;
    }

    protected abstract BasicAxisTransform newNullAxisTransform();

    public void setTransform(BasicAxisTransform transform) {
        this.transform = transform == null ? newNullAxisTransform() : transform;
    }

    public void setMajorTicks(final double[] tickLocations) {
        if(tickLocations == null || tickLocations.length == 0){
            this.majorTicks = null;
            return;
        }

        this.majorTicks = tickLocations.clone();
        Arrays.sort(tickLocations);
    }

    public void setTickLabelAngle(final double angle) {
        this.tickAngle = Double.isInfinite(angle) ? Double.NaN : angle * Math.PI /180 % (2 * Math.PI);
    }

    @Override
    public void setLowerBound(final double min) {
        if(maxRange <= min) { throw new IllegalArgumentException("Lower bound=" + min + " can't be smaller than upper bound=" + maxRange); }
        this.minRange = min;
        if(!Double.isNaN(minRange) && !Double.isNaN(maxRange)) {
            this.setRange(new Range(minRange, maxRange));
        }
    }

    @Override
    public void setUpperBound(final double max) {
        if(minRange >= max) { throw new IllegalArgumentException("Upper bound=" + max + " can't be smaller than lower bound=" + minRange); }
        this.maxRange = max;
        if(!Double.isNaN(minRange) && !Double.isNaN(maxRange)) {
            this.setRange(new Range(minRange, maxRange));
        }
    }

    @Override
    public Range getRange() {
        final Range r = super.getRange();
        if(!Double.isNaN(minRange) && !Double.isNaN(maxRange)) {
            return new Range(minRange, maxRange);
        } else if(!Double.isNaN(minRange)) {
            if(minRange > r.getUpperBound()) {
                return new Range(minRange, minRange + 1);
            }
        } else if(!Double.isNaN(maxRange)){
            if(maxRange < r.getLowerBound()) {
                return new Range(maxRange - 1, maxRange);
            }
        }

        return r;
    }

    @Override
    public java.util.List refreshTicks(Graphics2D g2, AxisState state, Rectangle2D dataArea, RectangleEdge edge) {
        g2.setFont(getTickLabelFont());
        if(isAutoTickUnitSelection()) {
            selectAutoTickUnit(g2, dataArea, edge);
        }

        final NumberTickUnit tu = getTickUnit();
        final double majorTickSize = majorTicks == null ? tu.getSize() : Double.NaN;
        final double lowestMajorTickValue = majorTicks == null ? calculateLowestVisibleTickValue() : majorTicks[0];

        final Function<Integer, Double> majorTickCalculator = majorTicks == null ?
                i -> lowestMajorTickValue + i * majorTickSize:
                i -> i<0 || i>= majorTicks.length ? Double.NaN : majorTicks[i];

        final int nMajorTicks = majorTicks == null ? calculateVisibleTickCount() : majorTicks.length;
        final ArrayList<NumberTick> result = new ArrayList<>();

        if (nMajorTicks > 500) {
            return result;
        }

        final int nMinorTicks = getMinorTickCount() <= 0 ? tu.getMinorTickCount() : getMinorTickCount();

        addMinorTicks(result, nMinorTicks, majorTickCalculator.apply(-1), majorTickCalculator.apply(0),edge);

        for (int i = 0; i < nMajorTicks; ++i) {
            final double majorTickValue = majorTickCalculator.apply(i);
            final String tickLabel = formatLabel(majorTickValue);

            if(okToPlotTick(majorTickValue)) {
                result.add(createTick(TickType.MAJOR, edge, majorTickValue, tickLabel));
            }

            final double nextMajorTickValue = majorTickCalculator.apply(i + 1);
            addMinorTicks(result, nMinorTicks, majorTickValue, nextMajorTickValue,edge);
        }

        addMinorTicks(result, nMinorTicks, majorTickCalculator.apply(nMajorTicks-1), majorTickCalculator.apply(nMajorTicks),edge);

        return result;
    }

    @Override
    protected void selectAutoTickUnit(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
        //multiple passes makes the auto tick unit more accurate
        for(int i = 0; i < 3; i++) {
            double size1 = getTickUnit().getSize();
            selectAutoTickUnitAux(g2, dataArea, edge);
            if (size1 == getTickUnit().getSize()) {
                return;
            }
        }
    }

    private void selectAutoTickUnitAux(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
        if(RectangleEdge.isTopOrBottom(edge)) {
            this.selectHorizontalAutoTickUnit(g2, dataArea, edge);
        } else if(RectangleEdge.isLeftOrRight(edge)) {
            this.selectVerticalAutoTickUnit(g2, dataArea, edge);
            this.selectVerticalAutoTickUnit(g2, dataArea, edge);
        }
    }

    @Override
    protected void selectVerticalAutoTickUnit(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
        double tickLabelHeight = this.estimateMaximumTickLabelHeight(g2);
        TickUnitSource tickUnits = this.getStandardTickUnits();
        TickUnit unit1 = tickUnits.getCeilingTickUnit(this.getTickUnit());

        //Smallest tick distance could be at either end
        final double unitHeightUpper = Math.abs(this.lengthToJava2D(getRange().getUpperBound(), dataArea, edge) - this.lengthToJava2D(getRange().getUpperBound() - unit1.getSize(), dataArea, edge));
        final double unitHeightLower = Math.abs(this.lengthToJava2D(getRange().getLowerBound(), dataArea, edge) - this.lengthToJava2D(getRange().getLowerBound() + unit1.getSize(), dataArea, edge));
        double unitHeight = Math.min(unitHeightLower, unitHeightUpper);

        double guess = unit1.getSize();
        if(unitHeight > 0.0D) {
            guess = tickLabelHeight / unitHeight * unit1.getSize();
        }

        NumberTickUnit unit2 = (NumberTickUnit)tickUnits.getCeilingTickUnit(guess);

        final double unit2HeightUpper = Math.abs(this.lengthToJava2D(getRange().getUpperBound(), dataArea, edge) - this.lengthToJava2D(getRange().getUpperBound() - unit2.getSize(), dataArea, edge));
        final double unit2HeightLower = Math.abs(this.lengthToJava2D(getRange().getLowerBound(), dataArea, edge) - this.lengthToJava2D(getRange().getLowerBound() + unit2.getSize(), dataArea, edge));
        final double unit2Height = Math.min(unit2HeightLower, unit2HeightUpper);

        tickLabelHeight = this.estimateMaximumTickLabelHeight(g2);

        if(tickLabelHeight > unit2Height) {
            unit2 = (NumberTickUnit)tickUnits.getLargerTickUnit(unit2);
        }

        this.setTickUnit(unit2, false, false);
    }

    @Override
    protected void selectHorizontalAutoTickUnit(Graphics2D g2, Rectangle2D dataArea, RectangleEdge edge) {
        double tickLabelWidth = this.estimateMaximumTickLabelWidth(g2, this.getTickUnit());
        TickUnitSource tickUnits = this.getStandardTickUnits();
        TickUnit unit1 = tickUnits.getCeilingTickUnit(this.getTickUnit());

        //if transform
        final double unit1WidthUpper = Math.abs(this.lengthToJava2D(getRange().getUpperBound(), dataArea, edge) - this.lengthToJava2D(getRange().getUpperBound() - unit1.getSize(), dataArea, edge));
        final double unit1WidthLower = Math.abs(this.lengthToJava2D(getRange().getLowerBound(), dataArea, edge) - this.lengthToJava2D(getRange().getLowerBound() + unit1.getSize(), dataArea, edge));
        double unit1Width = Math.min(unit1WidthLower, unit1WidthUpper);
        if(Math.abs(unit1Width) < SMALLEST_DOUBLE) {
            unit1Width = SMALLEST_DOUBLE;
        }
        final double guess = tickLabelWidth / unit1Width * unit1.getSize();
        NumberTickUnit unit2 = (NumberTickUnit)tickUnits.getCeilingTickUnit(guess);

        final double unit2WidthUpper = Math.abs(this.lengthToJava2D(getRange().getUpperBound(), dataArea, edge) - this.lengthToJava2D(getRange().getUpperBound() - unit2.getSize(), dataArea, edge));
        final double unit2WidthLower = Math.abs(this.lengthToJava2D(getRange().getLowerBound(), dataArea, edge) - this.lengthToJava2D(getRange().getLowerBound() + unit2.getSize(), dataArea, edge));
        final double unit2Width = Math.min(unit2WidthLower, unit2WidthUpper);

        tickLabelWidth = this.estimateMaximumTickLabelWidth(g2, unit2);
        if(tickLabelWidth > unit2Width) {
            unit2 = (NumberTickUnit)tickUnits.getLargerTickUnit(unit2);
        }

        this.setTickUnit(unit2, false, false);
    }

    private void addMinorTicks(final Collection<NumberTick> result, final int nMinorTicks, final double majorTickMin, final double majorTickMax, RectangleEdge edge){
        final double dx = (majorTickMax-majorTickMin)/nMinorTicks;

        for (int i = 1; i < nMinorTicks; ++i) {
            final double minorTickValue = majorTickMin + i*dx;

            if (okToPlotTick(minorTickValue)) {
                result.add(createTick(TickType.MINOR, edge, minorTickValue, ""));
            }
        }
    }

    private NumberTick createTick(final TickType tickType, final RectangleEdge edge, final double currentTickValue, final String tickLabel) {
        final boolean isHorizontal = RectangleEdge.isTopOrBottom(edge);

        if(isHorizontal) {
            double angle = Double.isNaN(tickAngle) ? 0.0 : tickAngle;
            TextAnchor anchor;
            TextAnchor rotationAnchor;
            if (edge == RectangleEdge.TOP) {
                if (angle < Math.PI / 3) {
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                } else if (angle < 2 * Math.PI / 3) {
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                } else if (angle < Math.PI) {
                    anchor = TextAnchor.TOP_CENTER;
                    rotationAnchor = TextAnchor.TOP_CENTER;
                } else if (angle < 4 * Math.PI / 3) {
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                } else if (angle < 5 * Math.PI / 3) {
                    anchor = TextAnchor.CENTER_LEFT;
                    rotationAnchor = TextAnchor.CENTER_LEFT;
                } else {
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                }
            } else {
                if (angle < Math.PI / 3) {
                    anchor = TextAnchor.TOP_CENTER;
                    rotationAnchor = TextAnchor.TOP_CENTER;
                } else if (angle < 2 * Math.PI / 3) {
                    anchor = TextAnchor.CENTER_LEFT;
                    rotationAnchor = TextAnchor.CENTER_LEFT;
                } else if (angle < Math.PI) {
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                } else if (angle < 4 * Math.PI / 3) {
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                } else if (angle < 5 * Math.PI / 3) {
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                } else {
                    anchor = TextAnchor.TOP_CENTER;
                    rotationAnchor = TextAnchor.TOP_CENTER;
                }
            }

            return new NumberTick(currentTickValue, tickLabel, anchor, rotationAnchor, angle);
        } else {
            double angle = Double.isNaN(tickAngle) ? 0.0 : tickAngle;
            TextAnchor anchor;
            TextAnchor rotationAnchor;
            if(edge == RectangleEdge.LEFT) {
                if (angle < Math.PI / 3) { //60
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                } else if (angle < 2 * Math.PI / 3) { //120
                    anchor = TextAnchor.TOP_CENTER;
                    rotationAnchor = TextAnchor.TOP_CENTER;
                } else if (angle < Math.PI) { //180
                    anchor = TextAnchor.CENTER_LEFT;
                    rotationAnchor = TextAnchor.CENTER_LEFT;
                } else if (angle < 4 * Math.PI / 3) { //240
                    anchor = TextAnchor.CENTER_LEFT;
                    rotationAnchor = TextAnchor.CENTER_LEFT;
                } else if (angle < 5 * Math.PI / 3) { //300
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                } else { //>=300
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                }
            } else {
                if (angle < Math.PI / 3) { //60
                    anchor = TextAnchor.CENTER_LEFT;
                    rotationAnchor = TextAnchor.CENTER_LEFT;
                } else if (angle < 2 * Math.PI / 3) { //120
                    anchor = TextAnchor.BOTTOM_CENTER;
                    rotationAnchor = TextAnchor.BOTTOM_CENTER;
                } else if (angle < Math.PI) { //180
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                } else if (angle < 4 * Math.PI / 3) { //240
                    anchor = TextAnchor.CENTER_RIGHT;
                    rotationAnchor = TextAnchor.CENTER_RIGHT;
                } else if (angle < 5 * Math.PI / 3) { //300
                    anchor = TextAnchor.TOP_CENTER;
                    rotationAnchor = TextAnchor.TOP_CENTER;
                } else { //>=300
                    anchor = TextAnchor.CENTER_LEFT;
                    rotationAnchor = TextAnchor.CENTER_LEFT;
                }
            }

            return new NumberTick(tickType, currentTickValue, tickLabel, anchor, rotationAnchor, angle);
        }
    }

    private String formatLabel(final double tickValue){
        final NumberFormat formatter = getNumberFormatOverride();
        return formatter == null ? getTickUnit().valueToString(tickValue) : formatter.format(tickValue);
    }

    private boolean okToPlotTick(final double tickValue){
        return !Double.isNaN(tickValue) && contains(getRange(), tickValue) && transform.isVisible(tickValue);
    }

    private boolean contains(Range range, double currentTickValue) {
        return currentTickValue >= range.getLowerBound() && currentTickValue <= range.getUpperBound();
    }


    //transform specific redrawing
    protected void autoAdjustRange() {
        final Plot plot = this.getPlot();
        if(plot != null) {
            if(plot instanceof ValueAxisPlot) {
                final ValueAxisPlot vap = (ValueAxisPlot)plot;
                Range r = vap.getDataRange(this);
                if(r == null) {
                    r = this.getDefaultAutoRange();
                }

                if(!Double.isNaN(minRange) && !Double.isNaN(maxRange)) {
                    throw new IllegalStateException("shouldn't happen");
                } else if(!Double.isNaN(minRange)) {
                    if(minRange > r.getUpperBound()) {
                        r = new Range(minRange, minRange + 1);
                    } else {
                        r = new Range(minRange, r.getUpperBound());
                    }
                } else if(!Double.isNaN(maxRange)){
                    if(maxRange < r.getLowerBound()) {
                        r = new Range(maxRange - 1, maxRange);
                    } else {
                        r = new Range(r.getLowerBound(), maxRange);
                    }
                }

                double upper = transform.transform(r.getUpperBound());
                double lower = transform.transform(r.getLowerBound());

                if(this.getRangeType() == RangeType.POSITIVE) {
                    lower = Math.max(0.0D, lower);
                    upper = Math.max(0.0D, upper);
                } else if(this.getRangeType() == RangeType.NEGATIVE) {
                    lower = Math.min(0.0D, lower);
                    upper = Math.min(0.0D, upper);
                }

                if(this.getAutoRangeIncludesZero()) {
                    lower = Math.min(lower, 0.0D);
                    upper = Math.max(upper, 0.0D);
                }

                final double range = upper - lower;
                final double fixedAutoRange = this.getFixedAutoRange();
                if(fixedAutoRange > 0.0D) {
                    lower = upper - fixedAutoRange;
                } else {
                    final double minRange = this.getAutoRangeMinimumSize();
                    if(range < minRange) {
                        final double expand = (minRange - range) / 2.0D;
                        upper += expand;
                        lower -= expand;
                        if(lower == upper) {
                            final double adjust = Math.abs(lower) / 10.0D;
                            lower -= adjust;
                            upper += adjust;
                        }

                        if(this.getRangeType() == RangeType.POSITIVE) {
                            if(lower < 0.0D) {
                                upper -= lower;
                                lower = 0.0D;
                            }
                        } else if(this.getRangeType() == RangeType.NEGATIVE && upper > 0.0D) {
                            lower -= upper;
                            upper = 0.0D;
                        }
                    }

                    if (this.getAutoRangeStickyZero()) {
                        if (upper <= 0.0D) {
                            upper = Math.min(0.0D, upper + this.getUpperMargin() * range);
                        } else {
                            upper += this.getUpperMargin() * range;
                        }

                        if (lower >= 0.0D) {
                            lower = Math.max(0.0D, lower - this.getLowerMargin() * range);
                        } else {
                            lower -= this.getLowerMargin() * range;
                        }
                    } else {
                        upper += Double.isNaN(this.maxRange) ? this.getUpperMargin() * range : 0.0D;
                        lower -= Double.isNaN(this.minRange) ? this.getLowerMargin() * range : 0.0D;
                    }
                }

                upper = this.transform.inverseTransform(upper);
                lower = this.transform.inverseTransform(lower);

                if (!Double.isNaN(upper) && !Double.isNaN(lower)) {
                    this.setRange(new Range(lower, upper), false, false);
                }
            }

        }
    }

    public double valueToJava2D(final double value, final Rectangle2D area, final RectangleEdge edge) {
        final Range range = this.getRange();
        final double tvalue = transform.isVisible(value) ? transform.transform(value) : value;
        final double axisMin = transform.transform(range.getLowerBound());
        final double axisMax = transform.transform(range.getUpperBound());
        double min = 0.0D;
        double max = 0.0D;
        if(RectangleEdge.isTopOrBottom(edge)) {
            min = area.getX();
            max = area.getMaxX();
        } else if(RectangleEdge.isLeftOrRight(edge)) {
            max = area.getMinY();
            min = area.getMaxY();
        }

        return this.isInverted()?max - (tvalue - axisMin) / (axisMax - axisMin) * (max - min):min + (tvalue - axisMin) / (axisMax - axisMin) * (max - min);
    }

    public double java2DToValue(final double java2DValue, final Rectangle2D area, final RectangleEdge edge) {
        final Range range = this.getRange();
        final double axisMin = transform.transform(range.getLowerBound());
        final double axisMax = transform.transform(range.getUpperBound());
        double min = 0.0D;
        double max = 0.0D;
        if(RectangleEdge.isTopOrBottom(edge)) {
            min = area.getX();
            max = area.getMaxX();
        } else if(RectangleEdge.isLeftOrRight(edge)) {
            min = area.getMaxY();
            max = area.getY();
        }

        double result;
        if(this.isInverted()) {
            result = axisMax - (java2DValue - min) / (max - min) * (axisMax - axisMin);
        } else {
            result = axisMin + (java2DValue - min) / (max - min) * (axisMax - axisMin);
        }

        return transform.inverseTransform(result);
    }

    @Override
    public void zoomRange(double lowerPercent, double upperPercent) {
        double lower = transform.transform(this.getRange().getLowerBound());
        double upper = transform.transform(this.getRange().getUpperBound());
        double length = upper - lower;
        double r0;
        double r1;
        if(this.isInverted()) {
            r0 = lower + length * (1.0D - upperPercent);
            r1 = lower + length * (1.0D - lowerPercent);
        } else {
            r0 = lower + length * lowerPercent;
            r1 = lower + length * upperPercent;
        }

        r0 = transform.inverseTransform(r0);
        r1 = transform.inverseTransform(r1);

        if(r1 > r0 && !java.lang.Double.isInfinite(r1 - r0)) {
            this.setRange(new Range(r0, r1));
        }

    }
}
