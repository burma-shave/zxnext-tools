package com.ericj.zxnexttools.tiled;

import org.w3c.dom.Document;

import java.nio.file.Path;

/**
 * Indicates a problem opening or parsing the TMX file.
 */
public class TmxException extends Exception {

    public TmxException(String message, String filepath, Exception e) {
        super(message + " TMX Filepath: " + filepath, e);
    }

    public TmxException(String message, Path filepath) {
        super(message + " TMX Filepath: " + filepath);
    }

    public TmxException(String message, String filepath) {
        super(message + " TMX Filepath: " + filepath);
    }

    public TmxException(String message, Document tmxDocument) {
        super(message + " TMX Filepath: " + tmxDocument.getDocumentURI());
    }

    public TmxException(String message, Document tmxDocument, Exception e) {
        super(message + " TMX Filepath: " + tmxDocument.getDocumentURI(), e);
    }
}
