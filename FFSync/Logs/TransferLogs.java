package Logs;

/**
 * Represents entry of a transfer.
 */
public record TransferLogs(String fileName, boolean sender) {

    // TODO: Não se percebe nada disto...que raio quer dizer o booleano?

    public String getFileName() {
        return fileName;
    }

    public boolean isSender() {
        return sender;
    }

}
