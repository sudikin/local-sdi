# Web Process Service

This is a simple web processing service that conforms to the Open Geospatial Consortium's WPS guidelines.

- It has GetCapabilities, DescribeProcess and Execute operations.
- Right now it has been implemented for calculating NDVI for a raster image.
- The service is implemented using Flask, images processed using numpy and pillow.
- It will work in conjunciton with any SDI, here we have implemented the SDI with GeoNetwork.


*THIS HAS NOT BEEN TESTED YET*