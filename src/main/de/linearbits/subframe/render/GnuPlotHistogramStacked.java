/*
 * SUBFRAME - Simple Java Benchmarking Framework
 * Copyright (C) 2012 - 2013 Fabian Prasser
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.linearbits.subframe.render;

import java.util.List;

import de.linearbits.subframe.graph.PlotHistogramStacked;

/**
 * GnuPlot implementation of a stacked histogram
 * 
 * @author Fabian Prasser
 */
class GnuPlotHistogramStacked extends GnuPlot<PlotHistogramStacked> {

    /**
     * Creates a new plot
     * 
     * @param plot
     * @param params
     */
    protected GnuPlotHistogramStacked(PlotHistogramStacked plot, GnuPlotParams params) {
        super(plot, params);
    }

    @Override
    protected String getData() {
        return GnuPlotClusterUtils.getData(this.plot);
    }

    @Override
    protected String getSource(String filename) {

        List<String> gpCommands = getGenericCommands(filename, plot);

        gpCommands.add("set style data histogram");
        gpCommands.add("set style histogram rowstacked gap 1");
        gpCommands.add("set auto x");
        gpCommands.add("set style fill solid border -1");

        if (params.boxwidth != null) {
            gpCommands.add("set boxwidth " + params.boxwidth + " relative");
        }

        int size = GnuPlotClusterUtils.getNumBars(this.plot);
        for (int i = 0; i < size; i++) {
            String command = null;
            String color = null;
            if (params.colorize) {
                color = "#" + params.colors[i % params.colors.length];
            } else {
                color = GnuPlotClusterUtils.getColor(i, size);
            }

            if (i == 0) {
                command = "plot '" + filename + ".dat' using 2:xtic(1) title col linetype 1 linecolor rgb \"" + color + "\"";
            } else {
                command = "     '' using " + (i + 2) + ":xtic(1) title col linetype 1 linecolor rgb \"" + color + "\"";
            }

            if (i < size - 1) command += ",\\";
            gpCommands.add(command);
        }

        StringBuffer buffer = new StringBuffer();
        for (String line : gpCommands) {
            buffer.append(line).append("\n");
        }
        return buffer.toString();
    }
}
