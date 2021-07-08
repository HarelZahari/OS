import java.io.File;
import java.io.FilenameFilter;

public class Searcher implements Runnable {

    private String pattren;
    private String extension;
    private SynchronizedQueue<File> directoryQueue;
    private SynchronizedQueue<File> resultsQueue;

    public Searcher(String pattren, String extension, SynchronizedQueue<File> directoryQueue, SynchronizedQueue<File> resultsQueue) {
        this.pattren = pattren;
        this.extension = extension;
        this.directoryQueue = directoryQueue;
        this.resultsQueue = resultsQueue;
    }

    // Check if inputFile finish with this.extension
    private boolean isFileExtension(File inputFile) {
        String fileName = inputFile.getName();
        String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
        return this.extension.equals(fileExtension);
    }
    // Check if inputFile contains this.pattren
    private boolean isFileNameContainString(File inputFile) {
        String fileName = inputFile.getName();
        String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
        return fileNameWithoutExtension.contains(this.pattren);
    }

    public void run() {
        File currentDirectory;
        File currentFileArray[] = null;
        this.directoryQueue.registerProducer();
        this.resultsQueue.registerProducer();
        while (this.directoryQueue.getSize() != 0) {
            currentDirectory = this.directoryQueue.dequeue();
            // Create array of all files that fulfill the conditions
            currentFileArray = currentDirectory.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    File file = new File(dir, name);
                    return isFileExtension(file) && isFileNameContainString(file) && file.isFile();
                }
            });
            // Update result queue
            for (File currentFile : currentFileArray) {
                resultsQueue.enqueue(currentFile);
            }
        }
        this.resultsQueue.unregisterProducer();
        this.directoryQueue.unregisterProducer();
        if (currentFileArray == null) {
            return;
        }
    }

}
