package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SF-212-19 : réponse de l'endpoint d'analyse de la régularité d'une mise à
 * pied disciplinaire (F-DT-48-mise-a-pied-disciplinaire, FRANCE — L. 1331-1
 * CT ; L. 1332-1 à L. 1332-4 CT ; Cass. soc. constante).
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI lors du
 * GET) ET les sorties calculées (verdict, score, points de régularité,
 * salaire dû pendant la période, bases juridiques).</p>
 */
public record MiseAPiedDisciplinaireResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        MiseAPiedDisciplinaireInput.NatureMiseAPied natureMiseAPied,
        boolean procedureEntretienSuivie,
        boolean prescriptionFauteVerifiee,
        boolean dureeDefiniedansRI,
        Integer dureeJours,
        boolean salaireSuspendu,
        boolean sancionsAnterieuresMemesFaits,
        double salaireMensuelBrutEuros,
        // --- Outputs calculés ---
        MiseAPiedDisciplinaireCalculator.AnalyseMiseAPied analyseMiseAPied,
        int scoreMiseAPied,
        List<MiseAPiedDisciplinaireCalculator.PointRegularite> pointsRegularite,
        double salaireDuPeriodeMiseAPiedEuros,
        List<String> basesJuridiques,
        List<String> messages,
        String country,
        Instant calculatedAt
) {}
