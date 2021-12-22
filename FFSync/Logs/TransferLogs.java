package Logs;

import java.nio.file.attribute.FileTime;

/**
 * Represents entry of a transfer.
 */
public record TransferLogs(String fileName, boolean sender, long elapsedTime, double bitsPSeg) {

}
