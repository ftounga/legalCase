package fr.ailegalcase.video;

/**
 * SF-231-01 : exception levée par {@link VideoFrameExtractor} quand l'extraction
 * de frames vidéo échoue (ffmpeg en erreur, timeout, vidéo corrompue, ...).
 *
 * <p>Le {@link Reason} permet à {@code ExtractionService} de mapper l'échec
 * vers un motif lisible côté frontend.
 */
public class VideoExtractionException extends RuntimeException {

    public enum Reason {
        /** ffmpeg n'a pas terminé dans le délai imparti (cf. {@code app.video.ffmpeg-timeout-seconds}). */
        VIDEO_EXTRACTION_TIMEOUT,
        /** ffmpeg a retourné un code de sortie non nul (vidéo corrompue, format invalide, ...). */
        VIDEO_EXTRACTION_FAILED
    }

    private final Reason reason;

    public VideoExtractionException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public VideoExtractionException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
