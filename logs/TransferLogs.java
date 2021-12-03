/**
 * Esta classe representa os logs de um ficheiro.
 */
public record TransferLogs(String fileName, boolean senderOrReceiver) {

    public String getFileName() {
        return fileName;
    }

    public boolean isSenderOrReceiver() {
        return senderOrReceiver;
    }

}
