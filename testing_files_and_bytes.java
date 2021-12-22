
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to send DATA packets. DATA packets can contain a portion of a file, a Log or a Guide.
 * To send a file we only need its path. The path must be given to the constructor.
 * To send a Log or a File, a byte[] must be given to the constructor.
 *
 * */
public class testing_files_and_bytes {

    public static final int DATA = 4;
    public static final int PACKET_SIZE = 512;
    public static final int DATA_HEADER_SIZE = (DATA + "@" + DATA + "@").getBytes(StandardCharsets.UTF_8).length; // 2 char's + 2 int's.
    public static final int DATA_CONTENT_SIZE = PACKET_SIZE - DATA_HEADER_SIZE;



    public static void main(String[] args) {
        // Reading file to memory.
        String filepathOriginal = "/home/rubensas/Desktop/tp2-folder3/wireshark.tar.xz";
        byte[] data = null;
        try {
            data = Files.readAllBytes(Paths.get(filepathOriginal));
        } catch (OutOfMemoryError e) {
            System.out.println("File is to big to be transferred.");
            e.printStackTrace();
        } catch (NoSuchFileException e) {
            System.out.println("File " + filepathOriginal + " was not found.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (data == null)
            return;


        // Split file into byte chunks.
        List<byte[]> splittedData = split(data);
        System.out.println("Number of packet = " + splittedData.size());
        System.out.println("Packet Size = " + splittedData.get(0).length);
        System.out.println("Last Packet Size = " + splittedData.get(splittedData.size() - 1).length);
        System.out.println("Filesize = " + data.length + " bytes.");


        // Save the file
        String filepathSave = "/home/rubensas/Desktop/test.tar.xz";
        byte[] collapsedData = collapse(splittedData, splittedData.get(splittedData.size() - 1).length);
        try {
            // Take name of the file out of filepath. Filepath is the full path of the file.
            StringBuilder finalDirectoryPath = new StringBuilder("/");
            String[] splitFilepath = filepathSave.split("/");
            for (int i = 0; i < splitFilepath.length - 1; ++i)
                finalDirectoryPath.append(splitFilepath[i]).append("/");

            Files.createDirectories(Paths.get(finalDirectoryPath.toString()));

            new FileOutputStream(filepathSave).write(collapsedData, 0, collapsedData.length);
        }
        catch (FileNotFoundException e){
            System.out.println("Receiver: " + filepathSave + " is not a valid filepath.");
        }
        catch (IOException e){
            e.printStackTrace();
        }



        System.out.println("BYE");
    }

    // Split byte[] into list. Packet Size defined in FTRapidPacket class.
    private static List<byte[]> split(byte[] data) {
        // Size of each packet.

        // Number of packets
        int nPackets = (int) Math.ceil((double) data.length / DATA_CONTENT_SIZE);

        // Create and fill list to be returned.
        List<byte[]> retList = new ArrayList<>(nPackets);
        int i;
        for (i = 0; i < nPackets - 1; i++) {
            retList.add(i, new byte[DATA_CONTENT_SIZE]);
            System.arraycopy(data, i * DATA_CONTENT_SIZE, retList.get(i), 0, retList.get(i).length);
        }

        // Create last packet - usually with a different size.
        int lastPacketSize = data.length - (nPackets-1) * DATA_CONTENT_SIZE;
        retList.add(i, new byte[lastPacketSize]);
        System.arraycopy(data, i * DATA_CONTENT_SIZE, retList.get(i), 0, retList.get(i).length);

        return retList;
    }

    private static byte[] collapse(List<byte[]> packetsSplit, int LAST_PACKET_DATA_SIZE) {
        byte[] ret = new byte[(packetsSplit.size() - 1) * DATA_CONTENT_SIZE + LAST_PACKET_DATA_SIZE];

        int i;
        for(i = 0; i < packetsSplit.size() - 1; ++i)
            System.arraycopy(packetsSplit.get(i), 0, ret, i * DATA_CONTENT_SIZE, DATA_CONTENT_SIZE);

        int offset = i * DATA_CONTENT_SIZE;
        byte[] buf = packetsSplit.get(i);
        for(int k = offset, j = 0; k < offset + LAST_PACKET_DATA_SIZE; ++k, j++)
            ret[k] = buf[j];

        return ret;
    }
}
