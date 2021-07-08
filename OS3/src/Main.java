import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        List<String> lines = getLinesFromFile();
        System.out.println("Number of lines found: " + lines.size());
        System.out.println("Starting to process");

        long startTimeWithoutThreads = System.currentTimeMillis();
        workWithoutThreads(lines);
        long elapsedTimeWithoutThreads = (System.currentTimeMillis() - startTimeWithoutThreads);
        System.out.println("Execution time: " + elapsedTimeWithoutThreads);

        long startTimeWithThreads = System.currentTimeMillis();
        workWithThreads(lines);
        long elapsedTimeWithThreads = (System.currentTimeMillis() - startTimeWithThreads);
        System.out.println("Execution time: " + elapsedTimeWithThreads);
    }

    private static void workWithThreads(List<String> lines) {
        // Get the number of available cores
        // Assuming X is the number of cores - Partition the data into x data sets
        // Create a fixed thread pool of size X
        // Submit X workers to the thread pool
        // Wait for termination
        int x = Runtime.getRuntime().availableProcessors();
        // Create threads pool
        ExecutorService executorPool = Executors.newFixedThreadPool(x);
        // Calculate listGapSize and linesRemainder for last subList
        int listGapSize = lines.size() / x;
        int linesRemainder=lines.size() % x;
        // Create new Worker threads with the relevant subList
        for (int i = 0; i < x; i++) {
            if(i==x-1) {
                executorPool.submit(new Worker(lines.subList(i * listGapSize, ((i+1) * listGapSize) +linesRemainder)));
            }else {
                executorPool.submit(new Worker(lines.subList(i * listGapSize, ((i+1) * listGapSize))));
            }
        }
        // Close threads pool
        executorPool.shutdown();
        try {
            if (!executorPool.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorPool.shutdownNow();
            } 
        } catch (InterruptedException e) {
            executorPool.shutdownNow();
        }
    }

    private static void workWithoutThreads(List<String> lines) {
        Worker worker = new Worker(lines);
        worker.run();
    }

    private static List<String> getLinesFromFile() {
        // Read the shakespeare file provided from C:\Temp\Shakespeare.txt
        // and return an ArrayList<String> that contains each line read from the file.
        ArrayList<String> fileLinesArrayList = new ArrayList<String>();
        BufferedReader fileBufferReader=null;
        String currentLine;

        try {
            fileBufferReader = new BufferedReader(new FileReader("C:\\Temp\\Shakespeare.txt"));
            while ((currentLine = fileBufferReader.readLine()) != null) {
                fileLinesArrayList.add(currentLine);
            }
        } catch (Exception e) {
            System.err.println("Error while create list of file lines " + e);
        }finally {
            try {
                fileBufferReader.close();
            } catch (IOException e) {
                System.err.println("Error while close buffer reader " + e);
            }
        }
        return fileLinesArrayList;
    }
}
