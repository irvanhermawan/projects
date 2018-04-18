package id.co.projectscoid.push;

import id.co.projectscoid.push.NonSuccessfulResponseCodeException;

public class LockedException extends NonSuccessfulResponseCodeException {

    private int  length;
    private long timeRemaining;

    LockedException(int length, long timeRemaining) {
        this.length        = length;
        this.timeRemaining = timeRemaining;
    }

    public int getLength() {
        return length;
    }

    public long getTimeRemaining() {
        return timeRemaining;
    }
}
