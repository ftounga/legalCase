package fr.ailegalcase.video;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SF-231-01 : configuration de l'extraction de frames vidéo via ffmpeg.
 *
 * <p>Valeurs par défaut alignées sur la mini-spec :
 * <ul>
 *   <li>{@code maxDurationSeconds=60} — vidéos > 60 s rejetées par {@code DocumentController} (V2 chunking temporel)</li>
 *   <li>{@code maxSizeMb=100} — vidéos > 100 Mo rejetées (413)</li>
 *   <li>{@code frameCount=5} — 5 frames extraites à 10/30/50/70/90% (compromis coût Vision / fidélité narrative)</li>
 *   <li>{@code ffmpegPath=/usr/bin/ffmpeg} — chemin standard sur l'image Docker backend</li>
 *   <li>{@code ffmpegTimeoutSeconds=30} — timeout strict par appel ffmpeg</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "app.video")
public class VideoFrameExtractorProperties {

    private int maxDurationSeconds = 60;
    private int maxSizeMb = 100;
    private int frameCount = 5;
    private String ffmpegPath = "/usr/bin/ffmpeg";
    private int ffmpegTimeoutSeconds = 30;

    public int getMaxDurationSeconds() { return maxDurationSeconds; }
    public void setMaxDurationSeconds(int maxDurationSeconds) { this.maxDurationSeconds = maxDurationSeconds; }

    public int getMaxSizeMb() { return maxSizeMb; }
    public void setMaxSizeMb(int maxSizeMb) { this.maxSizeMb = maxSizeMb; }

    public int getFrameCount() { return frameCount; }
    public void setFrameCount(int frameCount) { this.frameCount = frameCount; }

    public String getFfmpegPath() { return ffmpegPath; }
    public void setFfmpegPath(String ffmpegPath) { this.ffmpegPath = ffmpegPath; }

    public int getFfmpegTimeoutSeconds() { return ffmpegTimeoutSeconds; }
    public void setFfmpegTimeoutSeconds(int ffmpegTimeoutSeconds) { this.ffmpegTimeoutSeconds = ffmpegTimeoutSeconds; }

    public long getMaxSizeBytes() {
        return (long) maxSizeMb * 1024L * 1024L;
    }
}
