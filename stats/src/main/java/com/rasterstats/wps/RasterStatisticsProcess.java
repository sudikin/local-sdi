package com.rasterstats.wps;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.processing.Operations;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.StaticMethodsProcessFactory;
import org.geotools.text.Text;
import org.opengis.util.ProgressListener;

import java.awt.image.Raster;
import java.util.Collections;
import java.util.Set;

public class RasterStatisticsProcess extends StaticMethodsProcessFactory<RasterStatisticsProcess> {

    public RasterStatisticsProcess() {
        super(Text.text("Raster Statistics Calculation"), "wps", RasterStatisticsProcess.class);
    }

    @DescribeProcess(title = "Raster Statistics Calculation", description = "Calculates statistics (min, max, mean, standard deviation) from input raster")
    @DescribeResult(name = "Statistics", description = "Statistics of the input raster")
    public static RasterStatistics calculateStatistics(
            @DescribeParameter(name = "Raster", description = "Raster coverage to calculate statistics") GridCoverage2D raster,
            ProgressListener listener) {

        Raster data = raster.getRenderedImage().getData();

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0;
        double sumOfSquares = 0;
        int count = 0;

        int width = data.getWidth();
        int height = data.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixelValue = data.getSample(x, y, 0); // Assuming single band raster

                if (pixelValue < min) {
                    min = pixelValue;
                }
                if (pixelValue > max) {
                    max = pixelValue;
                }
                sum += pixelValue;
                sumOfSquares += pixelValue * pixelValue;
                count++;
            }
        }

        double mean = sum / count;
        double variance = (sumOfSquares - (sum * sum) / count) / count;
        double stdDev = Math.sqrt(variance);

        return new RasterStatistics(min, max, mean, stdDev);
    }

    // Inner class to hold the raster statistics
    public static class RasterStatistics {
        public final double min;
        public final double max;
        public final double mean;
        public final double stdDev;

        public RasterStatistics(double min, double max, double mean, double stdDev) {
            this.min = min;
            this.max = max;
            this.mean = mean;
            this.stdDev = stdDev;
        }

        @Override
        public String toString() {
            return String.format("Min: %.2f, Max: %.2f, Mean: %.2f, StdDev: %.2f", min, max, mean, stdDev);
        }
    }
}