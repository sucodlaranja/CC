import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Scanner;

public class Interpreter implements Runnable{

    private final String firstCommandFilePath;
    private final String firstCommandIPAddress;
    private final Syncs syncs;

    public Interpreter(String filepath, String ip) {
        this.firstCommandFilePath = filepath;
        this.firstCommandIPAddress = ip;
        this.syncs = new Syncs();
    }

    private void syncStarter(String filepath, String addressStr){

        if(Files.exists(Path.of(filepath))){
            System.out.println(filepath + " is a valid path.");
            try{
                InetAddress address = InetAddress.getByName(addressStr);
                System.out.println(addressStr + " is a valid ip address.");

                // TODO: Create Handler Thread.

                // TODO: Add to Syncs map -> CONCURRENCY ALERT!
                this.syncs.createSync(filepath, address);

            }
            catch (UnknownHostException e){
                System.out.println(addressStr + " is an invalid ip address.");
            }
        }
        else
            System.out.println(filepath + " is an invalid path.");

    }

    @Override
    public void run() {
        /*
         * Interpreter goal: read user input
         * Possible types of user input:
         *  1. Start new sync.
         *  2. See list of all active syncs.
         *  3. Terminate specific sync.
         *  4. Terminate all sync's.
         *  5. Exit (includes step 4.).
         *
         * */

        // Is there a synchronization to be started?
        if(!Objects.equals(this.firstCommandFilePath, "")
                && !Objects.equals(this.firstCommandIPAddress, ""))
        {
            // TODO: Start new Sync (user passed info through command line argument).
        }

        // Start interpreter.
        Scanner scanner = new Scanner(System.in);
        String userInput;
        System.out.print("> ");
        while(!Objects.equals(userInput = scanner.nextLine(), "exit")){
            // Parse user input.
            String[] splitInput = userInput.split(" ");
            switch (splitInput[0]) {
                case "FFSync" -> {
                    System.out.println("Starting new sync.");
                    if(splitInput.length > 2) {
                        this.syncStarter(splitInput[1], splitInput[2]);
                    }
                    else
                        System.out.println("Invalid arguments. Try \"> help\"");
                }
                case "all" -> {
                    System.out.println("Showing all the active syncs.");
                    System.out.println(this.syncs);
                }
                case "terminate" -> {
                    System.out.println("Terminate all or only one?");
                }
                case "help" -> {
                    System.out.println("  FFSync <filepath> <ip>: Starts a new sync.");
                    System.out.println("  all: Shows all the active syncs.");
                    System.out.println("  terminate <sync_id>: Terminates specific sync.");
                    System.out.println("  terminate all: Terminates all syncs.");
                    System.out.println("  exit: Exits interpreter and closes all syncs.");
                }
                default -> System.out.println("Command not found. Try \"> help\"");

            }

            System.out.print("> ");
        }

        // Close Scanner.
        scanner.close();

        // TODO: Close all the non-terminated synchronizations.


    }
}
