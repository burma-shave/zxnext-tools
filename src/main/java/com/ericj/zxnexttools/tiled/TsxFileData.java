package com.ericj.zxnexttools.tiled;

import java.util.stream.Stream;

public record TsxFileData(
        // parse out individual tiles and data
        String tilesetImageFilePath,
        int tilesetImageWidth,
        int tilesetImageHeight,
        int tileWidth,
        int tileHeight,
        int tileCount,
        int columns
) {
}
