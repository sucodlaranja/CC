/**
 * Esta classe representa os logs de um ficheiro.
 * */
public class TransferLogs {
    private final String fileName;
    private final boolean  senderOrReceiver; // true if i am sender

    public TransferLogs(String fileName, boolean senderOrReceiver){
        this.fileName = fileName;
        this.senderOrReceiver = senderOrReceiver;
    }

    public String getFileName(){ return fileName; }

    public boolean isSenderOrReceiver() {return senderOrReceiver; }

}
