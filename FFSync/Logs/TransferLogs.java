package Logs;

/// Record of a transfer.
public record TransferLogs(String fileName, boolean sender, long elapsedTime, double bitsPSeg) {

}
