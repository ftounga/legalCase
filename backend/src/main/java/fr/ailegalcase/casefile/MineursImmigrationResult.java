package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-IM-19-01 : résultat de l'analyse d'éligibilité mineur étranger.
 *
 * @param dispositifVise        dispositif demandé (entrée)
 * @param dispositifRecommande  dispositif recommandé (peut différer si visé peu pertinent)
 * @param dateNaissance         date de naissance
 * @param dateEntreeFrance      date d'entrée en France (peut être null)
 * @param parentRegulier        au moins un parent en situation régulière
 * @param isolementAvere        isolement avéré (MNA)
 * @param motifOrdrePublic      motif d'ordre public présent
 * @param nationalite           nationalité (informationnelle)
 * @param ageAnnees             âge calculé en années à la date d'analyse
 * @param verdictEligibilite    ELEVEE / MOYENNE / FAIBLE
 * @param criteresNonRemplis    liste des critères absents bloquants
 * @param documentsRequis       liste des pièces à fournir
 * @param delaiInstructionMois  délai estimatif (mois)
 * @param baseJuridique         articles applicables
 * @param formule               résumé en une ligne
 * @param messages              conseils pratiques
 */
public record MineursImmigrationResult(
        String dispositifVise,
        String dispositifRecommande,
        LocalDate dateNaissance,
        LocalDate dateEntreeFrance,
        boolean parentRegulier,
        boolean isolementAvere,
        boolean motifOrdrePublic,
        String nationalite,
        int ageAnnees,
        String verdictEligibilite,
        List<String> criteresNonRemplis,
        List<String> documentsRequis,
        int delaiInstructionMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
