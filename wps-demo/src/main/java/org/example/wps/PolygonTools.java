package org.example.wps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryComponentFilter;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.StaticMethodsProcessFactory;
import org.geotools.text.Text;


public class PolygonTools extends StaticMethodsProcessFactory<PolygonTools>{

    public PolygonTools() {
        super(Text.text("Polygon Tools"), "custom", PolygonTools.class);
    }

    static Geometry polygonize(Geometry geometry){
        List lines = LineStringExtracter.getLines(geometry);
        Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(lines);
        Collection polys = polygonizer.getPolygons();
        Polygon[] polyArray = GeometryFactory.toPolygonArray(polys);

        return geometry.getFactory().createGeometryCollection(polyArray);
    }

    @DescribeProcess(title = "splitPolygon", description = "Splits a polygon by a linestring")
    @DescribeResult(description = "Geometry collection created by splitting the input polygon")
    
    public static Geometry splitPolygon(
        @DescribeParameter(name = "polygon", description = "Polygon to be split") Geometry poly,
        @DescribeParameter(name = "line", description = "Line to split the polygon") Geometry line) {
        
        Geometry nodedLinework = poly.getBoundary().union(line);
        Geometry polys = polygonize(nodedLinework);

        List output = new ArrayList();
        for (int i = 0; i < polys.getNumGeometries(); i++) {
            Polygon candpoly = (Polygon) polys.getGeometryN(i);
            if (poly.contains(candpoly.getInteriorPoint())) {
                output.add(candpoly);
            }
        }
        return poly.getFactory().createGeometryCollection(GeometryFactory.toGeometryArray(output));
    }
}