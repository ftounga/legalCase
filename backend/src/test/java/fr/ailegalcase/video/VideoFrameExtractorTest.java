package fr.ailegalcase.video;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-231-01 — tests d'extraction de frames vidéo via ffmpeg.
 *
 * <p>Les tests qui invoquent réellement ffmpeg sont gardés par
 * {@link #ffmpegAvailable()} pour ne pas casser la CI dans un environnement
 * sans ffmpeg. Localement (et sur l'image Docker backend), ffmpeg est présent
 * sur {@code /usr/bin/ffmpeg}.
 */
class VideoFrameExtractorTest {

    private VideoFrameExtractor extractor;
    private VideoFrameExtractorProperties props;

    @BeforeAll
    static void announce() {
        System.out.println("VideoFrameExtractorTest — ffmpegAvailable=" + ffmpegAvailable());
    }

    @BeforeEach
    void setUp() {
        props = new VideoFrameExtractorProperties();
        // Sur certains systèmes ffmpeg peut être ailleurs (Mac/brew). Auto-détection.
        String detected = detectFfmpegPath();
        if (detected != null) props.setFfmpegPath(detected);
        extractor = new VideoFrameExtractor(props);
    }

    static boolean ffmpegAvailable() {
        return detectFfmpegPath() != null;
    }

    private static String detectFfmpegPath() {
        for (String candidate : new String[]{"/usr/bin/ffmpeg", "/usr/local/bin/ffmpeg",
                "/opt/homebrew/bin/ffmpeg"}) {
            if (new File(candidate).canExecute()) return candidate;
        }
        return null;
    }

    /** Récupère le fichier vidéo de test (12 s, 320x240, ~43 ko). */
    private File sampleVideo() {
        return new File("src/test/resources/videos/sample-12s.mp4");
    }

    // U-VFE-01 : extraction nominale → 5 PNG bytes retournés, durée 12s
    @Test
    @EnabledIf("ffmpegAvailable")
    void extract5Frames_validVideo_returnsFivePngs() {
        File video = sampleVideo();
        assertThat(video).exists();

        List<byte[]> frames = extractor.extract5Frames(video);

        assertThat(frames).hasSize(5);
        for (byte[] png : frames) {
            assertThat(png).isNotEmpty();
            // PNG magic number : 89 50 4E 47 0D 0A 1A 0A
            assertThat(png.length).isGreaterThan(8);
            assertThat(png[0] & 0xFF).isEqualTo(0x89);
            assertThat(png[1]).isEqualTo((byte) 'P');
            assertThat(png[2]).isEqualTo((byte) 'N');
            assertThat(png[3]).isEqualTo((byte) 'G');
        }
    }

    // U-VFE-02 : extraction avec durée fournie explicitement
    @Test
    @EnabledIf("ffmpegAvailable")
    void extractFrames_withExplicitDuration_returnsFivePngs() {
        File video = sampleVideo();

        List<byte[]> frames = extractor.extractFrames(video, 12.0);

        assertThat(frames).hasSize(5);
        for (byte[] png : frames) {
            assertThat(png).isNotEmpty();
        }
    }

    // U-VFE-03 : vidéo corrompue (bytes invalides) → VideoExtractionException
    @Test
    @EnabledIf("ffmpegAvailable")
    void extract5Frames_corruptedFile_throwsExtractionException() throws Exception {
        Path corrupt = Files.createTempFile("legalcase-corrupt-", ".mp4");
        try {
            Files.writeString(corrupt, "this is not a valid mp4 file at all");

            assertThatThrownBy(() -> extractor.extract5Frames(corrupt.toFile()))
                    .isInstanceOf(VideoExtractionException.class)
                    .satisfies(e -> {
                        VideoExtractionException ve = (VideoExtractionException) e;
                        assertThat(ve.getReason())
                                .isEqualTo(VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED);
                    });
        } finally {
            Files.deleteIfExists(corrupt);
        }
    }

    // U-VFE-04 : fichier inexistant → VideoExtractionException
    @Test
    void extract5Frames_missingFile_throws() {
        File missing = new File("/tmp/legalcase-does-not-exist-" + System.nanoTime() + ".mp4");

        assertThatThrownBy(() -> extractor.extractFrames(missing, 10.0))
                .isInstanceOf(VideoExtractionException.class);
    }

    // U-VFE-05 : durée invalide (zéro ou négative) → VideoExtractionException
    @Test
    void extractFrames_invalidDuration_throws() throws Exception {
        Path tmp = Files.createTempFile("legalcase-empty-", ".mp4");
        try {
            assertThatThrownBy(() -> extractor.extractFrames(tmp.toFile(), 0))
                    .isInstanceOf(VideoExtractionException.class);
            assertThatThrownBy(() -> extractor.extractFrames(tmp.toFile(), -5.0))
                    .isInstanceOf(VideoExtractionException.class);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // U-VFE-06 : ffmpeg path invalide (binaire absent) → VideoExtractionException
    @Test
    void extract5Frames_invalidFfmpegPath_throws() {
        VideoFrameExtractorProperties bad = new VideoFrameExtractorProperties();
        bad.setFfmpegPath("/nonexistent/ffmpeg-binary-" + System.nanoTime());
        VideoFrameExtractor badExtractor = new VideoFrameExtractor(bad);

        assertThatThrownBy(() -> badExtractor.extractFrames(sampleVideo(), 12.0))
                .isInstanceOf(VideoExtractionException.class);
    }

    // U-VFE-07 : timeout très court → ffmpeg killed → VideoExtractionException TIMEOUT
    // On ne peut pas garantir un timeout sur une vidéo de 12s qui prend ~50ms à traiter.
    // À la place : vérifier que le timeout est bien propagé via probe sur fichier corrompu
    // qui hang (pas testable de manière fiable). On teste donc via parseDuration sur output vide.
    @Test
    void parseDuration_noMatch_throws() {
        assertThatThrownBy(() -> VideoFrameExtractor.parseDurationFromFfmpegOutput("no duration here"))
                .isInstanceOf(VideoExtractionException.class)
                .hasMessageContaining("Could not parse Duration");
    }

    // U-VFE-08 : parsing du format ffmpeg "Duration: 00:00:12.04"
    @Test
    void parseDuration_validFormat_returnsSeconds() {
        String output = "Stream #0:0: Video: h264, yuv420p, 320x240\n" +
                "  Duration: 00:00:12.04, start: 0.000000, bitrate: 64 kb/s\n";
        double duration = VideoFrameExtractor.parseDurationFromFfmpegOutput(output);
        assertThat(duration).isEqualTo(12.04);
    }

    // U-VFE-09 : parsing avec heures
    @Test
    void parseDuration_withHours_returnsSeconds() {
        String output = "Duration: 01:02:03.5, start";
        double duration = VideoFrameExtractor.parseDurationFromFfmpegOutput(output);
        assertThat(duration).isEqualTo(3723.5);
    }

    // U-VFE-10 : cleanup — après extraction, aucun fichier temporaire frame ne reste
    @Test
    @EnabledIf("ffmpegAvailable")
    void extract5Frames_cleansUpTempFrames() {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        long before = countLegalcaseTmpFrames(tmpDir);

        List<byte[]> frames = extractor.extract5Frames(sampleVideo());
        assertThat(frames).hasSize(5);

        long after = countLegalcaseTmpFrames(tmpDir);
        assertThat(after).isEqualTo(before);
    }

    private long countLegalcaseTmpFrames(File dir) {
        File[] files = dir.listFiles((d, name) ->
                name.startsWith("legalcase-video-") && name.endsWith(".png"));
        return files == null ? 0 : files.length;
    }
}
