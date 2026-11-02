package br.com.cambioclaro.app.updates;

/** Immutable metadata for a stable app release published by the official repository. */
public final class GithubRelease {
    public final String version;
    public final String downloadUrl;
    public final String sha256;
    public final String tag;
    public final long apkBytes;

    GithubRelease(String version, String downloadUrl, String sha256, String tag, long apkBytes) {
        this.version = version;
        this.downloadUrl = downloadUrl;
        this.sha256 = sha256;
        this.tag = tag;
        this.apkBytes = apkBytes;
    }

    public boolean isNewerThan(String installedVersion) {
        return semanticCode(version) > semanticCode(installedVersion);
    }

    public static int semanticCode(String version) {
        if (version == null || !version.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) return -1;
        try {
            String[] parts = version.split("\\.");
            return Integer.parseInt(parts[0]) * 1000000
                    + Integer.parseInt(parts[1]) * 1000
                    + Integer.parseInt(parts[2]);
        } catch (NumberFormatException invalid) {
            return -1;
        }
    }
}
