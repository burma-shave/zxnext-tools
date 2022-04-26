package com.ericj.zxnexttools.tiled;

import java.nio.file.Path;

public record TsxFileData(
        // parse out individual tiles and data
        Path tilesetImageFilePath,
        int tilesetImageWidth,
        int tilesetImageHeight,
        int tileWidth,
        int tileHeight,
        int tileCount,
        int columns) {
}
