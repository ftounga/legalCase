package fr.ailegalcase.document;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * SF-144-01 : commit le flag {@code ocr_running} d'une extraction dans une
 * transaction séparée ({@link Propagation#REQUIRES_NEW}) pour que le polling
 * frontend puisse voir l'état intermédiaire pendant que l'appel synchrone
 * Textract bloque la transaction principale de {@code ExtractionService}.
 */
@Service
public class OcrRunningFlagService {

    private final DocumentExtractionRepository extractionRepository;

    public OcrRunningFlagService(DocumentExtractionRepository extractionRepository) {
        this.extractionRepository = extractionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markOcrRunning(UUID extractionId, boolean running) {
        extractionRepository.findById(extractionId).ifPresent(e -> {
            e.setOcrRunning(running);
            extractionRepository.save(e);
        });
    }
}
