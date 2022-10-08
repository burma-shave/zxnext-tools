package com.ericj.zxnexttools.tiled;

import java.nio.file.Path;

public record TmxFileData(Path tsxFilePath,
                          int[] layer1TilemapData,
                          int mapWidth,
                          int mapHeight) {
}
