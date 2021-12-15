package HistoryRecorder;

import java.nio.file.attribute.FileTime;

/**
 * Represents entry of a transfer.
 */
public class FileTransferHistory{
    private FileTime lastUpdated;
    private int timeOfTransfer;
    private int bitsPSeg;

    public FileTransferHistory(FileTime lastUpdated, int timeOfTransfer, int bitsPSeg) {
        this.lastUpdated = lastUpdated;
        this.timeOfTransfer = timeOfTransfer;
        this.bitsPSeg = bitsPSeg;
    }

    public void setLastUpdated(FileTime lastUpdated ) { this.lastUpdated = lastUpdated; }

    public void setTimeOfTransfer(int timeOfTransfer ) { this.timeOfTransfer = timeOfTransfer; }

    public void setBitsPSeg(int bitsPSeg ) { this.bitsPSeg = bitsPSeg; }

    public String toHTML(String filename){
        // TODO : JAO FAZER
        return "";
    }

}
