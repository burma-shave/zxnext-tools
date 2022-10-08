package com.ericj.zxnexttools.tiled;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.IntStream;

public class TmxToLayer3 {

    public static void main(String[] args) throws
            TmxException,
            ParserConfigurationException,
            IOException,
            SAXException,
            TilesetIsNotIndexedColourException,
            TilesetIsNot8BitsPerixelException {

//        String mapProject = "mtrd_tileset_4bit_rrrgggbb";

        Path tmxFilePath = Path.of(args[0]);

        Path outDir = Path.of(args[1]);


//        String mapProject = "simple";
//        Path tmxFilePath = Path.of("testdata", mapProject, mapProject + ".tmx");
//
//
        TmxFileData tmxFileData = readTmxFileData(tmxFilePath);

        TsxFileData tsxFileData = readTsxFileData(tmxFileData.tsxFilePath());

        Layer3TileData layer3TileData =
                Layer3TileDataCreator.getLayer3TileData(tmxFileData,
                                                        tsxFileData);

        createLayer3TileData(layer3TileData, outDir);
    }


    private static void createLayer3TileData(Layer3TileData tileData,
                                             Path outdir) throws
            IOException,
            TilesetIsNotIndexedColourException,
            TilesetIsNot8BitsPerixelException {

        Path tilesOutPath = outdir.resolve("tiles.bin");
        Path paletteOutPath = outdir.resolve("palette.bin");
        Path mapOutPath = outdir.resolve("map.bin");
        Files.write(tilesOutPath, tileData.tiles());
        Files.write(paletteOutPath, tileData.palette());
        byte[][] tileMapRows = tileData.tileMapRowMajorOrder();

        ByteBuffer tileMapBuffer =
                ByteBuffer.allocate(tileMapRows.length * tileMapRows[0].length);

        for (int i = 0; i < tileMapRows.length; i++) {
            tileMapBuffer.put(tileMapRows[i][1]);
            tileMapBuffer.put(tileMapRows[i][0]);
        }

//        ByteBuffer tileMapBuffer = ByteBuffer.allocate(tileMapRows.length);
//
//        for (int i = 0; i < tileMapRows.length; i++) {
//            tileMapBuffer.put(tileMapRows[i][1]);
//        }
        Files.write(mapOutPath, tileMapBuffer.array());
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

        Node widthInTilesNode =
                layerElement.getAttributes().getNamedItem("width");
        Node heightInTilesNode =
                layerElement.getAttributes().getNamedItem("height");

        int widthInTiles = Integer.parseInt(widthInTilesNode.getTextContent());
        int heightInTiles =
                Integer.parseInt(heightInTilesNode.getTextContent());


        String csvData = dataNode.getTextContent();
        String[] stringValues = normaliseCsvData(csvData);
        IntStream tileMapIds =
                Arrays.stream(stringValues).mapToInt(Integer::parseInt);


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

        return new TmxFileData(pathOfTsxFile, tileMapIds.toArray(),
                               widthInTiles, heightInTiles);
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
// TODO: output a simple tilemap - one screen, no offsets