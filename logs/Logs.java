import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Esta classe representa os logs de uma determinada pasta
 * e todas as operações que podemos fazer sobre eles.
 * */
public class Logs {

    private final String filepath; // Lugar onde está a pasta
    private Map<String, FileTime> logs; // contém um map com os nomes dos ficheiros com os seus timestamps

    public static final int Waiting = 0;
    public static final int InProgress = 1;
    public static final int Completed = 2;

    public static final int None = -1;
    public static final int Receiver = 0;
    public static final int Sender = 1;

    public Logs(String filepath) throws IOException {
        this.filepath = filepath;
        logs = new HashMap<>();

        insertFileLogs(Paths.get(filepath),"");
        }

    private boolean insertFileLogs(Path folder, String prePath) throws IOException {
        boolean update = false;
        for(Path file: Files.list(folder).collect(Collectors.toList())) {
            if (Files.isDirectory(file)) insertFileLogs(file,prePath + file.getFileName().toString() + "/");
            else {
                BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
                String filename = prePath + file.getFileName().toString();
                FileTime fl = attr.lastModifiedTime();
                if (!logs.containsKey(filename)) logs.put(filename, attr.lastModifiedTime());
                if (fl.compareTo(logs.get(filename)) != 0) {
                    logs.put(filename, attr.lastModifiedTime());
                    update = true;
                }
            }
        }
        return update;
    }

    public boolean updateFileLogs() throws IOException {
        return this.insertFileLogs(Paths.get(filepath),"");
    }

    public Map<String, FileTime> getLogs(){
        return new HashMap<>(logs);
    }

    public List<Triplet<String,Integer,Integer>> compareLogs(Map<String, FileTime> otherLogs){
        List<Triplet<String,Integer,Integer>> listOfTransfers = new ArrayList<>();

        for(Map.Entry<String, FileTime> file:  this.logs.entrySet()){
            int mode = None;
            if (otherLogs.containsKey(file.getKey())){
                int comp = otherLogs.remove(file.getKey()).compareTo(file.getValue());
                if (comp > 0) mode = Receiver;
                else if (comp < 0) mode = Sender;
            }
            else mode = Sender;

            if (mode != None) listOfTransfers.add(new Triplet<>(file.getKey(), mode, Waiting));
        }
        for(String fileName:  otherLogs.keySet()) listOfTransfers.add(new Triplet<>(fileName, Receiver, Waiting));

        return listOfTransfers;
    }

    public void printTransfers(List<Triplet<String,Integer,Integer>> listOfTransfers){
        for (Triplet<String,Integer,Integer> t: listOfTransfers){
            System.out.println( t.getFirst() + " " + t.getSecond() + " " + t.getThird());
        }
    }

    public void printLogs(){
        logs.forEach((key, value) -> System.out.println(key + " -> " + value.toString()));
    }
}

