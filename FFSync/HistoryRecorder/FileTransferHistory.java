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


    //TODO: TESTARRRRRR
    public String toHTML(String filename){
        StringBuilder html = new StringBuilder();
        html.append("<h3>" + filename + "</h3>");
        html.append("<p>&nbsp&nbsp&nbsp<i>Last Updated: </i>" + this.lastUpdated.toString() + "</p>");
        html.append("<p>&nbsp&nbsp&nbsp<i>Time of Transfer: </i>" + this.timeOfTransfer + "s</p>");
        html.append("<p>&nbsp&nbsp&nbsp<i>Bits Per Second: </i>" + this.bitsPSeg + "</p>");
        return html.toString();
    }

}
