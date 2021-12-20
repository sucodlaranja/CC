package Logs;

/**
 * Represents entry of a transfer.
 */
public record TransferLogs(String fileName, boolean sender) {

    // Name of the file
    public String getFileName() {return fileName;}

    // Indicates if this file is to be sent or received
    public boolean isSender() {return sender;}
}
