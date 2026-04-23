package fr.ailegalcase.casefile;

import fr.ailegalcase.document.DocumentPiece;
import fr.ailegalcase.document.DocumentPieceRepository;
import fr.ailegalcase.document.DocumentPieceType;
import fr.ailegalcase.document.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * SF-IM-01-05 : pré-remplit automatiquement les statuts de la checklist F-IM-01
 * à partir des pièces détectées par F-145 dans les documents du dossier.
 *
 * <p>Au premier chargement d'une checklist, si aucun {@link ImmigrationPieceCheck}
 * n'existe, parcourt les libellés du référentiel et les matche contre les
 * {@link DocumentPieceType} des pièces identifiées. Les items matchés passent
 * à {@code PRESENT}, les autres restent {@code INCONNU}.
 *
 * <p>Idempotent : ne s'exécute jamais si des checks existent déjà (préserve
 * les modifications manuelles de l'avocat).
 */
@Service
public class ImmigrationPieceAutoFillService {

    private static final Logger log = LoggerFactory.getLogger(ImmigrationPieceAutoFillService.class);

    private final DocumentRepository documentRepository;
    private final DocumentPieceRepository pieceRepository;
    private final ImmigrationPieceCheckRepository pieceCheckRepository;

    public ImmigrationPieceAutoFillService(DocumentRepository documentRepository,
                                           DocumentPieceRepository pieceRepository,
                                           ImmigrationPieceCheckRepository pieceCheckRepository) {
        this.documentRepository = documentRepository;
        this.pieceRepository = pieceRepository;
        this.pieceCheckRepository = pieceCheckRepository;
    }

    /**
     * Pré-remplit les statuts pour un dossier, titre, pays donnés. N'écrase
     * jamais les checks existants.
     *
     * @return nombre d'items marqués PRESENT
     */
    @Transactional
    public int autoFillIfEmpty(CaseFile caseFile, String titreType, String country) {
        // Idempotence : si au moins un check existe déjà, ne rien faire
        List<ImmigrationPieceCheck> existing = pieceCheckRepository
                .findByCaseFileIdAndTitreTypeAndCountry(caseFile.getId(), titreType, country);
        if (!existing.isEmpty()) return 0;

        List<String> labels = ImmigrationPieceReferentiel.getPieces(titreType, country);
        if (labels.isEmpty()) return 0;

        // Récupère les types de pièces détectés dans tous les documents du dossier
        Set<DocumentPieceType> detectedTypes = detectedPieceTypes(caseFile.getId());
        if (detectedTypes.isEmpty()) {
            log.debug("No DocumentPiece detected for case {} — skipping autofill", caseFile.getId());
            return 0;
        }

        int marked = 0;
        for (String label : labels) {
            Set<DocumentPieceType> candidates = matchPieceTypes(label);
            if (candidates.stream().anyMatch(detectedTypes::contains)) {
                ImmigrationPieceCheck check = new ImmigrationPieceCheck();
                check.setCaseFile(caseFile);
                check.setTitreType(titreType);
                check.setCountry(country);
                check.setLabel(label);
                check.setStatut("PRESENT");
                pieceCheckRepository.save(check);
                marked++;
            }
        }

        log.info("Autofill checklist F-IM-01 for case {} / {} / {} — {} item(s) PRESENT sur {}",
                caseFile.getId(), titreType, country, marked, labels.size());
        return marked;
    }

