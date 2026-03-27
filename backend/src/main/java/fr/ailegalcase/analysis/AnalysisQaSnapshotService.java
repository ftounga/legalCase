package fr.ailegalcase.analysis;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AnalysisQaSnapshotService {

    private final AiQuestionRepository aiQuestionRepository;
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private final AnalysisQaSnapshotRepository analysisQaSnapshotRepository;

    public AnalysisQaSnapshotService(AiQuestionRepository aiQuestionRepository,
                                     AiQuestionAnswerRepository aiQuestionAnswerRepository,
                                     AnalysisQaSnapshotRepository analysisQaSnapshotRepository) {
        this.aiQuestionRepository = aiQuestionRepository;
        this.aiQuestionAnswerRepository = aiQuestionAnswerRepository;
        this.analysisQaSnapshotRepository = analysisQaSnapshotRepository;
    }

    public void snapshot(UUID analysisId, UUID caseFileId) {
        List<AiQuestion> answered = aiQuestionRepository
                .findByCaseFileIdOrderByOrderIndex(caseFileId)
                .stream()
                .filter(q -> "ANSWERED".equals(q.getStatus()))
                .toList();

        List<AnalysisQaSnapshot> snapshots = answered.stream()
                .map(q -> {
                    String answerText = aiQuestionAnswerRepository
                            .findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId())
                            .map(AiQuestionAnswer::getAnswerText)
                            .orElse("");
                    AnalysisQaSnapshot snap = new AnalysisQaSnapshot();
                    snap.setAnalysisId(analysisId);
                    snap.setOrderIndex(q.getOrderIndex());
                    snap.setQuestionText(q.getQuestionText());
                    snap.setAnswerText(answerText);
                    return snap;
                })
                .toList();

        analysisQaSnapshotRepository.saveAll(snapshots);
    }

    public List<AnalysisQaSnapshot> findByAnalysisId(UUID analysisId) {
        return analysisQaSnapshotRepository.findByAnalysisIdOrderByOrderIndex(analysisId);
    }

    public Optional<String> buildQaContext(UUID analysisId) {
        List<AnalysisQaSnapshot> snapshots = findByAnalysisId(analysisId);
        if (snapshots.isEmpty()) return Optional.empty();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < snapshots.size(); i++) {
            AnalysisQaSnapshot s = snapshots.get(i);
            sb.append("Q").append(i + 1).append(" : ").append(s.getQuestionText()).append("\n");
            sb.append("R").append(i + 1).append(" : ").append(s.getAnswerText()).append("\n");
        }
        return Optional.of(sb.toString());
    }
}
