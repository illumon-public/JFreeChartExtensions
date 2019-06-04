package com.jfreechartextensions;

import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

import java.util.Iterator;

/**
 * An extension of {@link XYPlot} with following changes
 *
 * <ol>
 *     <li>Show series in legend even if series is not visible.</li>
 * </ol>
 */
public class ExtendedXYPlot extends XYPlot {

    @Override
    public LegendItemCollection getLegendItems() {
        if (this.getFixedLegendItems() != null) {
            return this.getFixedLegendItems();
        } else {
            LegendItemCollection result = new LegendItemCollection();
            Iterator var2 = this.getDatasets().values().iterator();

            while(true) {
                XYDataset dataset;
                int datasetIndex;
                XYItemRenderer renderer;
                do {
                    do {
                        if (!var2.hasNext()) {
                            return result;
                        }

                        dataset = (XYDataset)var2.next();
                    } while(dataset == null);

                    datasetIndex = this.indexOf(dataset);
                    renderer = this.getRenderer(datasetIndex);
                    if (renderer == null) {
                        renderer = this.getRenderer(0);
                    }
                } while(renderer == null);

                int seriesCount = dataset.getSeriesCount();

                for(int i = 0; i < seriesCount; ++i) {

                    //changed following predicate from super to show the legend item for series even if it's not visible since we want to show legend item and just don't want to render the series
                    if (renderer.isSeriesVisibleInLegend(i)) {
                        LegendItem item = renderer.getLegendItem(datasetIndex, i);
                        if (item != null) {
                            result.add(item);
                        }
                    }
                }
            }
        }
    }
}
