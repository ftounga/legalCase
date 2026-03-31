package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CaseFileExportService {

    private final CaseFileRepository caseFileRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final DocumentRepository documentRepository;
    private final CaseNoteRepository caseNoteRepository;
    private final CaseDeadlineRepository caseDeadlineRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final ObjectMapper objectMapper;

    public CaseFileExportService(CaseFileRepository caseFileRepository,
                                 CurrentUserResolver currentUserResolver,
                                 WorkspaceMemberRepository workspaceMemberRepository,
                                 DocumentRepository documentRepository,
                                 CaseNoteRepository caseNoteRepository,
                                 CaseDeadlineRepository caseDeadlineRepository,
                                 CaseAnalysisRepository caseAnalysisRepository,
                                 ObjectMapper objectMapper) {
        this.caseFileRepository = caseFileRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.documentRepository = documentRepository;
        this.caseNoteRepository = caseNoteRepository;
        this.caseDeadlineRepository = caseDeadlineRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public CaseFileExportResult export(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal)
            throws IOException {

        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        byte[] zipBytes = buildZip(caseFile);
        String filename = sanitize(caseFile.getTitle());
        return new CaseFileExportResult(zipBytes, filename);
    }

    private byte[] buildZip(CaseFile caseFile) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            // dossier.json
            addEntry(zos, "dossier.json", buildDossierJson(caseFile));

            // documents.csv
            List<Document> documents = documentRepository.findByCaseFileOrderByCreatedAtDesc(caseFile);
            addEntry(zos, "documents.csv", buildDocumentsCsv(documents));

            // notes.txt
            List<CaseNote> notes = caseNoteRepository.findByCaseFileIdOrderByCreatedAtDesc(caseFile.getId());
            addEntry(zos, "notes.txt", buildNotesTxt(notes));

            // delais.txt
            List<CaseDeadline> deadlines = caseDeadlineRepository.findByCaseFileIdOrderByDueDateAsc(caseFile.getId());
            addEntry(zos, "delais.txt", buildDelaisTxt(deadlines));

            // synthese.json (optional)
            Optional<CaseAnalysis> latestAnalysis = caseAnalysisRepository
                    .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFile.getId(), AnalysisStatus.DONE);
            if (latestAnalysis.isPresent()) {
                addEntry(zos, "synthese.json", buildSyntheseJson(latestAnalysis.get()));
            }
        }
        return baos.toByteArray();
    }

    private void addEntry(ZipOutputStream zos, String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }

    private byte[] buildDossierJson(CaseFile cf) throws IOException {
        Map<String, Object> data = Map.of(
                "id", cf.getId().toString(),
                "title", cf.getTitle(),
                "legalDomain", cf.getLegalDomain(),
                "status", cf.getStatus(),
                "createdAt", cf.getCreatedAt().toString()
        );
        return objectMapper.writeValueAsBytes(data);
    }

    private byte[] buildDocumentsCsv(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        sb.append("name,sizeBytes,uploadedAt,processingStatus\n");
        for (Document doc : documents) {
            sb.append(escapeCsv(doc.getOriginalFilename()))
                    .append(",")
                    .append(doc.getFileSize())
                    .append(",")
                    .append(doc.getCreatedAt())
                    .append(",")
                    .append(doc.getContentType())
                    .append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] buildNotesTxt(List<CaseNote> notes) {
        StringBuilder sb = new StringBuilder();
        for (CaseNote note : notes) {
            sb.append("[").append(note.getCreatedAt()).append("] ")
                    .append(note.getContent())
                    .append("\n---\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] buildDelaisTxt(List<CaseDeadline> deadlines) {
        StringBuilder sb = new StringBuilder();
        for (CaseDeadline d : deadlines) {
            sb.append(d.getLabel())
                    .append(" | ")
                    .append(d.getDueDate())
                    .append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] buildSyntheseJson(CaseAnalysis analysis) throws IOException {
        Map<String, Object> data = Map.of(
                "id", analysis.getId().toString(),
                "version", analysis.getVersion(),
                "analysisType", analysis.getAnalysisType().toString(),
                "analysisStatus", analysis.getAnalysisStatus().toString(),
                "analysisResult", analysis.getAnalysisResult() != null ? analysis.getAnalysisResult() : "",
                "modelUsed", analysis.getModelUsed() != null ? analysis.getModelUsed() : "",
                "createdAt", analysis.getCreatedAt().toString(),
                "updatedAt", analysis.getUpdatedAt().toString()
        );
        return objectMapper.writeValueAsBytes(data);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    static String sanitize(String title) {
        if (title == null) return "dossier";
        return title.replaceAll("[^a-zA-Z0-9\\-_]", "-").toLowerCase();
    }
}
