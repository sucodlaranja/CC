package HistoryRecorder;

import java.io.Serializable;
import java.nio.file.attribute.FileTime;
import java.sql.Timestamp;

/**
 * Represents entry of a transfer.
 */
public class FileTransferHistory implements Serializable {
    private long lastUpdated;
    private long timeOfTransfer;
    private double bitsPSeg;

    public FileTransferHistory(FileTime lastUpdated, long timeOfTransfer, double bitsPSeg) {
        if (lastUpdated == null) this.lastUpdated = -1;
        else this.lastUpdated = lastUpdated.toMillis();
        this.timeOfTransfer = timeOfTransfer;
        this.bitsPSeg = bitsPSeg;
    }

    public FileTransferHistory(FileTransferHistory fileTransferHistory){
        this.lastUpdated = fileTransferHistory.lastUpdated;
        this.timeOfTransfer = fileTransferHistory.timeOfTransfer;
        this.bitsPSeg = fileTransferHistory.bitsPSeg;
    }

    public long getFileTime(){
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated ) {
        this.lastUpdated = lastUpdated;
    }

    public void setTimeOfTransfer(long timeOfTransfer ) { this.timeOfTransfer = timeOfTransfer; }

    public void setBitsPSeg(double bitsPSeg ) { this.bitsPSeg = bitsPSeg; }

    public FileTransferHistory clone(){
        return new FileTransferHistory(this);
    }

    //TODO: TESTARRRRRR
    public String toHTML(String filename){
        StringBuilder html = new StringBuilder();
        html.append("<h3>" + filename + "</h3>");
        if (this.lastUpdated != -1) {
            html.append("<p>&nbsp&nbsp&nbsp<i>Last Updated: </i>" + FileTime.fromMillis(this.lastUpdated) + "</p>");
            if (this.timeOfTransfer != -1) {
                html.append("<p>&nbsp&nbsp&nbsp<i>Time of Transfer: </i>" + this.timeOfTransfer + "ms</p>");
                html.append("<p>&nbsp&nbsp&nbsp<i>Bits Per Second: </i>" + this.bitsPSeg + "Kbps</p>");
            }
            else html.append("<p>&nbsp&nbsp&nbsp<i>No Transfer yet </i> </p>");
        }
        else html.append("<p>&nbsp&nbsp&nbsp<i>No Update yet </i> </p>");
        return html.toString();
    }

}
