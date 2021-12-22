package Logs;

import java.nio.file.attribute.FileTime;

public record LogsRecord(FileTime fileTime, long checksum) {

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
