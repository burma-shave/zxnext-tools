package com.ericj.zxnexttools.tiled;

public record Layer3TileData(byte[] tiles,
                             byte[] palette,
                             byte[][] tileMapRowMajorOrder,
                             byte[][] tileMapColumnMajorOrder) {
}
