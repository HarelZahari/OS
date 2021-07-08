/**
 * A synchronized bounded-size queue for multithreaded producer-consumer
 * applications.
 * 
 * @param <T>
 *            Type of data items
 */
public class SynchronizedQueue<T> {
    private T[] buffer;
    private int producers;
    private int bufferSize;
    private int bufferStartIndex;
    private int bufferEndIndex;
    // TODO: Add more private members here as necessary

    /**
     * Constructor. Allocates a buffer (an array) with the given capacity and resets
     * pointers and counters.
     * 
     * @param capacity
     *            Buffer capacity
     */
    @SuppressWarnings("unchecked")
    public SynchronizedQueue(int capacity) {
        this.buffer = (T[]) (new Object[capacity]);
        this.producers = 0;
        this.bufferSize = 0;
        this.bufferStartIndex = 0;
        this.bufferEndIndex = -1;
    }

    /**
     * Dequeues the first item from the queue and returns it. If the queue is empty
     * but producers are still registered to this queue, this method blocks until
     * some item is available. If the queue is empty and no more items are planned
     * to be added to this queue (because no producers are registered), this method
     * returns null.
     * 
     * @return The first item, or null if there are no more items
     * @see #registerProducer()
     * @see #unregisterProducer()
     */
    public synchronized T dequeue() {
        T itemForDequeue = null;
        while (this.bufferSize == 0) {
            if (this.producers == 0) {
                return itemForDequeue;
            }
            try {
                if (this.producers != 0) {
                    // SynchronizedQueue is empty,current thread need to wait, and notifyAll to wake up threads that may need enqueue
                    this.notifyAll();
                    this.wait();
                }
            } catch (InterruptedException e) {
                System.out.println("Got error while waiting for queue to fill " + e.getMessage());
                this.notifyAll();
            }
        }
        // Take out from SynchronizedQueue current data and update indexes
        this.bufferStartIndex = this.bufferStartIndex % getCapacity();
        itemForDequeue = this.buffer[this.bufferStartIndex];
        this.bufferStartIndex = (this.bufferStartIndex + 1) % getCapacity();
        this.bufferSize--;
        this.notifyAll();
        return itemForDequeue;
    }

    /**
     * Enqueues an item to the end of this queue. If the queue is full, this method
     * blocks until some space becomes available.
     * 
     * @param item
     *            Item to enqueue
     */
    public synchronized void enqueue(T item) {
        while (getSize() == getCapacity()) {
            try {
                if (this.producers != 0) {
                 // SynchronizedQueue is full,current thread need to wait, and notifyAll to wake up threads that may need dequeue
                    this.notifyAll();
                }
                this.wait();
            } catch (InterruptedException e) {
                System.out.println("Got error while waiting for queue to get empty " + e.getMessage());
                return;
            }
        }
        // Put in SynchronizedQueue current data and update indexes
        this.bufferEndIndex = (this.bufferEndIndex + 1) % getCapacity();
        this.buffer[this.bufferEndIndex] = item;
        this.bufferSize++;
        this.notifyAll();
    }

    /**
     * Returns the capacity of this queue
     * 
     * @return queue capacity
     */
    public int getCapacity() {
        return this.buffer.length;
    }

    /**
     * Returns the current size of the queue (number of elements in it)
     * 
     * @return queue size
     */
    public synchronized int getSize() {
        return this.bufferSize;
    }

    /**
     * Registers a producer to this queue. This method actually increases the
     * internal producers counter of this queue by 1. This counter is used to
     * determine whether the queue is still active and to avoid blocking of consumer
     * threads that try to dequeue elements from an empty queue, when no producer is
     * expected to add any more items. Every producer of this queue must call this
     * method before starting to enqueue items, and must also call
     * <see>{@link #unregisterProducer()}</see> when finishes to enqueue all items.
     * 
     * @see #dequeue()
     * @see #unregisterProducer()
     */
    public synchronized void registerProducer() {
        this.producers++;
    }

    /**
     * Unregisters a producer from this queue. See
     * <see>{@link #registerProducer()}</see>.
     * 
     * @see #dequeue()
     * @see #registerProducer()
     */
    public synchronized void unregisterProducer() {
        if (this.producers > 0) {
            this.producers--;
        }
    }
}
