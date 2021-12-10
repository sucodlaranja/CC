package Logs;

/**
 * Represents entry of a transfer.
 */
public record TransferLogs(String fileName, boolean senderOrReceiver) {

    public String getFileName() {
        return fileName;
    }

    public boolean isSender() {
        return senderOrReceiver;
    }

}
