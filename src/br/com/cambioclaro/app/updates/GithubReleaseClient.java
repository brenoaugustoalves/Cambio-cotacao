package br.com.cambioclaro.app.updates;

import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/** Owns HTTP, GitHub response validation, APK download bounds and its published digest. */
public final class GithubReleaseClient {
    private static final String API = "https://api.github.com/repos/brenoaugustoalves/Cambio-cotacao/releases/latest";
    private static final String USER_AGENT = "CambioClaro-Android";
    private static final int MAX_JSON_BYTES = 512 * 1024;
    private static final long MAX_APK_BYTES = 64L * 1024L * 1024L;

    public GithubRelease fetchLatest() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try {
            int status = connection.getResponseCode();
            if (status == 404) throw new IOException("Ainda não há uma Release publicada no GitHub.");
            if (status == 403 || status == 429) throw new IOException("GitHub limitou temporariamente as consultas. Tente mais tarde.");
            if (status != 200) throw new IOException("GitHub respondeu HTTP " + status + ".");
            return parseRelease(readBounded(connection.getInputStream(), MAX_JSON_BYTES));
        } finally {
            connection.disconnect();
        }
    }

    public static GithubRelease parseRelease(String raw) throws Exception {
        JSONObject json = new JSONObject(raw);
        if (json.optBoolean("draft", true) || json.optBoolean("prerelease", true)) {
            throw new IOException("A Release mais recente não é pública/estável.");
        }
        String tag = json.optString("tag_name", "");
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        if (GithubRelease.semanticCode(version) < 0) throw new IOException("Versão da Release inválida.");
        String expectedName = "CambioClaro-" + version + ".apk";
        JSONArray assets = json.optJSONArray("assets");
        if (assets == null) throw new IOException("A Release não contém APK.");
        for (int index = 0; index < assets.length(); index++) {
            JSONObject asset = assets.getJSONObject(index);
            if (!expectedName.equals(asset.optString("name"))) continue;
            long size = asset.optLong("size", 0);
            if (size <= 0 || size > MAX_APK_BYTES) throw new IOException("Tamanho do APK fora do limite permitido.");
            String url = asset.optString("browser_download_url", "");
            validateReleaseUrl(url, tag, expectedName);
            String digest = asset.optString("digest", "");
            if (!digest.isEmpty() && !digest.matches("sha256:[0-9a-fA-F]{64}")) {
                throw new IOException("Resumo SHA-256 da Release inválido.");
            }
            return new GithubRelease(version, url, digest.isEmpty() ? "" : digest.substring(7), tag, size);
        }
        throw new IOException("APK " + expectedName + " não encontrado nos arquivos da Release.");
    }

    public File download(File cacheDirectory, GithubRelease release) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(release.downloadUrl).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", USER_AGENT);
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Não foi possível baixar o APK do GitHub.");
            File output = new File(cacheDirectory, "CambioClaro-update.apk");
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            try (InputStream input = connection.getInputStream(); FileOutputStream file = new FileOutputStream(output)) {
                byte[] buffer = new byte[16384];
                int count;
                long total = 0;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_APK_BYTES) throw new IOException("Download excedeu o tamanho permitido.");
                    sha256.update(buffer, 0, count);
                    file.write(buffer, 0, count);
                }
                file.getFD().sync();
            }
            if (output.length() != release.apkBytes) {
                output.delete();
                throw new IOException("O tamanho baixado não corresponde ao arquivo publicado pelo GitHub.");
            }
            if (!release.sha256.isEmpty() && !release.sha256.equalsIgnoreCase(hex(sha256.digest()))) {
                output.delete();
                throw new IOException("O SHA-256 do APK não confere com a Release.");
            }
            return output;
        } finally {
            connection.disconnect();
        }
    }

    private static void validateReleaseUrl(String value, String tag, String asset) throws IOException {
        Uri uri = Uri.parse(value);
        String path = "/brenoaugustoalves/Cambio-cotacao/releases/download/" + tag + "/" + asset;
        if (!"https".equals(uri.getScheme()) || !"github.com".equals(uri.getHost())
                || !path.equals(uri.getPath()) || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IOException("Endereço do APK não corresponde à Release oficial do projeto.");
        }
    }

    private static String readBounded(InputStream input, int maximum) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                if (output.size() + count > maximum) throw new IOException("Resposta do GitHub grande demais.");
                output.write(buffer, 0, count);
            }
            return output.toString("UTF-8");
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for (byte item : bytes) value.append(String.format(Locale.ROOT, "%02x", item & 255));
        return value.toString();
    }
}
