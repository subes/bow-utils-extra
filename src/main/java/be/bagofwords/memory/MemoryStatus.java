package be.bagofwords.memory;

/**
 * Created by Koen Deschacht (koendeschacht@gmail.com) on 9/9/14.
 */
public enum MemoryStatus {

    FREE(0.0), SOMEWHAT_LOW(0.8), LOW(0.85), CRITICAL(0.90);

    private double minMemoryUsage;

    MemoryStatus(double minMemoryUsage) {
        this.minMemoryUsage = minMemoryUsage;
    }

    public double getMinMemoryUsage() {
        return minMemoryUsage;
    }
}
