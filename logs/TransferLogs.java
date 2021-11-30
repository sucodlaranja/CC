/**
 * Esta classe representa os logs de um ficheiro.
 * */
public class TransferLogs {
    private final String fileName;
    private final boolean  senderOrReceiver;
    private int stateOfTransfer;

    public TransferLogs(String fileName, boolean senderOrReceiver, int stateOfTransfer){
        this.fileName = fileName;
        this.senderOrReceiver = senderOrReceiver;
        this.stateOfTransfer = stateOfTransfer;
    }

    public String getFileName(){ return fileName; }

    public boolean isSenderOrReceiver() {return senderOrReceiver; }

    public int getStateOfTransfer() { return stateOfTransfer; }

    public void setStateOfTransfer(int stateOfTransfer){ this.stateOfTransfer = stateOfTransfer; }

}
