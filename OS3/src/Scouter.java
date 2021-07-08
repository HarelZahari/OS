import java.io.File;

public class Scouter implements Runnable {
    private SynchronizedQueue<File> directoryQueue;
    private File root;

    public Scouter(SynchronizedQueue<File> directoryQueue, File root) {
        this.directoryQueue = directoryQueue;
        this.root = root;
    }

    public void run() {
        this.directoryQueue.registerProducer();
        try {
            if (this.root.isDirectory()) {
                // Add root dir to queue and execute addAllSubDirectoriesToQueueRecursive
                this.directoryQueue.enqueue(this.root);
                addAllSubDirectoriesToQueueRecursive(this.root);
            }
        } catch (Exception e) {
            System.out.println("Got excepion while execute addAllSubDirectoriesToQueueRecursive method " + e.getMessage());
        }
        this.directoryQueue.unregisterProducer();
    }

    // Add all sub dirs of currentRoot File
    public void addAllSubDirectoriesToQueueRecursive(File currentRoot) {
        File[] subDirectoriesOfCurrentRoot = currentRoot.listFiles();
        for (File currentNewRoot : subDirectoriesOfCurrentRoot) {
            if (currentNewRoot.isDirectory()) {
                this.directoryQueue.enqueue(currentNewRoot);
                addAllSubDirectoriesToQueueRecursive(currentNewRoot);
            }
        }
    }
}
