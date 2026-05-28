package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-219-10 : payload de création / mise à jour de l'analyse <i>statut
 * du délégué syndical</i> (CCT n° 5 du 24/05/1971 — statut des
 * délégations syndicales du personnel des entreprises).
 *
 * <h2>Logique métier (CCT n° 5)</h2>
 * <ol>
 *   <li><b>Champ d'application</b> — l'institution d'une délégation
 *       syndicale n'est pas automatique : elle relève des
 *       <b>conventions sectorielles</b> (CCT conclues en commission
 *       paritaire) qui fixent généralement un seuil minimum (le plus
 *       souvent 20 ou 50 travailleurs selon le secteur) + une demande
 *       d'au moins une OS représentative.</li>
 *   <li><b>Désignation</b> — par les organisations syndicales
 *       représentatives (CSC, FGTB, CGSLB), pas d'élection. Notification
 *       écrite à l'employeur obligatoire.</li>
 *   <li><b>Missions</b> (art. 2 et 3 CCT n° 5) — négociation des CCT
 *       d'entreprise ; traitement des réclamations individuelles et
 *       collectives ; information du personnel ; intervention en cas
 *       de conflit collectif. Quand il n'existe pas de CE ni de CPPT,
 *       la DS exerce leurs missions à titre supplétif (art. 24).</li>
 *   <li><b>Durée du mandat</b> — fixée par les CCT sectorielles
 *       (généralement 4 ans, renouvelable). La CCT n° 5 ne fixe pas
 *       de durée plafond, mais une révocation par l'OS reste possible
 *       à tout moment.</li>
 *   <li><b>Crédit d'heures / facilités</b> — fixés par les CCT
 *       sectorielles : temps nécessaire à l'exercice du mandat, accès
 *       aux locaux, communications.</li>
 * </ol>
 *
 * <h2>Champs obligatoires</h2>
 * <ul>
 *   <li>{@code dateDesignation} — date de désignation par l'OS.</li>
 *   <li>{@code effectifEntreprise} — nombre de travailleurs (référence
 *       sectorielle, généralement seuil 20 ou 50).</li>
 *   <li>{@code seuilSectorielRequis} — seuil minimum applicable selon
 *       la CCT sectorielle (par défaut 50, paramétrable).</li>
 *   <li>{@code presenceOsRepresentative} — au moins une OS représentative
 *       (CSC / FGTB / CGSLB) présente dans l'entreprise.</li>
 *   <li>{@code statutDesignation} — qualité de la désignation
 *       (régulière, notification manquante, pas par OS, contestée).</li>
 *   <li>{@code ceExistant} — Conseil d'entreprise existant dans
 *       l'entreprise.</li>
 *   <li>{@code cpptExistant} — Comité PPT existant dans l'entreprise.</li>
 * </ul>
 */
public record DelegueSyndicalCct5Request(

        @NotNull(message = "dateDesignation est requise")
        LocalDate dateDesignation,

        @NotNull(message = "effectifEntreprise est requis")
        @Min(value = 0, message = "effectifEntreprise doit être positif ou nul")
        Integer effectifEntreprise,

        @NotNull(message = "seuilSectorielRequis est requis")
        @Min(value = 1, message = "seuilSectorielRequis doit être au moins 1")
        Integer seuilSectorielRequis,

        @NotNull(message = "presenceOsRepresentative est requise")
        Boolean presenceOsRepresentative,

        @NotNull(message = "statutDesignation est requis")
        DelegueSyndicalCct5StatutDesignation statutDesignation,

        @NotNull(message = "ceExistant est requis")
        Boolean ceExistant,

        @NotNull(message = "cpptExistant est requis")
        Boolean cpptExistant
) {
}
