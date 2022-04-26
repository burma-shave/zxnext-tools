package com.ericj.zxnexttools.tiled;

import java.nio.file.Path;
import java.util.stream.Stream;

public record TmxFileData(Path tsxFilePath, Stream<Integer> layer1TilemapData) {
}
