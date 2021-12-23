package Logs;

import java.nio.file.attribute.FileTime;

/// A simple record of a log of a file. Contains the lat time it was updated and its checksum.
public record LogsRecord(FileTime fileTime, long checksum) {

    /// Simple compareTo method. If the checksum is equal, the files are equal, so we use the time to compare.
    public int compareTo(LogsRecord logsRecord) {
        long compareChecksum = this.checksum - logsRecord.checksum;
        int compareFileTime = this.fileTime.compareTo(logsRecord.fileTime);

        if (compareChecksum == 0) return 0;
        else if (compareFileTime > 0) return 1;
        else if (compareFileTime < 0) return -1;
        else if (compareChecksum > 0) return 1;
        else return -1;
    }
}
