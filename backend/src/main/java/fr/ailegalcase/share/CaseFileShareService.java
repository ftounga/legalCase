package fr.ailegalcase.share;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.CaseAnalysisResponse;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CaseFileShareService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CaseFileShareRepository shareRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final String frontendUrl;

    public CaseFileShareService(CaseFileShareRepository shareRepository,
                                CaseFileRepository caseFileRepository,
                                CaseAnalysisRepository caseAnalysisRepository,
                                CurrentUserResolver currentUserResolver,
                                WorkspaceMemberRepository workspaceMemberRepository,
                                @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.shareRepository = shareRepository;
        this.caseFileRepository = caseFileRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public ShareResponse createShare(UUID caseFileId, int expiresInDays,
                                     OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Workspace non trouvé"));

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .filter(cf -> cf.getWorkspace().getId().equals(member.getWorkspace().getId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dossier introuvable"));

        CaseFileShare share = new CaseFileShare();
        share.setCaseFile(caseFile);
        share.setToken(generateToken());
        share.setExpiresAt(Instant.now().plus(expiresInDays, ChronoUnit.DAYS));
        share.setCreatedBy(user);
        shareRepository.save(share);

        return new ShareResponse(
                share.getId(),
                frontendUrl + "/share/" + share.getToken(),
                share.getExpiresAt(),
                share.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ShareResponse> listActiveShares(UUID caseFileId,
                                                OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Workspace non trouvé"));

        caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .filter(cf -> cf.getWorkspace().getId().equals(member.getWorkspace().getId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dossier introuvable"));

        return shareRepository.findActiveByCaseFileId(caseFileId, Instant.now()).stream()
                .map(s -> new ShareResponse(s.getId(),
                        frontendUrl + "/share/" + s.getToken(),
                        s.getExpiresAt(),
                        s.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void revokeShare(UUID caseFileId, UUID shareId,
                            OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Workspace non trouvé"));

        caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .filter(cf -> cf.getWorkspace().getId().equals(member.getWorkspace().getId()))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dossier introuvable"));

        CaseFileShare share = shareRepository.findByIdAndCaseFileId(shareId, caseFileId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lien de partage introuvable"));

        share.setRevokedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public PublicShareResponse getPublicShare(String token) {
        CaseFileShare share = shareRepository.findByToken(token)
                .filter(CaseFileShare::isActive)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Lien introuvable ou expiré"));

        CaseFile caseFile = share.getCaseFile();

        CaseAnalysis synthesis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                        caseFile.getId(), AnalysisStatus.DONE)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Aucune synthèse disponible"));

        return new PublicShareResponse(
                caseFile.getId(),
                caseFile.getTitle(),
                caseFile.getLegalDomain(),
                share.getExpiresAt(),
                CaseAnalysisResponse.from(synthesis)
        );
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
