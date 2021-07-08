import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Copier implements Runnable {

    public static final int COPY_BUFFER_SIZE = 4096;
    private File destination;
    private SynchronizedQueue<File> resultsQueue;

    public Copier(File destination, SynchronizedQueue<File> resultsQueue) {
        if (!destination.isDirectory()) {
            throw new IllegalArgumentException("The destination argument isn't a directory");
        }
        this.destination = destination;
        this.resultsQueue = resultsQueue;
    }

    public void run() {
        File currentFileToCopy;
        int currentReadLength;
        FileInputStream readerStream = null;
        FileOutputStream writerStream = null;
        File newFile = null;
        byte[] copyBuffer = new byte[COPY_BUFFER_SIZE];
        this.resultsQueue.registerProducer();
        while (this.resultsQueue.getSize() != 0) {
            currentFileToCopy = this.resultsQueue.dequeue();
            try {
                // Create new file on the destination location
                newFile = new File(destination, currentFileToCopy.getName());
                try {
                    readerStream = new FileInputStream(currentFileToCopy);
                    writerStream = new FileOutputStream(newFile);
                    // Copy data to newFile with buffer a
                    while ((currentReadLength = readerStream.read(copyBuffer)) > 0) {
                        writerStream.write(copyBuffer, 0, currentReadLength);
                    }

                } catch (Exception e) {
                    System.err.println("Exception been caught during copy file " + e);
                } finally {
                    try {
                        if (readerStream != null) {
                            readerStream.close();
                        }
                        if (writerStream != null) {
                            writerStream.close();
                        }
                    } catch (IOException e) {
                        System.err.println("Exception been caught during I/O stream closing" + e);
                    }
                }
            } catch (Exception e) {
                System.err.println("Exception failed to create file for copy " + e);
            }
        }
        this.resultsQueue.unregisterProducer();
    }
}
