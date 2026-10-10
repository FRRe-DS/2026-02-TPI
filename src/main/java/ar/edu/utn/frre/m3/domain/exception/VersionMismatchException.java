package ar.edu.utn.frre.m3.domain.exception;

public class VersionMismatchException extends RuntimeException {
    private final long currentVersion;
    private final long expectedVersion;

    public VersionMismatchException(long currentVersion, long expectedVersion) {
        super(String.format("If-Match version mismatch: expected %d, but current version is %d",
                expectedVersion, currentVersion));
        this.currentVersion = currentVersion;
        this.expectedVersion = expectedVersion;
    }

    public long getCurrentVersion() {
        return currentVersion;
    }

    public long getExpectedVersion() {
        return expectedVersion;
    }
}
