package main.helpers;

/**
 * Represents a Lamport logical clock used for tracking events in a distributed system.
 */
public class LamportClock {
    private int time;

    /**
     * Initializes the Lamport clock with an initial time of 0.
     */
    public LamportClock() {
        this.time = 0;
    }

    /**
     * Increments the clock time due to an internal event.
     */
    public void increment() {
        time++;
    }

    /**
     * Increment the clock and return the current time.
     *
     * @return The current time after sending the message.
     */
    public int sendLCTime() {
        increment();
        return time;
    }

    /**
     * Simulates receiving a message and updates the clock time accordingly.
     *
     * @param timeReceived The timestamp received in the message.
     */
    public void receiveLCTime(int timeReceived) {
        time = Math.max(time, timeReceived) + 1;
    }

    /**
     * Gets the current time on the Lamport clock.
     *
     * @return The current time.
     */
    public int getCurrentTime() {
        return time;
    }

    /**
     * Sets the current time on the Lamport clock.
     *
     * @return The current time.
     */
    public void setCurrentTime(int time) {
        this.time = time;
    }
}
