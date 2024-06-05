from flask import Flask, request, send_file
import numpy as np
from PIL import Image
import xml.etree.ElementTree as ET
import io
from base64 import b64decode

app = Flask(__name__)

@app.route('/wps', methods=['GET', 'POST'])
def wps():
    request_type = request.args.get('request', None)
    if request.method == 'GET' and request_type == 'GetCapabilities':
        return get_capabilities()
    elif request.method == 'GET' and request_type == 'DescribeProcess':
        return describe_process()
    elif request.method == 'POST' and 'Execute' in request.data.decode('utf-8'):
        return execute()
    else:
        return "Invalid Request", 400

def get_capabilities():
    capabilities_xml = """
    <wps:Capabilities xmlns:wps="http://www.opengis.net/wps/1.0.0"
                      xmlns:ows="http://www.opengis.net/ows/1.1">
        <ows:ServiceIdentification>
            <ows:Title>NDVI Calculator</ows:Title>
            <ows:Abstract>A simple NDVI Calculator</ows:Abstract>
            <ows:ServiceType>WPS</ows:ServiceType>
            <ows:ServiceTypeVersion>1.0.0</ows:ServiceTypeVersion>
        </ows:ServiceIdentification>
        <ows:OperationsMetadata>
            <ows:Operation name="GetCapabilities">
                <ows:DCP>
                    <ows:HTTP>
                        <ows:Get xlink:href="http://localhost:5000/wps?request=GetCapabilities"/>
                    </ows:HTTP>
                </ows:DCP>
            </ows:Operation>
            <ows:Operation name="DescribeProcess">
                <ows:DCP>
                    <ows:HTTP>
                        <ows:Get xlink:href="http://localhost:5000/wps?request=DescribeProcess&identifier=NDVI"/>
                    </ows:HTTP>
                </ows:DCP>
            </ows:Operation>
            <ows:Operation name="Execute">
                <ows:DCP>
                    <ows:HTTP>
                        <ows:Post xlink:href="http://localhost:5000/wps"/>
                    </ows:HTTP>
                </ows:DCP>
            </ows:Operation>
        </ows:OperationsMetadata>
        <wps:ProcessOfferings>
            <wps:Process>
                <ows:Identifier>NDVI</ows:Identifier>
                <ows:Title>NDVI Calculator</ows:Title>
            </wps:Process>
        </wps:ProcessOfferings>
    </wps:Capabilities>
    """
    return capabilities_xml, 200, {'Content-Type': 'text/xml'}

def describe_process():
    process_description_xml = """
    <wps:ProcessDescriptions xmlns:wps="http://www.opengis.net/wps/1.0.0"
                             xmlns:ows="http://www.opengis.net/ows/1.1">
        <wps:ProcessDescription wps:processVersion="1.0.0">
            <ows:Identifier>NDVI</ows:Identifier>
            <ows:Title>NDVI Calculator</ows:Title>
            <ows:Abstract>Calculates the Normalized Difference Vegetation Index (NDVI).</ows:Abstract>
            <wps:DataInputs>
                <wps:Input>
                    <ows:Identifier>image</ows:Identifier>
                    <ows:Title>Input Image</ows:Title>
                    <ows:Abstract>The input image containing multiple bands.</ows:Abstract>
                    <wps:ComplexData>
                        <ows:Format>
                            <ows:MimeType>image/tiff</ows:MimeType>
                        </ows:Format>
                    </wps:ComplexData>
                </wps:Input>
            </wps:DataInputs>
            <wps:ProcessOutputs>
                <wps:Output>
                    <ows:Identifier>NDVI_Result</ows:Identifier>
                    <ows:Title>NDVI Calculation Result</ows:Title>
                    <ows:Abstract>The resulting NDVI values.</ows:Abstract>
                    <wps:ComplexOutput>
                        <ows:Format>
                            <ows:MimeType>image/tiff</ows:MimeType>
                        </ows:Format>
                    </wps:ComplexOutput>
                </wps:Output>
            </wps:ProcessOutputs>
        </wps:ProcessDescription>
    </wps:ProcessDescriptions>
    """
    return process_description_xml, 200, {'Content-Type': 'text/xml'}

def execute():
    request_xml = request.data.decode('utf-8')
    if '<ows:Identifier>NDVI</ows:Identifier>' in request_xml:
        # Parse the input XML to find the image data
        image_data = extract_image(request_xml, 'image')

        if image_data:
            image = Image.open(io.BytesIO(image_data))
            red_band, nir_band = extract_bands(image)

            # Calculate NDVI
            ndvi = calculate_ndvi(red_band, nir_band)

            # Convert NDVI result to an image
            ndvi_image = Image.fromarray((ndvi * 255).astype(np.uint8))

            # Save NDVI result to a bytes buffer
            ndvi_buffer = io.BytesIO()
            ndvi_image.save(ndvi_buffer, format='TIFF')
            ndvi_buffer.seek(0)

            return send_file(ndvi_buffer, mimetype='image/tiff', as_attachment=True, download_name='ndvi_result.tiff')

        return "Invalid image data", 400
    else:
        return "Invalid Request", 400

def extract_image(request_xml, identifier):
    tree = ET.ElementTree(ET.fromstring(request_xml))
    root = tree.getroot()
    for input_element in root.findall('.//wps:Input', namespaces={'wps': 'http://www.opengis.net/wps/1.0.0'}):
        identifier_text = input_element.find('ows:Identifier', namespaces={'ows': 'http://www.opengis.net/ows/1.1'}).text
        if identifier_text == identifier:
            complex_data = input_element.find('wps:Data/wps:ComplexData', namespaces={'wps': 'http://www.opengis.net/wps/1.0.0'})
            image_data = complex_data.text
            return b64decode(image_data)
    return None

def extract_bands(image):
    red_band = np.array(image.getchannel(0))
    nir_band = np.array(image.getchannel(1))
    return red_band, nir_band

def calculate_ndvi(red_band, nir_band):
    np.seterr(divide='ignore', invalid='ignore')
    ndvi = (nir_band - red_band) / (nir_band + red_band)
    return ndvi

if __name__ == '__main__':
    app.run(debug=True)