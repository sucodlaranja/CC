package HTTP;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import HistoryRecorder.TransferHistory;

///This class is responsable for the http server.
/**
 * This class creates, controls and maintains the http Server, implements \b Runnable so that we can run this code in a separated thread. \n
 * Contains one constructor \b HTTPServer(int port) that initializes the serversocket, and creates the \ref HTTP_FILEPATH directory. \n
 * This server is running alongside it the program, receives requests from clients and gives information about the \b syncs that are happening. \n
 * The server has a Main Menu that contains all the names of the folders that are in sync, the client have the option to click in one of the names,
 * and will be redirected to a new page, that contains all files from that sync with some information about each one.
 */

public class HTTPServer implements Runnable {

    
    private final ServerSocket serverSocket; ///< ServerSocket for the HTTP server.
    private final int port; ///< Port that the http server is listening on.
    public static String HTTP_FILEPATH = "HistorySaved"; ///< Path that has files with info of all syncs.

    /**
     * Constructor that receives the \ref port that the HTTP server will listen
     * on.\n Initializes the \b serverSocket with the given port, and
     * verifies \n if the directory that will store all info about the syncs exists,
     * if so \n deletes it.\N Finnaly creates a directory in \b HTTP_FILEPATH.
     * 
     * @param port \ref port
     * 
     * @throws IOException IO Exception.
     */
    public HTTPServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);

        if (Files.exists(Paths.get(HTTP_FILEPATH))) {
            try {
                Files.list(Paths.get(HTTP_FILEPATH)).map(Path::toFile).forEach(File::delete);
                Files.delete(Paths.get(HTTP_FILEPATH));
            } catch (IOException ignored) {
            }
        }

        Files.createDirectory(Paths.get(HTTP_FILEPATH));
    }

    /**
     * This method contains a loop that receives HTTP requests, \n
     * accepts a connection with the client, reads all content in connection \b InputStream. \n
     * Isolates the get request, splits the argument and redirects to \ref getHandler method.\n
     * That is responsable for creating the html code that will be sent by \b outputstream.
     * Maintains in the loop until the program shuts down and the \ref closeServer is called.
     * 
     */
    public void run() {
        boolean closed = true;
        while (!serverSocket.isClosed() && closed) {
            try {
                Socket clientSocket = serverSocket.accept();

                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String s;
                String[] newString;
                String[] control = null;
                while ((s = in.readLine()) != null && !s.isEmpty()) {
                    newString = s.split(" ");

                    if (newString[0] != null && newString[0].equals("GET")) {
                        control = newString.clone();
                    }

                }

                if (control != null) {
                    OutputStream out = clientSocket.getOutputStream();
                    out.write("HTTP/1.1 200 OK\r\n".getBytes());
                    out.write("\r\n".getBytes());
                    if (s != null)
                        getHandler(control[1], out); // handler
                    out.write("\r\n\r\n".getBytes());

                    out.flush();
                    out.close();
                }

                in.close();
            } catch (SocketException e) {
                closed = false;
            } catch (IOException ignored) {
            }
        }

        try {
            Files.list(Paths.get(HTTP_FILEPATH)).map(Path::toFile).forEach(File::delete);
            Files.delete(Paths.get(HTTP_FILEPATH));
        } catch (IOException ignored) {
        }

        // Termination message.
        System.err.println("HTTP server terminated.");
    }

    /// Handles all get requests from the user.
    /**
     * Receives the get request argument, verifies if the arguments is simply "/",
     * if so, calls \ref mainMenu that creates the Main menu.\n
     * If the arguments has more than "/", it fabricates and inserts html code for
     * the given argument, starts by creating the title for the page,\n
     * creates a \b back "button", that traces back to \b Main Menu, and displays all
     * info about the given arguments with the help of \ref getSync method.
     * 
     * @param argument argument for the get request.
     * @param out      outputstream from connection.
     * @throws IOException Happens when an error occurs while reading files in \ref getallSyncs that is called in \ref mainMenu.
     */
    public void getHandler(String argument, OutputStream out) throws IOException {
        String[] splitArgument = argument.split("/");

        if (argument.equals("/") || argument.equals("/favicon.ico")) {
            mainMenu(out);
        } else if (splitArgument.length == 2) {
            out.write(("<h1>" + splitArgument[1] + "</h1>").getBytes());
            out.write(("<a href=\"http://localhost:" + port + "\">back</a>").getBytes());
            out.write((getSync(HTTP_FILEPATH + "/" + splitArgument[1])).getBytes());
        }

    }

    /**
     * Auxiliar method to use in \ref mainMenu so it can fabricate the html
     * code for the http main menu.
     * 
     * @return returns a set with all folder's names inside the \ref HTTP_FILEPATH
     *         
     * @throws IOException
     *  Happens when an error occurs while reading files inside \ref HTTP_FILEPATH 
     */
    public Set<String> getAllSyncs() throws IOException {
        Path folder = Paths.get(HTTP_FILEPATH);
        Set<String> files = new HashSet<>();
        for (Path file : Files.list(folder).toList()) {
            files.add(file.getFileName().toString());

        }
        return files;
    }

    /**
     * Method that receives a \b filename, this \b filename represents the folder in
     * sync that the user wants to visualize, \n
     * the return string will have all files syncing inside that directory, it will
     * have information about the \n date of last tranfer, \n
     * time and Kbps per second the last tranfer took.
     * 
     * @return returns a string with all html code to display.
     */
    public String getSync(String filename) {
        try {
            TransferHistory history = new TransferHistory(filename);
            return history.toHTML();
        } catch (IOException | ClassNotFoundException e) {
            return "<h2>Information about this sync does not exist</h2>";
        }

    }

    /**
     *
     * Constructs the mainMenu of the HTTP server.\n Obtains all the files that are
     * syncing from \ref getAllSyncs, uses this set to \n
     * fabricates and insert html code in the outputstream \b out.
     * 
     * @param out outputstream from connection
     */
    public void mainMenu(OutputStream out) throws IOException {
        Set<String> files = getAllSyncs();
        out.write("<b><h1>Menu Principal</h1></b>".getBytes());
        for (String entry : files) {
            out.write(
                    ("<h2><a href=\"http://localhost:" + port + "/" + entry + "\">" + entry + "</a></h2>").getBytes());
        }
    }

    /**
     * Closes the \ref serverSocket when the program is shutting down.
     */
    public void closeServer() {
        try {
            this.serverSocket.close();
        } catch (IOException ignored) {
        }
    }
}
