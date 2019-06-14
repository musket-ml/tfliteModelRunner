package com.onpositive.dldemos;

public class ProgressEvent {
    private boolean isCanceled = false;
    private int progressInPercent;
    private int expectedMinutes;

    public ProgressEvent(int progressInPercent, int expectedMinutes) {
        this.progressInPercent = progressInPercent;
        this.expectedMinutes = expectedMinutes;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void setCanceled(boolean canceled) {
        isCanceled = canceled;
    }

    public int getExpectedMinutes() {
        return expectedMinutes;
    }

    public void setExpectedMinutes(int expectedMinutes) {
        this.expectedMinutes = expectedMinutes;
    }

    public int getProgressInPercent() {
        return progressInPercent;
    }

    public void setProgressInPercent(int progressInPercent) {
        this.progressInPercent = progressInPercent;
    }
}
