package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.referential.LegalReferentialService;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

@Service
public class ImmigrationRecoursService {

    private final ImmigrationRecoursRepository recoursRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;
    private final LegalReferentialService referentialService;

    public ImmigrationRecoursService(ImmigrationRecoursRepository recoursRepository,
                                      CaseFileRepository caseFileRepository,
                                      WorkspaceMemberRepository workspaceMemberRepository,
                                      CurrentUserResolver currentUserResolver,
                                      ObjectMapper objectMapper,
                                      LegalReferentialService referentialService) {
        this.recoursRepository = recoursRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
        this.referentialService = referentialService;
    }

    @Transactional
    public ImmigrationRecoursResponse generate(UUID caseFileId,
                                                ImmigrationRecoursRequest request,
                                                OidcUser oidcUser,
                                                Principal principal) {
        validateRequest(request);
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        GeneratedRecours generated = RecoursGenerator.generate(
                request.recoursType(),
                request.dateNotification(),
                request.requerant().nom(),
                request.requerant().prenom(),
                request.requerant().nationalite(),
                request.requerant().adresse(),
                request.decisionContestee().autorite(),
                request.decisionContestee().date(),
                request.decisionContestee().reference(),
                request.exposeFaits()
        );

        ImmigrationRecours recours = recoursRepository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ImmigrationRecours r = new ImmigrationRecours();
                    r.setCaseFile(caseFile);
                    return r;
                });

        recours.setRecoursType(request.recoursType());
        recours.setDateNotification(request.dateNotification());
        recours.setDateLimite(generated.dateLimite());
        recours.setRequerantData(serialize(request.requerant()));
        recours.setDecisionContesteeData(serialize(request.decisionContestee()));
        recours.setExposeFaits(request.exposeFaits());
        recours.setGeneratedDocument(serialize(generated));

        recoursRepository.save(recours);

        return toResponse(caseFileId, recours, generated);
    }

    @Transactional(readOnly = true)
    public ImmigrationRecoursResponse get(UUID caseFileId,
                                           OidcUser oidcUser,
                                           Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);

        ImmigrationRecours recours = recoursRepository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun recours trouvé pour ce dossier"));

        GeneratedRecours generated = deserialize(recours.getGeneratedDocument(), GeneratedRecours.class);
        return toResponse(caseFileId, recours, generated);
    }

    private void validateRequest(ImmigrationRecoursRequest request) {
        if (!ImmigrationRecoursReferentiel.isTypeValid(request.recoursType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type de recours inconnu : " + request.recoursType());
        }
        if (request.dateNotification() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Date de notification requise");
        }
        if (request.requerant() == null || isBlank(request.requerant().nom()) || isBlank(request.requerant().prenom())
                || isBlank(request.requerant().nationalite()) || isBlank(request.requerant().adresse())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Champs requérant obligatoires : nom, prenom, nationalite, adresse");
        }
        if (request.decisionContestee() == null || isBlank(request.decisionContestee().autorite())
                || request.decisionContestee().date() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Champs décision contestée obligatoires : autorite, date");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_IMMIGRATION".equals(caseFile.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return caseFile;
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ImmigrationRecoursResponse toResponse(UUID caseFileId,
                                                    ImmigrationRecours recours,
                                                    GeneratedRecours generated) {
        RecoursType type = referentialService.getRecoursType(recours.getRecoursType());
        ImmigrationRecoursRequest.RequerantData req = deserialize(
                recours.getRequerantData(), ImmigrationRecoursRequest.RequerantData.class);
        ImmigrationRecoursRequest.DecisionContesteeData dec = deserialize(
                recours.getDecisionContesteeData(), ImmigrationRecoursRequest.DecisionContesteeData.class);

        return new ImmigrationRecoursResponse(
                caseFileId,
                recours.getRecoursType(),
                type != null ? type.label() : recours.getRecoursType(),
                recours.getDateNotification(),
                recours.getDateLimite(),
                generated.dateLimiteDepassee(),
                generated.avertissement(),
                new ImmigrationRecoursResponse.RequerantData(req.nom(), req.prenom(), req.nationalite(), req.adresse()),
                new ImmigrationRecoursResponse.DecisionContesteeData(dec.autorite(), dec.date(), dec.reference()),
                recours.getExposeFaits(),
                new ImmigrationRecoursResponse.DocumentData(
                        generated.enTete(),
                        generated.objetDemande(),
                        generated.visaTextes(),
                        generated.exposeFaits(),
                        generated.moyensDroit(),
                        generated.conclusions(),
                        generated.piecesJointes()
                )
        );
    }
}
