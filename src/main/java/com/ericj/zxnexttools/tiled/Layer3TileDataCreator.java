package com.ericj.zxnexttools.tiled;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Layer3TileDataCreator {

    public static byte[][] getLayer3TileMapDataColumnOrder(
            TmxFileData tmxFileData, TsxFileData tsxFileData) {
        byte[][] layer3TileMapData =
                getLayer3TileMapData(tmxFileData, tsxFileData);

        byte[][] columnMajorData = new byte[layer3TileMapData.length][2];
        int mapWidth = tmxFileData.mapWidth() * (tsxFileData.tileWidth() / 8);
        int mapHeight =
                tmxFileData.mapHeight() * (tsxFileData.tileHeight() / 8);

        int i = 0;
        for (int k = 0; k < mapHeight; k++)
            for (int j = 0; j < mapWidth; j++) {
                columnMajorData[i++] = layer3TileMapData[j * mapHeight + k];
            }

        return columnMajorData;
    }

    public static byte[][] getLayer3TileMapData(TmxFileData tmxFileData,
                                                TsxFileData tsxFileData) {
        int metaTileHeight = tsxFileData.tileHeight() / 8;
        int metaTileWidth = tsxFileData.tileWidth() / 8;

        byte[][] layer3TileMaData = new byte[metaTileHeight * metaTileWidth *
                                             tmxFileData.mapWidth() *
                                             tmxFileData.mapHeight()][2];

        int tileMapIndex = 0;
        for (int tileMapRow = 0;
             tileMapRow < tmxFileData.mapHeight();
             tileMapRow++) {

            for (int metaTileRow = 0;
                 metaTileRow < metaTileHeight;
                 metaTileRow++) {

                for (int tileMapColumn = 0;
                     tileMapColumn < tmxFileData.mapWidth();
                     tileMapColumn++) {
                    int mapTileIndex = tileMapColumn +
                                       (tileMapRow * tmxFileData.mapWidth());
                    int tileId = tmxFileData.layer1TilemapData()[mapTileIndex];

                    for (int metaTileColumn = 0;
                         metaTileColumn < metaTileWidth;
                         metaTileColumn++) {

                        int layer3TileId =
                                (tileId * metaTileWidth + metaTileColumn);
                        byte[] value = new byte[2];
                        value[0] = 0;
                        value[1] = (byte) layer3TileId;
                        layer3TileMaData[tileMapIndex] = value;
                        tileMapIndex += 1;
                    }
                }
            }
        }
        return layer3TileMaData;
    }

    public static Layer3TileData getLayer3TileData(TmxFileData tmxFileData,
                                                   TsxFileData tsxFileData)
            throws
            IOException,
            TilesetIsNotIndexedColourException,
            TilesetIsNot8BitsPerixelException {
        BufferedImage tilesetImage =
                ImageIO.read(tsxFileData.tilesetImageFilePath().toFile());
        byte[] tiles = getTiles(tilesetImage, tsxFileData);

        return new Layer3TileData(tiles, getPalette(tilesetImage),
                                  getLayer3TileMapData(tmxFileData,
                                                       tsxFileData),
                                  getLayer3TileMapDataColumnOrder(tmxFileData,
                                                                  tsxFileData));
    }

    private static byte[] getPalette(BufferedImage tilesetImage) {
        IndexColorModel indexColourModel =
                (IndexColorModel) tilesetImage.getColorModel();

        int paletteSize = indexColourModel.getMapSize();

        byte[][] imagePalette = new byte[3][paletteSize];

        indexColourModel.getReds(imagePalette[0]);
        indexColourModel.getGreens(imagePalette[1]);
        indexColourModel.getBlues(imagePalette[2]);

        byte[] layer3PaletteData = new byte[paletteSize];

        for (int i = 0; i < paletteSize; i++) {
            byte red = imagePalette[0][i];
            byte green = imagePalette[1][i];
            byte blue = imagePalette[2][i];
            layer3PaletteData[i] =
                    (byte) ((red & 0b11100000) | ((green & 0b11100000) >>> 3) |
                            ((blue & 0b11000000) >>> 6));
        }

        return layer3PaletteData;
    }

    private static byte[] getTiles(BufferedImage tilesetImage,
                                   TsxFileData tsxFileData) throws
            TilesetIsNotIndexedColourException,
            TilesetIsNot8BitsPerixelException {

        ColorModel colorModel = tilesetImage.getColorModel();

        int pixelSize = colorModel.getPixelSize();
        boolean is8BitsPerPixel = pixelSize == 8;
        boolean isIndexedColour = colorModel instanceof IndexColorModel;

        if (!isIndexedColour)
            throw new TilesetIsNotIndexedColourException();

//        if (!is8BitsPerPixel)
//            throw new TilesetIsNot8BitsPerixelException();

        IntStream tileIds = IntStream.range(0, tsxFileData.tileCount());

        Raster tilesetImageData = tilesetImage.getData();
        Stream<Rectangle> tileRectagnles = tileIds.mapToObj(id -> new Rectangle(
                (id % (tsxFileData.tilesetImageWidth() / 8)) * 8,
                (id / (tsxFileData.tilesetImageWidth() / 8)) * 8, 8, 8));

        Function<Rectangle, int[]> rectangleIntStreamFunction =
                rect -> tilesetImageData.getPixels(rect.x, rect.y, rect.height,
                                                   rect.width, new int[8 * 8]);
        Stream<int[]> tileData = tileRectagnles.map(rectangleIntStreamFunction);

        int layer3TileCount =
                ((tsxFileData.tileWidth() * tsxFileData.tileHeight()) / 64) *
                tsxFileData.tileCount();

        // allocate space for all tiles plus one transparent tile - id 0
        ByteBuffer layer3TileData =
                ByteBuffer.allocate((layer3TileCount + 1) * 32);


        byte[] trasparentTile = new byte[32];
        Arrays.fill(trasparentTile, (byte) 0xff);

        layer3TileData.put(trasparentTile);


        tileData.forEach(pixels -> {
            byte[] tilePixels = new byte[32];
            for (int i = 0; i < 64; i = i + 2) {
                int leftPixel = pixels[i] << 4;
                int rightPixel = pixels[i + 1];
                int combined = leftPixel | rightPixel;
                tilePixels[i / 2] = (byte) combined;
            }
            layer3TileData.put(tilePixels);
        });

        return layer3TileData.array();
    }
}
