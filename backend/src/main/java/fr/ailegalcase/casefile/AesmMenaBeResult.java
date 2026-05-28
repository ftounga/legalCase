package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-215-11 : résultat interne business du calcul de l'outil composite AESM + tutelle
 * MENA BE — F-IM-30-aesm-mena-be.
 *
 * <p>Outil composite 2 volets :
 * <ul>
 *   <li><b>Volet tutelle DGDE</b> — {@code etapeTutelle} (nullable si tuteur déjà
 *       désigné), {@code delaiDesignationTuteur} (date arrivée + 30 jours, art. 7
 *       loi 04/05/2007 — à vérifier).</li>
 *   <li><b>Volet AESM</b> — {@code scoreIntegration} (0-100, capé), {@code bonus}
 *       (0 ou 5 selon durée scolaire ≥ 24 mois), {@code verdictAESM}
 *       (FAVORABLE/SOUS_RESERVE/DEFAVORABLE), {@code prioriteUrgence}
 *       ({@code ageActuel ≥ 17}).</li>
 * </ul>
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — mineurs (gate {@code ageActuel < 18}).
 * Distinct de F-IM-19 mineurs FR (MNA — ordonnance JE + L.435-3 CESEDA).
 */
public record AesmMenaBeResult(
        int ageActuel,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateArriveeBelgique,
        boolean tuteurDesigne,
        boolean integrationScolaire,
        Integer dureeScolaire,
        boolean projetVieElabore,
        boolean perspectiveAutonomie,
        boolean menaceOrdrePublic,
        // Volet tutelle DGDE
        String etapeTutelle,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate delaiDesignationTuteur,
        // Volet AESM
        int scoreIntegration,
        int bonus,
        AesmMenaBeVerdictAesm verdictAESM,
        boolean prioriteUrgence,
        List<String> criteresNonRemplis,
        String prochainActe,
        String baseJuridique
) {}
