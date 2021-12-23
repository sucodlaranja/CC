package HistoryRecorder;

import java.io.Serializable;
import java.nio.file.attribute.FileTime;

///This class is represents a completed transfer.
/**
 * This class is just a register of a completed transfer.\n
 * It is not a record because the instance of this class can be updated.\n
 * The instance \ref lastUpdated will eventually be converted to a \b FileTime, so it can be stored.
 */
public class FileTransferHistory implements Serializable {

    /// Represents the last time it was updated.
    private long lastUpdated;
    /// Represents the time it took to complete the transfer in /bms.
    private long timeOfTransfer;
    /// Represents the velocity of the transfer in /bKbps.
    private double bitsPSeg;

    /**
     * Main constructor of this class.\n
     * Given the three parameters will create a class. \n
     * To do that will convert the \b FileTime to a \b long, so it can be stored.
     *
     * @param lastUpdated last time the file was updated.Will be converted to a long.
     * @param timeOfTransfer The time the transfer took.
     * @param bitsPSeg The velocity of the transfer.
     */
    public FileTransferHistory(FileTime lastUpdated, long timeOfTransfer, double bitsPSeg) {
        if (lastUpdated == null) this.lastUpdated = -1;
        else this.lastUpdated = lastUpdated.toMillis();
        this.timeOfTransfer = timeOfTransfer;
        this.bitsPSeg = bitsPSeg;
    }

    ///Constructor that will clone a \ref FileTransferHistory.
    public FileTransferHistory(FileTransferHistory fileTransferHistory){
        this.lastUpdated = fileTransferHistory.lastUpdated;
        this.timeOfTransfer = fileTransferHistory.timeOfTransfer;
        this.bitsPSeg = fileTransferHistory.bitsPSeg;
    }

    ///Basic get method.
    public long getLastUpdated(){ return lastUpdated; }

    ///Basic set method.
    public void setLastUpdated(long lastUpdated ) { this.lastUpdated = lastUpdated; }

    ///Basic set method.
    public void setTimeOfTransfer(long timeOfTransfer ) { this.timeOfTransfer = timeOfTransfer; }

    ///Basic set method.
    public void setBitsPSeg(double bitsPSeg ) { this.bitsPSeg = bitsPSeg; }


    /**
     * This simple method is a simple toString method but modified for html \n
     * Will print the information it has on the format the html requires.
     *
     * @param filename Given the filename of this transfer, so it can be added to the information.
     * @return The string that has the information with the format the html needs.
     */
    public String toHTML(String filename){
        StringBuilder html = new StringBuilder();
        html.append("<h3>").append(filename).append("</h3>");
        if (this.lastUpdated != -1) {
            html.append("<p>&nbsp&nbsp&nbsp<i>Last Updated: </i>").append(FileTime.fromMillis(this.lastUpdated)).append("</p>");
            if (this.timeOfTransfer != -1) {
                html.append("<p>&nbsp&nbsp&nbsp<i>Time of Transfer: </i>").append(this.timeOfTransfer).append("ms</p>");
                html.append("<p>&nbsp&nbsp&nbsp<i>Bits Per Second: </i>").append(this.bitsPSeg).append("Kbps</p>");
            }
            else html.append("<p>&nbsp&nbsp&nbsp<i>No Transfer yet </i> </p>");
        }
        else html.append("<p>&nbsp&nbsp&nbsp<i>No Update yet </i> </p>");
        return html.toString();
    }

}
