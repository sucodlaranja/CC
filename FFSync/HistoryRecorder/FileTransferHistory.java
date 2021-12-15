package HistoryRecorder;

import java.nio.file.attribute.FileTime;

/**
 * Represents entry of a transfer.
 */
public record FileTransferHistory(String fileName, FileTime lastUpdated, int timeOfTransfer, int bitsPSeg) {

    public String getFileName() {
        return fileName;
    }

    public FileTime lastUpdated() { return lastUpdated; }

    public int timeOfTransfer() { return timeOfTransfer; }

    public int bitsPSeg() { return bitsPSeg; }

    public String toHTML(){
        // TODO : JAO FAZER
        return "";
    }

}
