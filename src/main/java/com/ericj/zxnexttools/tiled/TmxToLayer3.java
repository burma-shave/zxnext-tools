package com.ericj.zxnexttools.tiled;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

public class TmxToLayer3 {

    public static void main(String[] args) throws
            TmxException,
            ParserConfigurationException,
            IOException,
            SAXException {
        // open tmx (xml) file
        // open tileset image (png)
        String tmxFileName = "deepforest.tmx";
//        String tmxFilePath = resourcesBasePath + File.separator + tmxFileName;
        Path tmxFilePath = Path.of("testdata", "8x8_16colour", tmxFileName);


        TmxFileData tmxFileData = readTmxFileData(tmxFilePath);

        System.out.println(tmxFileData);
        TsxFileData tsxFileData = readTsxFileData(tmxFileData.tsxFilePath());
        System.out.println(tsxFileData);

        // convert tsx to layer 3 tile data

        try (OutputStream tilesOut = Files.newOutputStream(
                Path.of("foo.out"))) {
            createLayer3TileData(tsxFileData, tilesOut);
        }
    }


    private static void createLayer3TileData(TsxFileData tiles,
                                             OutputStream out)
            throws IOException {
        System.out.println(tiles);
        BufferedImage tilesetImage =
                ImageIO.read(tiles.tilesetImageFilePath().toFile());

        System.out.println(tilesetImage.getColorModel());
        // create tiledata stream
        // write to file

    }

    /**
     * Read in metadata about the tileset.
     *
     * @param tsxFilePath
     * @return
     */
    private static TsxFileData readTsxFileData(Path tsxFilePath) throws
            ParserConfigurationException,
            IOException,
            SAXException,
            TmxException {
        Document tsxDocument = openXmlFile(tsxFilePath);
        Element rootElement = tsxDocument.getDocumentElement();
        if (rootElement.getNodeName() != "tileset") {
            throw new TmxException(
                    "Does not appear to be a valid tsx file, root element " +
                    "must be 'tilset'", tsxFilePath);
        }

        int tileWidth = readIntAttribute(rootElement, "tilewidth");
        int tileHeight = readIntAttribute(rootElement, "tileheight");
        int tileCount = readIntAttribute(rootElement, "tilecount");
        int columns = readIntAttribute(rootElement, "columns");

        NodeList matchingNodes = tsxDocument.getElementsByTagName("image");

        if (!(matchingNodes.getLength() == 1)) {
            throw new TmxException(
                    "More than one image element found in tileset.",
                    tsxDocument);
        }

        Node imageNode = matchingNodes.item(0);
        Element imageElement = (Element) imageNode;
        int tilesetImageWidth = readIntAttribute(imageElement, "width");
        int tilesetImageHeight = readIntAttribute(imageElement, "height");
        String tilesetSource = readStringAttribute(imageElement, "source");
        Path tileSetImagePath = tsxFilePath.resolveSibling(tilesetSource);

        return new TsxFileData(tileSetImagePath, tilesetImageWidth,
                               tilesetImageHeight, tileWidth, tileHeight,
                               tileCount, columns);
    }

    /**
     * Reads an attribute from an element. Just wraps the call with some
     * error handling.
     *
     * @param element
     * @param attributeName
     * @return
     * @throws TmxException
     */
    private static int readIntAttribute(Element element, String attributeName)
            throws TmxException {
        String attributeValue = readStringAttribute(element, attributeName);

        int intValue;
        try {
            intValue = Integer.parseInt(attributeValue);
        } catch (NumberFormatException ex) {
            throw new TmxException(
                    "Could not parse int value of " + attributeName +
                    " in tileset element", element.getOwnerDocument(), ex);
        }

        return intValue;
    }

    private static String readStringAttribute(Element element,
                                              String attributeName)
            throws TmxException {
        String attributeValue = element.getAttribute(attributeName);
        if (attributeValue.equals("")) {
            throw new TmxException("No " + attributeName +
                                   " attribute found on tileset element.",
                                   element.getOwnerDocument());
        }

        return attributeValue;
    }

    private static TmxFileData readTmxFileData(Path tmxFilePath) throws
            ParserConfigurationException,
            IOException,
            SAXException,
            TmxException {
        Document tmxDocument = openXmlFile(tmxFilePath);
        // extract data from tmx document
        tmxDocument.getDocumentElement().normalize();
        NodeList layerElements = tmxDocument.getElementsByTagName("layer");
        if (layerElements.getLength() == 0) {
            throw new TmxException(
                    "No layers found in tmx file, there must be at least one " +
                    "layer", tmxDocument.getDocumentURI());
        }

        Node layerElement = layerElements.item(0);
        NodeList layerChildren = layerElement.getChildNodes();

        Node dataNode = null;
        for (int i = 0; i < layerChildren.getLength(); i++) {
            Node node = layerChildren.item(i);
            if (node.getNodeName().equals("data")) {
                dataNode = node;
                break;
            }
        }

        if (dataNode == null) {
            throw new TmxException(
                    "First layer must have a data element for csv tilemap data",
                    tmxDocument.getDocumentURI());
        }


        String csvData = dataNode.getTextContent();
        String[] stringValues = normaliseCsvData(csvData);
        Stream<Integer> tileMapIds =
                Arrays.stream(stringValues).map(Integer::parseInt);

        NodeList tilesetElements = tmxDocument.getElementsByTagName("tileset");
        if (!(tilesetElements.getLength() == 1)) {
            throw new TmxException("There must be one tileset defined",
                                   tmxDocument);
        }

        Node sourceAttribute =
                tilesetElements.item(0).getAttributes().getNamedItem("source");
        if (sourceAttribute == null) {
            throw new TmxException("tileset does not have a source element, " +
                                   "unable to determine tileset file. " +
                                   "The tmx file must define an external " +
                                   "tileset.", tmxDocument);
        }

        String tilesetPath = sourceAttribute.getTextContent();

        Path pathOfTsxFile = tmxFilePath.resolveSibling(tilesetPath);

        return new TmxFileData(pathOfTsxFile, tileMapIds);
    }

    private static String[] normaliseCsvData(String csvData) {
        String trimmedCsvData = csvData.trim();
        String whiteSpaceRemoved = trimmedCsvData.replaceAll("\\s", "");
        return whiteSpaceRemoved.split(",");
    }

    /**
     * Open and parse the contents of a xml file.
     *
     * @param filePath
     * @return The document representation of the contents of the xml file
     * @throws TmxException
     */
    public static Document openXmlFile(Path filePath)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder =
                DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.parse(filePath.toFile());
    }
}
