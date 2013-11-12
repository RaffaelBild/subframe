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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.linearbits.subframe.graph.Plot;
import de.linearbits.subframe.graph.PlotHistogram;
import de.linearbits.subframe.graph.PlotHistogramClustered;
import de.linearbits.subframe.graph.PlotHistogramStacked;
import de.linearbits.subframe.graph.PlotLines;
import de.linearbits.subframe.graph.PlotLinesClustered;

/**
 * Static class for rendering plots with GnuPlot
 * 
 * @author Prasser, Kohlmayer
 */
public abstract class GnuPlot<T extends Plot<?>> {

    /** Counter for random plot names */
    private static int counter = 0;

    /**
     * Renders the given plot
     * @param plot
     * @throws IOException
     */
    public static void plot(Plot<?> plot) throws IOException {
        plot(plot, "plot" + (counter++), false);
    }

    /**
     * Renders the given plot, taking into account the given parameters
     * @param plot
     * @param params
     * @throws IOException
     */
    public static void plot(Plot<?> plot, GnuPlotParams params) throws IOException {
        plot(plot, params, "plot" + (counter++), false);
    }

    /**
     * Renders the given plot, taking into account the given parameters,
     * writing to the given file
     * @param plot
     * @param params
     * @param filename
     * @throws IOException
     */
    public static void plot(Plot<?> plot, GnuPlotParams params, String filename) throws IOException {
        plot(plot, params, filename, false);
    }

    /**
     * Renders the given plot, taking into account the given parameters,
     * writing to the given file. Allows keeping the GnuPlot sources.
     * @param plot
     * @param params
     * @param filename
     * @param keepSources
     * @throws IOException
     */
    public static void
    plot(Plot<?> plot, GnuPlotParams params, String filename, boolean keepSources) throws IOException {

        // Create gnuplot
        GnuPlot<?> gPlot = null;
        if (plot instanceof PlotHistogram) {
            gPlot = new GnuPlotHistogram((PlotHistogram) plot, params);
        } else if (plot instanceof PlotLines) {
            gPlot = new GnuPlotLines((PlotLines) plot, params);
        } else if (plot instanceof PlotHistogramClustered) {
            gPlot = new GnuPlotHistogramClustered((PlotHistogramClustered) plot, params);
        } else if (plot instanceof PlotLinesClustered) {
            gPlot = new GnuPlotLinesClustered((PlotLinesClustered) plot, params);
        } else if (plot instanceof PlotHistogramStacked) {
            gPlot = new GnuPlotHistogramStacked((PlotHistogramStacked) plot, params);
        } else {
            throw new RuntimeException("Invalid type of plot");
        }

        // Write gnuplot file
        String gpFilename = filename;
        if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
            gpFilename = gpFilename.replaceAll("\\\\", "\\\\\\\\");
        }
        File file = new File(filename + ".gp");
        FileWriter writer = new FileWriter(file);
        writer.write(gPlot.getSource(gpFilename));
        writer.close();

        // Write data file
        file = new File(filename + ".dat");
        writer = new FileWriter(file);
        writer.write(gPlot.getData());
        writer.close();

        // Execute
        try {
            plot(filename);
        } catch (IOException e) {
            new File(filename + ".gp").delete();
            new File(filename + ".dat").delete();
            new File(filename + ".eps").delete();
            new File(filename + ".pdf").delete();
            throw (e);
        }

        // Delete files
        if (!keepSources) {
            new File(filename + ".gp").delete();
            new File(filename + ".dat").delete();
            new File(filename + ".eps").delete();
        }
    }

    /**
     * Renders the given plot, writing to the given file
     * @param plot
     * @param filename
     * @throws IOException
     */
    public static void plot(Plot<?> plot, String filename) throws IOException {
        plot(plot, filename, false);
    }

    /**
     * Renders the given plot, writing to the given file. Allows keeping the GnuPlot sources.
     * @param plot
     * @param filename
     * @param keepSources
     * @throws IOException
     */
    public static void plot(Plot<?> plot, String filename, boolean keepSources) throws IOException {
        plot(plot, new GnuPlotParams(), filename, keepSources);
    }

    /**
     * Runs gnuplot.
     * 
     * @param file the file
     * @param preserveGnuplotFiles the preserve gnuplot files
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void plot(final String file) throws IOException {

        // Run gnuplot
        ProcessBuilder b = new ProcessBuilder();
        b.command("gnuplot", file + ".gp");
        Process p = b.start();
        StreamReader in = new StreamReader(p.getInputStream());
        StreamReader error = new StreamReader(p.getErrorStream());
        new Thread(in).start();
        new Thread(error).start();
        try {
            p.waitFor();
        } catch (final InterruptedException e) {
            throw new IOException(e);
        }

        // Check messages
        File eps = new File(file + ".eps");
        String errorMsg = error.getString();
        if (p.exitValue() != 0 || (errorMsg != null && !errorMsg.equals(""))) {
            if (eps.exists()) eps.delete();
            throw new IOException("Error executing gnuplot. Please check the provided series. Error: " + errorMsg);
        }

        // Check file
        if (!eps.exists() || eps.length() == 0) {
            if (eps.exists()) eps.delete();
            throw new IOException("Error executing gnuplot. Please check the provided series. Error: " + errorMsg);
        }

        // Run ps2pdf
        if (new File(file + ".eps").exists()) {

            b = new ProcessBuilder();
            if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
                b.command("ps2pdf",
                          "-dPDFSETTINGS#/prepress",
                          "-dEmbedAllFonts#true",
                          "-dUseFlateCompression#true",
                          file + ".eps",
                          file + ".pdf");
            } else {
                b.command("pstopdf", file + ".eps", file + ".pdf");
            }

            p = b.start();
            in = new StreamReader(p.getInputStream());
            error = new StreamReader(p.getErrorStream());
            new Thread(in).start();
            new Thread(error).start();
            try {
                p.waitFor();
            } catch (final InterruptedException e) {
                throw new IOException(e);
            }

            if (p.exitValue() != 0) {
                new File(file + ".eps").delete();
                throw new IOException("Error running psd2pdf: " + error.getString());
            }
        }

        // Run pdfcrop
        if (new File(file + ".pdf").exists()) {

            b = new ProcessBuilder();
            b.command("pdfcrop", file + ".pdf", file + ".pdf");

            p = b.start();

            in = new StreamReader(p.getInputStream());
            error = new StreamReader(p.getErrorStream());
            new Thread(in).start();
            new Thread(error).start();
            try {
                p.waitFor();
            } catch (final InterruptedException e) {
                throw new IOException(e);
            }

            if (p.exitValue() != 0) {
                new File(file + ".pdf").delete();
                throw new IOException("Error running pdfcrop: " + error.getString());
            }
        }
    }

    /** The plot to render*/
    protected T             plot;
    /** The parameters to use*/
    protected GnuPlotParams params;
    
    /**
     * Constructs a new instance
     * @param plot
     * @param params
     */
    protected GnuPlot(T plot, GnuPlotParams params) {
        this.plot = plot;
        this.params = params;
    }

    /** Returns the data*/
    protected abstract String getData();

    /** Returns the GnuPlot source code*/
    protected abstract String getSource(String filename);
}