/*
 * Created by Koen Deschacht (koendeschacht@gmail.com) 2017-3-17. For license
 * information see the LICENSE file in the root folder of this repository.
 */

package be.bagofwords.memory;

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