    private Set<DocumentPieceType> detectedPieceTypes(UUID caseFileId) {
        EnumSet<DocumentPieceType> out = EnumSet.noneOf(DocumentPieceType.class);
        documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId).forEach(doc ->
                pieceRepository.findByDocument_IdOrderByOrderIndexAsc(doc.getId())
                        .stream()
                        .map(DocumentPiece::getType)
                        .filter(t -> t != DocumentPieceType.AUTRE)
                        .forEach(out::add)
        );
        return out;
    }

    /**
     * Mapping libellé → types de pièces candidats. Heuristique sur mots-clés en français.
     * Exposé package-private pour tests.
     */
    static Set<DocumentPieceType> matchPieceTypes(String label) {
        if (label == null) return Set.of();
        String l = label.toLowerCase();

        if (l.contains("passeport")) return Set.of(DocumentPieceType.PASSEPORT);
        if (l.contains("acte de mariage")) return Set.of(DocumentPieceType.ACTE_MARIAGE);
        if (l.contains("acte de naissance") && l.contains("enfant")) return Set.of(DocumentPieceType.ACTE_NAISSANCE_ENFANT);
        if (l.contains("acte de naissance")) return Set.of(DocumentPieceType.ACTE_NAISSANCE);
        if (l.contains("livret de famille")) return Set.of(DocumentPieceType.LIVRET_FAMILLE);
        if (l.contains("jugement de divorce")) return Set.of(DocumentPieceType.JUGEMENT_DIVORCE);
        if (l.contains("contrat de travail") || l.contains("contrat cdi") || l.contains("contrat cdd")) return Set.of(DocumentPieceType.CONTRAT);
        if (l.contains("bulletin")) return Set.of(DocumentPieceType.BULLETIN_PAIE);
        if (l.contains("promesse d'embauche") || l.contains("promesse d embauche")) return Set.of(DocumentPieceType.PROMESSE_EMBAUCHE);
        if (l.contains("titre de séjour") || l.contains("carte de séjour")) return Set.of(DocumentPieceType.TITRE_DE_SEJOUR);
        if (l.contains("visa")) return Set.of(DocumentPieceType.VISA);
        if (l.contains("récépissé")) return Set.of(DocumentPieceType.RECEPISSE_PREFECTURE);
        if (l.contains("oqtf") || l.contains("refus") && l.contains("décision")) return Set.of(DocumentPieceType.DECISION_OQTF);
        if (l.contains("avis d'imposition") || l.contains("avis d imposition")) return Set.of(DocumentPieceType.AVIS_IMPOSITION);
        if (l.contains("quittance")) return Set.of(DocumentPieceType.QUITTANCE_LOYER);
        if (l.contains("bail")) return Set.of(DocumentPieceType.BAIL_LOCATION);
        if (l.contains("attestation d'hébergement") || l.contains("attestation d hébergement")) return Set.of(DocumentPieceType.ATTESTATION_HEBERGEMENT);
        if (l.contains("justificatif") && (l.contains("domicile") || l.contains("logement"))) {
            return Set.of(DocumentPieceType.BAIL_LOCATION, DocumentPieceType.QUITTANCE_LOYER,
                    DocumentPieceType.ATTESTATION_HEBERGEMENT, DocumentPieceType.AVIS_IMPOSITION);
        }
        if (l.contains("justificatif") && (l.contains("ressources") || l.contains("revenus"))) {
            return Set.of(DocumentPieceType.JUSTIFICATIF_REVENUS, DocumentPieceType.AVIS_IMPOSITION,
                    DocumentPieceType.BULLETIN_PAIE);
        }
        if (l.contains("communauté de vie")) {
            return Set.of(DocumentPieceType.BAIL_LOCATION, DocumentPieceType.ATTESTATION,
                    DocumentPieceType.QUITTANCE_LOYER);
        }
        if (l.contains("attestation")) return Set.of(DocumentPieceType.ATTESTATION);
        if (l.contains("photo")) return Set.of(DocumentPieceType.PHOTO, DocumentPieceType.PIECE_IDENTITE);
        if (l.contains("certificat médical")) return Set.of(DocumentPieceType.CERTIFICAT_MEDICAL);
        if (l.contains("email") || l.contains("courriel")) return Set.of(DocumentPieceType.EMAIL);
        if (l.contains("sms") || l.contains("message")) return Set.of(DocumentPieceType.SMS);
        if (l.contains("lettre")) return Set.of(DocumentPieceType.LETTRE);

        // Aucun match : laisse INCONNU
        return Set.of();
    }
}
