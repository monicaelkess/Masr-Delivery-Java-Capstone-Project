package com.masrdelivery.config;

import com.masrdelivery.money.Money;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Platform configuration, loaded exactly once and immutable afterwards.
 *
 * Singleton via the initialization-on-demand holder idiom: the JVM guarantees the
 * Holder class is initialised once, lazily, and safely published to every thread,
 * with no synchronized/volatile code of our own. Because every field is final and
 * the object never changes, concurrent readers (Part E) need no locking at all.
 *
 * Testability caveat (see DESIGN.md G.5): only the composition root (Main) calls
 * getInstance(). Services receive a PlatformConfig through their constructor, so tests
 * can build an isolated one with {@link #fromProperties(Properties)}.
 */
public final class PlatformConfig {

    public static final String DEFAULT_FILE = "masr-delivery.properties";

    private final Money baseDeliveryFee;
    private final Money perKmFee;
    private final BigDecimal freeKm;
    private final BigDecimal serviceFeePercent;
    private final int workerPoolSize;
    private final Path menuFile;
    private final Path auditLogFile;
    private final DistrictDistances distances;

    private PlatformConfig(Properties p) {
        baseDeliveryFee   = Money.of(p.getProperty("delivery.baseFee", "15"));
        perKmFee          = Money.of(p.getProperty("delivery.perKmFee", "3"));
        freeKm            = new BigDecimal(p.getProperty("delivery.freeKm", "3"));
        serviceFeePercent = new BigDecimal(p.getProperty("service.feePercent", "10"));
        workerPoolSize    = Integer.parseInt(p.getProperty("worker.poolSize", "4"));
        menuFile          = Path.of(p.getProperty("data.menuFile", "data/menu.csv"));
        auditLogFile      = Path.of(p.getProperty("audit.logFile", "logs/audit.log"));
        distances         = DistrictDistances.defaults();
        if (workerPoolSize <= 0) throw new IllegalStateException("worker.poolSize must be positive");
    }

    private static final class Holder {
        private static final PlatformConfig INSTANCE = load();
    }

    public static PlatformConfig getInstance() { return Holder.INSTANCE; }

    /** Isolated configuration for tests and tools; does NOT touch the singleton. */
    public static PlatformConfig fromProperties(Properties properties) { return new PlatformConfig(properties); }
    public static PlatformConfig defaults() { return new PlatformConfig(new Properties()); }

    private static PlatformConfig load() {
        Properties p = new Properties();
        Path file = Path.of(System.getProperty("masr.config", DEFAULT_FILE));
        try {
            if (Files.isRegularFile(file)) {
                try (Reader r = Files.newBufferedReader(file)) { p.load(r); }
            } else {
                try (InputStream in = PlatformConfig.class.getResourceAsStream("/" + DEFAULT_FILE)) {
                    if (in != null) p.load(in);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read configuration " + file, e);
        }
        return new PlatformConfig(p);
    }

    public Money baseDeliveryFee()       { return baseDeliveryFee; }
    public Money perKmFee()              { return perKmFee; }
    public BigDecimal freeKm()           { return freeKm; }
    public BigDecimal serviceFeePercent(){ return serviceFeePercent; }
    public int workerPoolSize()          { return workerPoolSize; }
    public Path menuFile()               { return menuFile; }
    public Path auditLogFile()           { return auditLogFile; }
    public DistrictDistances distances() { return distances; }
}
