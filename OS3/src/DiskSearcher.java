import java.io.File;

public class DiskSearcher {
    public static final int DIRECTORY_QUEUE_CAPACITY = 50;
    public static final int RESULTS_QUEUE_CAPACITY = 50;

    /**
     * @param args
     */
    public static void main(String[] args) {
        // Initialize input variables 
        String pattren = null;
        String extension = null;
        File rootDirectory = null;
        File destinationDirectory = null;
        int numOfSearchers = 0;
        int numOfCopiers = 0;

        // Check if got all relevant arguments
        if (args.length != 6) {
            System.err.println("You entered invalid command, to execute run in the following format: ");
            System.err.println("java DiskSearcher <Pattren> <Extension> <Root Directory> <Destintaion Directory> <# of Searchers> <# of Copiers>");
        }
        // Check if arguments are valid
        try {
            rootDirectory = new File(args[2]);
            if(!rootDirectory.exists()) {
                System.err.println("Root directory does not exists");
                return;
            }
            destinationDirectory = new File(args[3]);
        } catch (Exception e) {
            System.err.println("Error - can't create files objects");
        }
        if (!destinationDirectory.exists()) {
            if (!destinationDirectory.mkdir()) {
                System.out.println("Cant create destination directory");
                return;
            }
            try {
                pattren = args[0];
                extension = args[1];
                numOfSearchers = Integer.parseInt(args[4]);
                numOfCopiers = Integer.parseInt(args[5]);
            } catch (Exception e) {
                System.err.println("You entered invalid arguments, please try again");
            }
            // Initialize new SynchronizedQueue of files
            SynchronizedQueue<File> directoryQueue = new SynchronizedQueue<>(DIRECTORY_QUEUE_CAPACITY);
            SynchronizedQueue<File> resultsQueue = new SynchronizedQueue<>(RESULTS_QUEUE_CAPACITY);

            // Create and start scouterThread
            Thread scouterThread = new Thread(new Scouter(directoryQueue, rootDirectory));
            scouterThread.start();
            
            // Create and start searchersThreadArray and copiersThreadArray
            Thread[] searchersThreadArray = new Thread[numOfSearchers];
            Thread[] copiersThreadArray = new Thread[numOfCopiers];
            while (scouterThread.isAlive()) {
                for (int i = 0; i < searchersThreadArray.length; i++) {
                    searchersThreadArray[i] = new Thread(new Searcher(pattren, extension, directoryQueue, resultsQueue));
                    searchersThreadArray[i].start();
                }
                for (int i = 0; i < copiersThreadArray.length; i++) {
                    copiersThreadArray[i] = new Thread(new Copier(destinationDirectory, resultsQueue));
                    copiersThreadArray[i].start();
                }
            }
            // Wait for all Threads of Copiers, Searchers, Scouter to finish
            try {
                for (Thread currentCopierThread : copiersThreadArray) {
                    currentCopierThread.join();
                }
                for (Thread currentSearcherThread : searchersThreadArray) {
                    currentSearcherThread.join();
                }
                try {
                    scouterThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (InterruptedException e) {
                System.err.println("Error while waiting for all thread finish run " + e);
            }
            
            System.out.println("Finished to copy all relevant files");
        }
    }
}
