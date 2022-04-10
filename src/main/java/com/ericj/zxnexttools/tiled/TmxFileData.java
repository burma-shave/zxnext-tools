package com.ericj.zxnexttools.tiled;

import java.util.stream.Stream;

public record TmxFileData(String tsxFilePath, Stream<Integer> layer1TilemapData) {
}
