package com.example.wps;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.Envelope2D;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.StaticMethodsProcessFactory;
import org.geotools.text.Text;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Collections;
import java.util.Set;

public class NDVIProcess extends StaticMethodsProcessFactory<NDVIProcess> {
    
    public NDVIProcess() {
        super(Text.text("NDVI Calculation"), "wps", NDVIProcess.class);
    }
        
        // @Override
        // public Set<Name> getNames() {
        //     return Collections.singleton(new NameImpl("NDVIProcess"));
        //     }
                
    @DescribeProcess(title = "NDVI Calculation", description = "Calculates NDVI from input raster bands")
    @DescribeResult(name = "NDVI", description = "Calculated NDVI raster")
    public static GridCoverage2D calculateNDVI(
            @DescribeParameter(name = "Red Band", description = "Raster coverage of the red band") GridCoverage2D redBand,
            @DescribeParameter(name = "NIR Band", description = "Raster coverage of the near-infrared band") GridCoverage2D nirBand,
            ProgressListener listener) {

        RenderedImage A = redBand.getRenderedImage();
        RenderedImage B = nirBand.getRenderedImage();

        // Check compatibility of raster images
        if (B.getWidth() != A.getWidth() || A.getHeight() != B.getHeight()) {
            throw new IllegalArgumentException("Input rasters have different dimensions");
        }

        int width = A.getWidth();
        int height = A.getHeight();

        // Create a new BufferedImage to store the result
        BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // Access pixel values and perform (A-B) / (A+B)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Retrieve pixel values at (x, y) from both images
                int pixelA = getPixelValue(A, x, y);
                int pixelB = getPixelValue(B, x, y);

                // Perform (A-B)
                int subtractedValue = pixelA - pixelB;

                // Perform (A+B)
                int addedValue = pixelA + pixelB;

                // Avoid division by zero
                int newValue = (addedValue != 0) ? subtractedValue / addedValue : 0;

                // Set the resulting pixel value in the result image
                resultImage.setRGB(x, y, newValue);
            }
        }

        // Create a GridCoverageFactory to create GridCoverage2D
        GridCoverageFactory coverageFactory = new GridCoverageFactory();

        // Create envelope based on the bounding box of the input bands
        Envelope2D envelope = new Envelope2D(redBand.getEnvelope());

        // Create GridCoverage2D from the result image and envelope
        GridCoverage2D coverage = coverageFactory.create("NDVI", resultImage, envelope);
        
        return coverage;
    }

    // Method to retrieve pixel value from a RenderedImage
    private static int getPixelValue(RenderedImage image, int x, int y) {
        // Get pixel value using BufferedImage
        BufferedImage bufferedImage = (BufferedImage) image;
        return bufferedImage.getRGB(x, y) & 0xFF; // Assuming grayscale image
    }
}
