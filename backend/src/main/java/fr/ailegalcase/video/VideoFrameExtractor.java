package fr.ailegalcase.video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * SF-231-01 : extrait N frames clés d'une vidéo via ffmpeg (CLI, ProcessBuilder).
 *
 * <p>Choix architectural — ProcessBuilder plutôt que JavaCV :
 * <ul>
 *   <li>JavaCV embarque ~150 Mo de natives + risques d'incompatibilités OS</li>
 *   <li>ffmpeg CLI est un binaire stable et léger (~50 Mo dans l'image Docker)</li>
 *   <li>L'isolation processus protège la JVM en cas de crash sur vidéo corrompue</li>
 * </ul>
 *
 * <p>Format de sortie : PNG (préservation de la fidélité visuelle pour Claude Vision).
 *
 * <p>Cleanup : tous les fichiers temporaires créés par cette classe sont systématiquement
 * supprimés via try-finally (les frames PNG sont lus en mémoire puis effacés du disque).
 */
@Component
public class VideoFrameExtractor {

    private static final Logger log = LoggerFactory.getLogger(VideoFrameExtractor.class);

    /** Timestamps en pourcentage de la durée — préfère 10/30/50/70/90 plutôt que 0/25/50/75/100
     *  pour éviter les frames noires de transition souvent présentes en début / fin de vidéo. */
    private static final int[] FRAME_PERCENTS = {10, 30, 50, 70, 90};

    private final VideoFrameExtractorProperties props;

    public VideoFrameExtractor(VideoFrameExtractorProperties props) {
        this.props = props;
    }

    /**
     * Extrait {@code frameCount} frames PNG d'une vidéo. La durée est lue depuis ffprobe
     * (ou via {@link #extractFrames(File, double)} si déjà connue côté caller).
     *
     * @param videoFile fichier vidéo source (mp4 / mov)
     * @return liste des bytes PNG, dans l'ordre temporel
     * @throws VideoExtractionException si ffmpeg échoue ou time out
     */
    public List<byte[]> extract5Frames(File videoFile) {
        double durationSeconds = probeDurationSeconds(videoFile);
        return extractFrames(videoFile, durationSeconds);
    }

    /**
     * Variante avec durée fournie explicitement (évite un appel ffprobe supplémentaire
     * quand le caller a déjà la durée — par exemple via le header HTTP {@code X-Video-Duration-Seconds}).
     */
    public List<byte[]> extractFrames(File videoFile, double durationSeconds) {
        if (videoFile == null || !videoFile.exists()) {
            throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                    "Video file does not exist");
        }
        if (durationSeconds <= 0) {
            throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                    "Invalid video duration: " + durationSeconds);
        }

        String runId = UUID.randomUUID().toString();
        List<File> tmpFrames = new ArrayList<>(props.getFrameCount());
        try {
            int frameCount = Math.min(props.getFrameCount(), FRAME_PERCENTS.length);
            for (int i = 0; i < frameCount; i++) {
                double timestamp = durationSeconds * FRAME_PERCENTS[i] / 100.0;
                File outFile = new File(System.getProperty("java.io.tmpdir"),
                        "legalcase-video-" + runId + "-frame-" + i + ".png");
                tmpFrames.add(outFile);
                runFfmpegFrameExtraction(videoFile, timestamp, outFile);
            }

            List<byte[]> result = new ArrayList<>(tmpFrames.size());
            for (File frame : tmpFrames) {
                if (!frame.exists() || frame.length() == 0) {
                    throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                            "ffmpeg did not produce frame: " + frame.getName());
                }
                result.add(Files.readAllBytes(frame.toPath()));
            }
            return result;
        } catch (IOException e) {
            throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                    "Failed to read frame bytes: " + e.getMessage(), e);
        } finally {
            for (File frame : tmpFrames) {
                deleteQuietly(frame.toPath());
            }
        }
    }

    /**
     * Probe la durée totale de la vidéo via ffprobe-like — on utilise ffmpeg avec
     * {@code -i} et on parse le {@code stderr} pour récupérer "Duration: HH:MM:SS.ms".
     * Cette méthode est exposée package-private pour testabilité.
     */
    double probeDurationSeconds(File videoFile) {
        ProcessBuilder pb = new ProcessBuilder(props.getFfmpegPath(), "-i", videoFile.getAbsolutePath());
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            String output = readAll(p.getInputStream());
            boolean finished = p.waitFor(props.getFfmpegTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_TIMEOUT,
                        "ffmpeg probe timeout after " + props.getFfmpegTimeoutSeconds() + "s");
            }
            // ffmpeg sans output flag exit en non-zero, c'est attendu : la sortie reste utilisable.
            return parseDurationFromFfmpegOutput(output);
        } catch (IOException e) {
            throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                    "ffmpeg probe failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                    "ffmpeg probe interrupted", e);
        }
    }

    /**
     * Parse la chaîne {@code Duration: HH:MM:SS.ms} dans la sortie ffmpeg.
     * Retourne -1 si non trouvé (ce qui sera traité comme erreur côté caller).
     * Package-private pour testabilité.
     */
    static double parseDurationFromFfmpegOutput(String output) {
        if (output == null) return -1;
        // Pattern : "Duration: 00:00:12.04," (le préfixe peut comporter d'autres espaces).
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("Duration:\\s*(\\d+):(\\d+):(\\d+(?:\\.\\d+)?)").matcher(output);
        if (!m.find()) {
            throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                    "Could not parse Duration from ffmpeg output");
        }
        int hours = Integer.parseInt(m.group(1));
        int minutes = Integer.parseInt(m.group(2));
        double seconds = Double.parseDouble(m.group(3));
        return hours * 3600.0 + minutes * 60.0 + seconds;
    }

    private void runFfmpegFrameExtraction(File videoFile, double timestamp, File outFile) {
        // Ordre des arguments : "-ss <timestamp> -i <input>" → seek rapide (input seek) puis read.
        ProcessBuilder pb = new ProcessBuilder(
                props.getFfmpegPath(),
                "-ss", String.format(java.util.Locale.ROOT, "%.3f", timestamp),
                "-i", videoFile.getAbsolutePath(),
                "-frames:v", "1",
                "-y",
                outFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        try {
            Process p = pb.start();
            // On consomme la sortie pour ne pas bloquer ffmpeg (ring buffer plein).
            String output = readAll(p.getInputStream());
            boolean finished = p.waitFor(props.getFfmpegTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_TIMEOUT,
                        "ffmpeg frame extraction timeout after " + props.getFfmpegTimeoutSeconds() + "s");
            }
            int exit = p.exitValue();
            if (exit != 0) {
                log.warn("ffmpeg exited with code {} for frame at {}s — output: {}", exit, timestamp,
                        truncateForLog(output));
                throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                        "ffmpeg failed (exit=" + exit + ") at timestamp " + timestamp);
            }
        } catch (IOException e) {
            throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                    "ffmpeg execution failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoExtractionException(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                    "ffmpeg interrupted", e);
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) return "";
        return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String truncateForLog(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete temporary frame {}: {}", path, e.getMessage());
        }
    }
}
