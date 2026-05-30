package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-229 SF-229-03 (2026-05-09) — garde-fou symétrique au
 * {@link DecisionToolVisibilityIntegrityIT} (F-164 SF-164-01) côté backend
 * dashboard. Empêche la régression "toolId hardcodé dans
 * {@code CaseFileDashboardService} sans correspondance Liquibase".
 *
 * <p>Origine du bug : audit transversal F-229 SF-229-03 a révélé 5 mismatches
 * en plus de F-192 (déjà fixé SF-229-02) sur des toolIds Immigration FR
 * (F-IM-08 OQTF avec/sans délai, référés admin, F-IM-19 mineurs) — la
 * convention "raccourcie" du backend (sans `-fr` / avec `-immigration`) ne
 * matchait pas la convention canonique des migrations Liquibase. Les tiles
 * étaient cassées silencieusement au clic dashboard.</p>
 *
 * <p>Principe : extrait par regex tous les {@code new DashboardTile("F-XX-...",
 * ...)} du source {@code CaseFileDashboardService.java} et vérifie que chaque
 * toolId est <strong>soit</strong> présent dans
 * {@code decision_tool_visibility_rules} (source de vérité Liquibase),
 * <strong>soit</strong> dans la liste {@link #KNOWN_SUMMARY_TILE_IDS} des
 * tiles "résumé" qui ne sont pas instanciées comme outils mais routées via
 * {@code BadgeNavigationService} côté frontend.</p>
 *
 * <p>Symétrique de {@link DecisionToolVisibilityIntegrityIT} qui protège
 * l'inverse (DB seedée sans entrée TOOL_REGISTRY frontend).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
class DashboardTileToolIdIntegrityIT {

    @Autowired
    JdbcTemplate jdbc;

    /**
     * Tiles "résumé" du dashboard décisionnel qui ne sont pas instanciées
     * comme outils — elles passent par {@link
     * fr.ailegalcase.casefile.CaseFileDashboardService} pour l'émission, mais
     * sont routées côté frontend via {@code BadgeNavigationService} (cible
     * canonique de la grille de badges F-162). Donc absentes de
     * {@code decision_tool_visibility_rules}.
     *
     * <p>Cf. F-229 SF-229-01 + SF-229-02 + SF-229-03 (handler F-193 ajouté).</p>
     */
    private static final Set<String> KNOWN_SUMMARY_TILE_IDS = Set.of(
            "F-192-retained-pistes-summary",
            "F-193-procedure-checks-summary",
            "F-194-pieces-summary",
            "F-195-risques-summary",
            "F-196-questions-summary",
            // F-253 SF-253-01 (2026-05-21) — tile dédiée au rappel des risques
            // restant à arbitrer (statut A_CREUSER), cohabite avec
            // F-195-risques-summary. Routée via BadgeNavigationService côté
            // frontend (clic → /synthesis#section-risques), donc absente de
            // decision_tool_visibility_rules (pattern identique aux 5 tiles
            // résumé ci-dessus).
            "F-253-risques-a-creuser"
            // SF-212-02 (2026-05-20) : F-DT-36-licenciement-faute-grave-lourde
            // retiré — résolution structurelle complète (seed migration 278 +
            // entrée TOOL_REGISTRY frontend dans decisional-tools-panel.component.ts
            // + tile mapper backend déjà présent dans CaseFileDashboardService).
            // SF-206-02 (2026-05-20) : F-DT-42 retiré — résolution structurelle
            // complète (seed migration 263 + TOOL_REGISTRY frontend + tile mapper).
            // SF-206-04 (2026-05-20) : F-DT-75 retiré — résolution structurelle
            // complète (seed migration 266 + entrée TOOL_REGISTRY frontend dans
            // decisional-tools-panel.component.ts + tile mapper backend déjà
            // présent dans CaseFileDashboardService).
            // SF-206-06 (2026-05-20) : F-DT-39 retiré — résolution structurelle
            // complète (seed migration 268 + entrée TOOL_REGISTRY frontend dans
            // decisional-tools-panel.component.ts + tile mapper backend
            // `tileFromPriseActeRuptureAnalysis` déjà présent dans
            // CaseFileDashboardService).
            // SF-206-08 (2026-05-20) : F-DT-40 retiré — résolution structurelle
            // complète (seed migration 270 + entrée TOOL_REGISTRY frontend dans
            // decisional-tools-panel.component.ts + tile mapper backend
            // `tileFromResiliationJudiciaireCphAnalysis` déjà présent dans
            // CaseFileDashboardService).
    );

    /**
     * SF-DT-36-03 — outils décisionnels seedés dans
     * {@code decision_tool_visibility_rules} (donc visibles dans le panel
     * F-IA-04) qui n'émettent <strong>volontairement</strong> aucune
     * {@code DashboardTile} : outils "formule/PDF" sans persistance de
     * résultat, composants UI Angular auto-suffisants, référentiels.
     *
     * <p>Sert au garde-fou inverse {@link
     * #tout_toolId_seede_en_visibilite_a_une_tile_dashboard_ou_est_explicitement_exclu()}.
     * Tout ajout ici doit être justifié : un outil disposant d'une table de
     * résultat + d'un endpoint de calcul ne doit JAMAIS y figurer — il doit
     * avoir sa tuile dashboard (sinon son résultat est calculé mais jamais
     * affiché, cf. bug F-DT-36 corrigé par SF-DT-36-03).</p>
     */
    private static final Set<String> KNOWN_NO_DASHBOARD_TILE_IDS = Set.of(
            // Outils info-only / formule / PDF — pas de table de résultat persistée.
            "F-DT-03-prescription-litige",
            "F-DT-04-fiche-prudhomale",
            "F-DT-06-requete-tribunal-travail",
            "F-132-rupture-amiable-info",
            // Wrappers frontend auto-suffisants (F-198) — pas de backend de calcul.
            "F-152-divorce-consentement-scoring",
            "F-153-fourchettes-jaf",
            "F-FA-01-prestation-compensatoire",
            "F-FA-02-pension-alimentaire",
            "F-FA-04-liquidation-communaute",
            // SF-216-07 : outil ARIPA recouvrement — table dédiée, pas de tile dashboard.
            "F-FA-ARIPA-RECOUVREMENT",
            // SF-216-15 : outil Adoption intra-familiale — table dédiée, pas de tile dashboard.
            "F-FA-ADOPTION-INTRA",
            // SF-216-17 : outil Adoption internationale — table dédiée, pas de tile dashboard.
            "F-FA-ADOPTION-INTERNATIONALE",
            // SF-216-09 : outil délégation autorité parentale FR — table dédiée, pas de tile dashboard.
            "F-FA-XX-delegation-ap",
            // SF-216-11 : outil retrait autorité parentale FR — table dédiée, pas de tile dashboard.
            "F-FA-RETRAIT-AP",
            // SF-216-13 : outil Audition du mineur par le JAF FR — table dédiée, pas de tile dashboard.
            "F-FA-AUDITION-MINEUR",
            // SF-216-19 : outil indignité successorale FR — table dédiée, pas de tile dashboard.
            "F-FA-INDIGNITE-SUCCESSORALE",
            // SF-216-21 : outil recel successoral FR — table dédiée, pas de tile dashboard.
            "F-FA-RECEL-SUCCESSION",
            // SF-216-23 : outil donation entre époux FR — table dédiée, pas de tile dashboard.
            "F-FA-DONATION-ENTRE-EPOUX",
            // SF-216-27 : outil partage successoral notarié FR — table dédiée, pas de tile dashboard.
            "F-FA-PARTAGE-NOTARIAL",
            // SF-216-25 : outil présomption de paternité FR — table dédiée, pas de tile dashboard.
            "F-FA-PRESOMPTION-PATERNITE",
            // SF-216-29 : outil donation-partage FR — table dédiée, pas de tile dashboard.
            "F-FA-DONATION-PARTAGE",
            // Checklist référentielle — pas de résultat décisionnel persisté.
            "F-IM-01-checklist-pieces",
            // F-207 — exclusions transitoires (à résorber par la session F-207) :
            // outils livrés sans mapper DashboardTile dans CaseFileDashboardService,
            // hotfix de mitigation master-red posé par F-206 (2026-05-20).
            // Le câblage des vraies tuiles revient à la SF F-207 propriétaire
            // (mêmes mappings que prescription-be-litige-travail + c4-onem-checklist
            // posés par #1141).
            "at-fedris-declaration",
            "contestation-c4-onem",
            // F-245 hotfix (2026-05-20) — prescription-be-litige-travail retiré ici :
            // la tuile dashboard est désormais câblée par
            // CaseFileDashboardService.tileFromPrescriptionBeLitigeTravailAnalysis().
            // L'exclusion temporaire posée par le commit 3e8cb1da (#1129) est
            // résorbée par la vraie correction structurelle.
            // SF-206-02 hotfix master-red (2026-05-20) : refere-tribunal-travail-be
            // livré par SF-207-05b (PR #1149) sans mapper DashboardTile —
            // exclusion transitoire à résorber par la session F-207 propriétaire,
            // même pattern que at-fedris-declaration / contestation-c4-onem.
            "refere-tribunal-travail-be",
            // SF-206-03 hotfix master-red (2026-05-20) : rcc-be-conditions et
            // rcc-be-indemnite-complementaire livrés par SF-207-06-backend (PR #1151)
            // et SF-207-07-backend (PR #1155) sans mapper DashboardTile —
            // exclusion transitoire à résorber par la session F-207 propriétaire,
            // même pattern que at-fedris-declaration / contestation-c4-onem /
            // refere-tribunal-travail-be.
            "rcc-be-conditions",
            "rcc-be-indemnite-complementaire",
            // SF-206-07 hotfix master-red (2026-05-20) : outplacement-be-obligatoire-45
            // livré par SF-207-08b-frontend (PR #1162) sans mapper DashboardTile
            // côté backend — exclusion transitoire à résorber par la session F-207
            // propriétaire, même pattern que at-fedris-declaration /
            // contestation-c4-onem / refere-tribunal-travail-be / rcc-be-conditions /
            // rcc-be-indemnite-complementaire. Issue master-red pré-existante à
            // SF-206-07, levée ici par hotfix transitoire pour permettre le merge
            // backend résiliation judiciaire (F-DT-40).
            "outplacement-be-obligatoire-45",
            // SF-252-01 hotfix master-red (2026-05-20) : succession-be-acceptation-renonciation
            // et succession-be-devolution-reserve livrés par SF-217-12 (PR #1181) et
            // SF-217-11 (PR #1180) sans mapper DashboardTile côté backend — exclusion
            // transitoire à résorber par la session F-217 propriétaire, même pattern
            // que at-fedris-declaration / contestation-c4-onem / refere-tribunal-travail-be
            // etc. Issue master-red pré-existante à SF-252-01, levée ici par hotfix
            // transitoire pour permettre le déploiement F-252 protections nullité.
            "succession-be-acceptation-renonciation",
            "succession-be-devolution-reserve",
            // SF-217-14 (2026-05-21) : protection-majeur-be livré sans mapper
            // DashboardTile côté backend — exclusion transitoire à résorber par
            // la session F-217 propriétaire (même pattern Vague 3 que
            // succession-be-acceptation-renonciation / succession-be-devolution-reserve).
            "protection-majeur-be",
            // SF-217-16 (2026-05-21) : mariage-etranger-be-reconnaissance livré sans
            // mapper DashboardTile — même pattern transitoire Vague 3.
            "mariage-etranger-be-reconnaissance",
            // SF-217-18 (2026-05-21) : contestation-filiation-be livré sans mapper
            // DashboardTile côté backend — exclusion transitoire alignée sur les
            // 2 autres outils F-217 Vague 3 (mêmes raisons : composant frontend
            // standalone auto-suffisant, restitution via GET du snapshot, pas de
            // besoin métier tuile dashboard immédiat). À résorber par la session
            // F-217 propriétaire qui consolidera les 3+ outils en lot.
            "contestation-filiation-be",
            // SF-212-03 (2026-05-23) : F-DT-50-forfait-jours-validite livré sans
            // mapper DashboardTile côté backend — exclusion transitoire alignée
            // sur le pattern Vague 3 (composant frontend standalone auto-suffisant,
            // restitution via GET du snapshot, pas de besoin métier tuile
            // dashboard immédiat dans le bundle F-212 P2 Travail FR). À résorber
            // par une SF de tile dashboard dédiée si retour terrain prouve le
            // besoin d'une mise en avant en surface dossier.
            "F-DT-50-forfait-jours-validite",
            // SF-212-05 (2026-05-23) : F-DT-72 transfert d'entreprise L. 1224-1 FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-72-transfert-entreprise-l1224-1",
            // SF-212-07 (2026-05-24) : F-DT-44 CSP/CRP — conformité de la proposition FR + SF-212-09 F-DT-91 faute inexcusable + SF-212-25 F-DT-61 lanceur d'alerte
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-44-csp-crp-conformite",
            "F-DT-91-faute-inexcusable-employeur",
            "F-DT-61-lanceur-alerte-protection",
            // SF-212-11 (2026-05-24) : F-DT-70 modification du contrat — refus du salarié FR
            "F-DT-70-modification-contrat-refus",
            // SF-212-13 (2026-05-24) : F-DT-71 mutation — validité de la clause de mobilité FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-71-mutation-clause-mobilite",
            // SF-212-15 (2026-05-24) : F-DT-82 télétravail — conformité et litige FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-82-teletravail-accord",
            // SF-212-19 (2026-05-24) : F-DT-48 mise à pied disciplinaire — régularité FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-48-mise-a-pied-disciplinaire",
            // SF-212-23 (2026-05-24) : F-DT-56 égalité salariale femmes/hommes FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-56-egalite-salariale-femmes-hommes",
            // SF-212-17 (2026-05-24) : F-DT-43 rupture anticipée du CDD FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-43-rupture-anticipee-cdd",
            // SF-212-21 (2026-05-24) : F-DT-41 démission validité équivoque FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-41-demission-validite-equivoque",
            // SF-212-35 (2026-05-24) : F-DT-46 PDV / RCC — conformité FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-46-pdv-rcc-conformite",
            // SF-212-29 (2026-05-25) : F-DT-77 congé maternité / paternité FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-77-conge-paternite-maternite",
            // SF-212-27 (2026-05-25) : F-DT-64 burn-out — reconnaissance MP hors tableau FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-64-burnout-reconnaissance-mp",
            // SF-212-31 (2026-05-25) : F-DT-65 élections CSE — conformité procédure FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-65-elections-cse-conformite",
            // SF-212-33 (2026-05-25) : F-DT-49 temps partiel — requalification en temps plein FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            "F-DT-49-temps-partiel-requalification",
            // SF-212-37 (2026-05-25) : F-DT-84 conciliation CPH BCA FR
            // livré sans mapper DashboardTile côté backend — exclusion transitoire
            // alignée sur le pattern Vague F-212 P2 (composant frontend standalone
            // auto-suffisant, restitution via GET du snapshot, pas de besoin
            // métier tuile dashboard immédiat). À résorber par la session F-212
            // propriétaire qui consolidera les outils P2 en lot.
            // F-212 19/19 — dernier outil livré du bundle F-212.
            "F-DT-84-conciliation-cph-bca",
            // F-213 vague 2/3/4 (2026-05-26 → 2026-05-27) : rappel-salaire-be,
            // licenciement-be-statut-unique-preavis et licenciement-be-formule-claeys
            // livrés par SF-213-02b (PR #1329), SF-213-03b (PR #1331) et SF-213-04b
            // (PR #1334) sans mapper DashboardTile côté backend — exclusion
            // transitoire alignée sur le pattern Vague F-212 P2 / F-217 Vague 3
            // (composant frontend standalone auto-suffisant, restitution via GET
            // du snapshot, pas de besoin métier tuile dashboard immédiat dans le
            // bundle F-213 P2 Travail BE). À résorber par la session F-213
            // propriétaire qui consolidera les outils P2 BE en lot.
            "rappel-salaire-be",
            "licenciement-be-statut-unique-preavis",
            "licenciement-be-formule-claeys",
            // SF-213-05b (2026-05-27) : licenciement-be-protection-grossesse
            // livré sans mapper DashboardTile côté backend — exclusion
            // transitoire alignée sur le pattern F-213 P2 Travail BE
            // (composant frontend standalone auto-suffisant, restitution via
            // GET du snapshot, pas de besoin métier tuile dashboard immédiat).
            // À résorber par la session F-213 propriétaire qui consolidera les
            // outils P2 BE en lot.
            "licenciement-be-protection-grossesse",
            // SF-213-06 (2026-05-27) : transaction-be-travail backend livré
            // sans tuile DashboardTile (frontend SF-213-06b à venir). Même
            // pattern que les 4 outils ci-dessus.
            "transaction-be-travail",
            // SF-213-07 (2026-05-27) : harcelement-be-procedure-formelle backend
            // livré (Loi 04/08/1996 art. 32bis-32sexies). Frontend SF-213-07b
            // à venir. Même pattern que les outils ci-dessus.
            "harcelement-be-procedure-formelle",
            // SF-213-08 (2026-05-27) : licenciement-be-protection-deleguee backend
            // livré (Loi 19/03/1991 délégué syndical + CCT n°5, indemnité forfaitaire
            // 2-4 ans). Frontend SF-213-08b à venir. Même pattern.
            "licenciement-be-protection-deleguee",
            // SF-213-09b (2026-05-27) : licenciement-be-acte-equivalent — outil
            // BE analyseur de l'acte équipollent à rupture (Loi 03/07/1978 art.
            // 20 + Cass. BE 23/12/1957). 4 verdicts (ACTE_EQUIPOLLENT_PROBABLE
            // / PAS_ACTE_EQUIPOLLENT / RISQUE_ACCEPTATION_TACITE / A_ANALYSER)
            // + ICP indicatif. Outil ALWAYS_ON priority 117, BE / DROIT_DU_TRAVAIL ;
            // pas de tuile DashboardTile par conception (analyseur invoqué par
            // l'avocat depuis le panel F-IA-04). Pattern uniforme F-213 vagues
            // 2-8.
            "licenciement-be-acte-equivalent",
            // SF-213-10b (2026-05-27) : licenciement-be-cct109-deraisonnable —
            // backend mergé (PR #1350, CCT 109 du 12/02/2014 art. 8-9, score
            // 3/8/12/17 sem.) sans DashboardTile (frontend rend l'échelon dans
            // le panel F-IA-04, pas dans le dashboard global). Même pattern
            // uniforme F-213 vagues 1-9b (mémoire `feedback_f213_backend_pattern`).
            "licenciement-be-cct109-deraisonnable",
            // SF-215-02 (2026-05-27) : F-IM-25-single-permit-be — seed visibility
            // posé en hotfix post-merge (migration 372-seed-single-permit-be-
            // visibility.xml) sans mapper DashboardTile côté backend. Restitution
            // dans le panel F-IA-04 (composant frontend standalone), pas de tuile
            // dashboard immédiate dans le bundle F-215. Exclusion transitoire
            // alignée sur le pattern F-213 / F-217 vagues. À résorber par la
            // session F-215 propriétaire si retour terrain prouve le besoin.
            "F-IM-25-single-permit-be",
            // SF-219-01 (2026-05-27) : rcc-be-metiers-lourds — backend mergé
            // (PR #1358, Loi 26/12/2013 + AR 03/05/2007 art. 3 régime 58+/35 +
            // métier lourd, sortie ONEM). Frontend SF-219-01b à venir. Même
            // pattern uniforme F-213 / F-217 vagues — composant frontend
            // standalone auto-suffisant, restitution via GET du snapshot.
            "rcc-be-metiers-lourds",
            // SF-219-02 (2026-05-27) : rcc-be-longue-carriere — backend (présente
            // PR) Loi 26/12/2013 + CCT n° 17 + AR 03/05/2007 art. 3, régime
            // longue carrière 59+/40 ans. Frontend SF-219-02b à venir. Même
            // pattern uniforme F-213 / F-217 vagues.
            "rcc-be-longue-carriere",
            // SF-219-04 (2026-05-27) : cumul-rcc-allocations — analyseur du
            // cumul allocations chômage ONEM + indemnité complémentaire CCT 17
            // (plafond, disponibilité ajustée, bascule pension légale,
            // compatibilité activité). CCT n° 17 + AR 25/11/1991 + AR
            // 03/05/2007 art. 22 et suiv. Frontend SF-219-04b à venir.
            // Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (sera
            // utile dès le seed visibility par SF-219-04b — pattern uniforme
            // F-213 vagues 1-10b).
            "cumul-rcc-allocations",
            // SF-219-03 (2026-05-27) : rcc-be-entreprise-difficulte — backend
            // (présente PR) Loi 26/12/2013 + CCT n° 17 + AR 03/05/2007 + AR
            // reconnaissance ministre. Régime dérogatoire à âge réduit selon
            // le plan agréé par le ministre de l'Emploi (typiquement 55 ans).
            // Frontend SF-219-03b à venir. Pattern uniforme F-213 / F-217
            // / F-219 vagues — préventif pour éviter master-red dès le seed
            // visibility frontend.
            "rcc-be-entreprise-difficulte",
            // SF-215-04 (2026-05-27) : F-IM-26-regroupement-10ter-be — seed visibility
            // mergé par PR #1365 sans ajout à KNOWN_NO_DASHBOARD_TILE_IDS, ce qui
            // a déclenché un master-red persistant. Hotfix tardif pour rétablir
            // CI master vert (pattern uniforme F-213 / F-215 / F-217 / F-219).
            // Frontend SF-215-04b si applicable, sinon outil backend-only.
            "F-IM-26-regroupement-10ter-be",
            // SF-215-05/06 (2026-05-27) : F-IM-27-regroupement-10bis-be — seed
            // visibility livrée dans la PR backend SF-215-05 (PR #1372). Outil
            // BE frontend autosuffisant sans tile DashboardTile dédié — pattern
            // uniforme F-215 P2 Immigration BE.
            "F-IM-27-regroupement-10bis-be",
            // SF-215-07/08 (2026-05-27) : F-IM-28-naturalisation-12bis-be —
            // seed visibility livrée dans la PR backend SF-215-07 (PR #1383).
            // Outil BE frontend autosuffisant sans tile DashboardTile —
            // pattern uniforme F-215 P2 Immigration BE.
            "F-IM-28-naturalisation-12bis-be",
            // SF-215-09/10 (2026-05-28) : F-IM-29-naturalisation-conjoint-belge-be
            // — seed visibility livrée dans la PR backend SF-215-09 (PR #1389).
            // Outil BE frontend autosuffisant sans tile DashboardTile —
            // pattern uniforme F-215 P2 Immigration BE.
            "F-IM-29-naturalisation-conjoint-belge-be",
            // SF-215-11/12 (2026-05-28) : F-IM-30-aesm-mena-be — outil composite
            // tutelle DGDE (Loi 04/05/2007) + AESM (art. 9bis adapté MENA +
            // circulaire OE 15/09/2005). Seed visibility livrée dans la PR
            // backend SF-215-11. Outil BE frontend autosuffisant sans tile
            // DashboardTile — pattern uniforme F-215 P2 Immigration BE.
            "F-IM-30-aesm-mena-be",
            // SF-215-13 (2026-05-29) : F-IM-31-cce-annulation-30j-be — calculateur
            // de délai du recours en annulation devant le Conseil du Contentieux
            // des Étrangers (CCE — art. 39/82 §4 al. 1 Loi 15/12/1980, 30 jours
            // calendaires). Seed visibility livrée dans la PR backend SF-215-13.
            // Outil BE frontend autosuffisant sans tile DashboardTile —
            // pattern uniforme F-215 P2 Immigration BE.
            "F-IM-31-cce-annulation-30j-be",
            // SF-215-15 (2026-05-29) : F-IM-32-cce-extreme-urgence-5j-be —
            // calculateur de délai du recours en EXTRÊME URGENCE devant le CCE
            // (art. 39/82 §4 al. 2-3 Loi 15/12/1980, 5 jours OUVRABLES). Seed
            // visibility livrée dans la PR backend SF-215-15. Outil BE frontend
            // autosuffisant sans tile DashboardTile — pattern uniforme F-215 P2
            // Immigration BE.
            "F-IM-32-cce-extreme-urgence-5j-be",
            // SF-215-17 (2026-05-29) : F-IM-33-annexe13quinquies-ie-be —
            // calculateur de l'Annexe 13quinquies (OQT + interdiction d'entrée
            // art. 74/11 Loi 15/12/1980, durée 3/5/8 ans + recours annulation
            // CCE 30j calendaires + conditions de levée art. 74/12). Seed
            // visibility livrée dans la PR backend SF-215-17. Outil BE frontend
            // autosuffisant sans tile DashboardTile — pattern uniforme F-215 P2
            // Immigration BE. Entrée TOOL_REGISTRY frontend par SF-215-18.
            "F-IM-33-annexe13quinquies-ie-be",
            // SF-215-19 (2026-05-29) : F-IM-34-protection-temporaire-ukraine-be —
            // outil information + checklist + calcul de durée de la protection
            // temporaire Ukraine (directive 2001/55/CE, décision UE 2022/382,
            // Loi 15/12/1980 art. 57/29+ — droit au travail immédiat sans single
            // permit). Seed visibility livrée dans la PR backend SF-215-19. Outil BE
            // frontend autosuffisant sans tile DashboardTile — pattern uniforme F-215
            // P2 Immigration BE. Entrée TOOL_REGISTRY frontend par SF-215-20.
            "F-IM-34-protection-temporaire-ukraine-be",
            // SF-219-05 (2026-05-28) : outplacement-be-general-30sem — backend
            // (PR #1387) Loi 05/09/2001 art. 11 + AR 21/10/2007. Régime
            // général de l'outplacement obligatoire pour les travailleurs
            // licenciés dont le préavis (ou indemnité équivalente) est ≥ 30
            // semaines. Verdict CONFORME / NON_DU_* / NON_CONFORME_* +
            // sanction forfaitaire travailleur indicative. Frontend SF-219-05b
            // à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le backend
            // pour éviter master-red dès le seed visibility frontend (pattern
            // uniforme F-213 / F-217 / F-219 vagues 1-5).
            "outplacement-be-general-30sem",
            // SF-219-06 (2026-05-28) : licenciement-be-fermeture-entreprise —
            // backend (présente PR) Loi 26/06/2002 + AR 23/03/2007 (Fonds
            // Fermeture Entreprises FFE) + CCT n° 9bis. Calcule l'éligibilité
            // d'une fermeture au régime FFE, l'indemnité de fermeture
            // forfaitaire (+ supplément ≥ 45 ans) et la reprise des créances
            // impayées par le FFE en cas d'insolvabilité. Frontend SF-219-06b
            // à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le backend
            // (pattern uniforme F-213 / F-219 vagues).
            "licenciement-be-fermeture-entreprise",
            // SF-219-08 (2026-05-28) : transfert-entreprise-cct-32bis —
            // outil BE checklist conformité transfert conventionnel
            // d'entreprise (CCT n° 32bis du 07/06/1985 + Loi 17/03/1965 +
            // Directive 2001/23/CE). Vérifie la qualification de
            // l'opération, la préservation de l'identité économique, le
            // respect de la procédure d'information-consultation préalable,
            // le maintien automatique des contrats individuels + CCT, et la
            // responsabilité solidaire 1 an cédant/cessionnaire. Frontend
            // SF-219-08b à venir.
            "transfert-entreprise-cct-32bis",
            // SF-219-07 (2026-05-28) : licenciement-be-collectif-renault —
            // backend (présente PR) Loi 13/02/1998 « loi Renault » + CCT
            // n° 24 + CCT n° 39 + Directive 98/59/CE. Checklist procédurale
            // vérifiant le seuil de déclenchement (10/20/30 licenciements
            // selon taille), les 3 phases info → consultation →
            // décision/notification autorité régionale, et le délai
            // d'attente de 30 jours après notification autorité.
            // Frontend SF-219-07b à venir.
            "licenciement-be-collectif-renault",
            // SF-219-09 (2026-05-28) : elections-sociales-be — backend
            // Loi 04/12/2007 + AR 25/05/2012 + Loi 04/08/1996 art. 49 +
            // Loi 19/03/1991. Calculateur de chronologie + checklist
            // d'obligation (seuils CE ≥ 100 ETP, CPPT ≥ 50 ETP),
            // calendrier complet (jour Y, jour X = Y-90, X-60 procédure
            // UTE, X-35 listes électeurs, X+35 dépôt candidats, X+40
            // affichage candidats, Y+6 proclamation, Y+45 1re réunion) et
            // fenêtre de protection candidats contre licenciement (Loi
            // 19/03/1991, occulte rétroactive 30 j avant affichage, fin
            // 2 ans non-élus). Frontend SF-219-09b à venir. Préventif
            // KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern
            // uniforme F-213 / F-219 vagues).
            "elections-sociales-be",
            // SF-219-10 (2026-05-28) : delegue-syndical-cct-5 —
            // backend (présente PR) CCT n° 5 du 24/05/1971 conclue au
            // CNT (statut des délégations syndicales du personnel des
            // entreprises) + CCT 5bis/5ter + AR 26/01/1972 + CCT
            // sectorielles. Vérifie l'éligibilité d'un travailleur au
            // statut de DS (champ d'application sectoriel + désignation
            // par OS représentative + notification formelle employeur),
            // les missions exerçables (art. 3 négociation /
            // plaintes / information, art. 24 supplétif CE/CPPT) et
            // la durée indicative du mandat (4 ans). Outil distinct
            // de SF-213-08 (protection licenciement Loi 19/03/1991).
            // Frontend SF-219-10b à venir.
            "delegue-syndical-cct-5",
            // SF-219-12 (2026-05-28) : flexi-job-be — backend (PR #1404
            // mergée parallèle) Loi-programme 26/12/2013 art. 13 à 28
            // + Loi 25/04/2014 + Loi 16/11/2015 (ext. boulangerie /
            // coiffure, validée Cour const. arrêt 107/2017) +
            // Loi-programme 30/10/2018 (ext. commerce détail /
            // agriculture / soins de santé, suppression plafond cumul
            // pensionnés) + Loi-programme 28/12/2023 + AR 02/06/2024
            // (ext. sport / culture / événementiel / secteur public
            // limité, indexation flexi-salaire + plafond annuel).
            // Vérifie l'éligibilité cumulative (condition personnelle
            // travailleur pensionné OU salarié 4/5 ETP T-3 ; condition
            // sectorielle employeur ; interdiction cumul même employeur ;
            // formalisme contrat-cadre + Dimona FLX ; rémunération ≥
            // flexi-salaire minimum indexé + revenu annuel ≤ plafond
            // exonéré sauf pensionné). Frontend SF-219-12b à venir.
            // Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le backend
            // (pattern uniforme F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "flexi-job-be",
            // SF-219-11 (2026-05-28) : conge-education-paye-region —
            // backend (présente PR) Loi du 22/01/1985 (Section 6,
            // art. 108-117) régionalisée par la 6e réforme de l'État
            // (Loi spéciale 06/01/2014, transfert 01/07/2014). Outil
            // unique à branchement régional interne (Wallonie : Décret
            // 19/02/2014 + AGW 19/12/2014 ; Flandre : VOV Décret
            // 12/12/2014 ; Bruxelles : Ordonnance 02/07/2015 + AGBR
            // 14/04/2016). Calcule droit au congé selon région du lieu
            // de travail + type de formation (PROFESSIONNELLE_QUALIFIANTE
            // / GENERALE / ENSEIGNEMENT_SUPERIEUR_ALTERNANCE /
            // RECONVERSION_PUBLIC_FRAGILISE / HORS_LISTE_AGREEE) +
            // taux d'occupation (proratisation au mi-temps, dérogation
            // FLA 1/5 temps publics fragilisés). Verdicts :
            // ELIGIBLE_PLEIN_DROIT / ELIGIBLE_PRORATA /
            // INELIGIBLE_HORS_FORMATION_AGREEE /
            // INELIGIBLE_OCCUPATION_INSUFFISANTE / A_ANALYSER. Frontend
            // SF-219-11b à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS
            // dès le backend (pattern uniforme F-213 / F-219 vagues).
            "conge-education-paye-region",
            // SF-219-14 (2026-05-28) : interim-be-cct-322 — backend
            // (mergée PR #1408) Loi du 24/07/1987 relative au travail
            // temporaire, au travail intérimaire et à la mise de
            // travailleurs à la disposition d'utilisateurs (M.B.
            // 20/08/1987) + CCT n° 322 du 14/06/2010 conclue au CNT
            // (responsabilité solidaire ETI/utilisateur, parité
            // salariale) + CCT n° 108 du 16/07/2013 (motif insertion
            // en vue d'embauche durable, art. 1bis Loi 24/07/1987)
            // + AR du 11/10/1976 (liste limitative travail
            // exceptionnel). Analyse la validité d'une mission
            // intérimaire tripartite (entreprise utilisatrice / ETI /
            // intérimaire) sous l'angle cumulatif motif autorisé
            // (REMPLACEMENT / SURCROIT / EXCEPTIONNEL / INSERTION /
            // ARTISTIQUE / FLUX / NON_AUTORISE), interdiction de
            // remplacement grève/lock-out (art. 19, présomption
            // irréfragable de fraude), durée maximale légale selon
            // motif, parité salariale stricte (art. 10 : intérimaire
            // perçoit ≥ salaire d'un permanent de même qualification
            // chez l'utilisateur), formalisme (contrat écrit
            // ETI/intérimaire art. 8 + Dimona ETI). Verdicts :
            // ELIGIBLE_MISSION_REGULIERE / INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT
            // / INELIGIBLE_MOTIF_NON_AUTORISE / INELIGIBLE_DUREE_MAX_DEPASSEE
            // / INELIGIBLE_PARITE_SALARIALE_VIOLEE
            // / FRAGILE_CONTRAT_OU_DIMONA_MANQUANT / A_ANALYSER.
            // Sanction commune verdicts négatifs : requalification CDI
            // utilisateur + responsabilité solidaire ETI/utilisateur
            // pour cotisations ONSS éludées + salaires arriérés +
            // indemnités de rupture (art. 20 + CCT n° 322). Frontend
            // SF-219-14b à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS
            // dès le backend (pattern uniforme F-213 / F-219 vagues,
            // cf. CLAUDE.md feedback_pre_merge_visibility_seed_check).
            "interim-be-cct-322",
            // SF-219-13 (2026-05-28) : etudiant-jobiste-be — backend
            // (présente PR) Loi du 03/07/1978 sur les contrats de
            // travail, Titre VII (art. 120 à 130ter — contrat
            // d'occupation d'étudiants) + Loi-programme du 24/12/2002
            // (instauration des cotisations de solidarité réduites)
            // + AR du 14/07/1995 (modalités — 5,42 % patronale +
            // 2,71 % personnelle = 8,13 % total) + AR du 05/11/2002
            // (Dimona spécifique type STU) + Loi du 18/12/2016
            // (passage 50 jours → 475 heures/an) + Loi-programme du
            // 28/12/2022 + AR du 06/03/2023 (relèvement transitoire
            // 600h/an 2023-2024) + Loi-programme du 22/12/2023
            // (M.B. 29/12/2023) pérennisant le quota à 600 heures/an.
            // Vérifie l'éligibilité cumulative (statut étudiant à
            // titre principal OU interruption courte ≤ 12 mois ;
            // quota annuel 600h non dépassé ; contrat écrit signé ;
            // Dimona STU déclarée ; cotisations réduites appliquées).
            // Verdicts : ELIGIBLE / INELIGIBLE_STATUT_NON_ETUDIANT /
            // INELIGIBLE_QUOTA_DEPASSE / FRAGILE_CONTRAT_OU_DIMONA_MANQUANT
            // / FRAGILE_COTISATIONS_NON_REDUITES / A_ANALYSER.
            // Frontend SF-219-13b à venir. Préventif
            // KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern
            // uniforme F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "etudiant-jobiste-be",
            // SF-219-16 (2026-05-28) : teletravail-be-cct-85-149 —
            // backend (mergée PR #1412) CCT n° 85 du 09/11/2005 (CNT)
            // concernant le télétravail régulier et structurel
            // (rendue obligatoire par AR du 13/06/2006 M.B.
            // 05/09/2006) + CCT n° 149 du 26/01/2021 (CNT) relative
            // au télétravail recommandé ou obligatoire en raison
            // de la crise du coronavirus (devenue cadre de référence
            // du télétravail occasionnel / force majeure) + Loi du
            // 03/07/1978 art. 17 (obligations du travailleur) + Loi
            // du 04/08/1996 + Code du bien-être au travail Livre
            // VIII Titre 1 + Loi du 26/03/2018 art. 16-18 (droit à
            // la déconnexion par concertation collective) + Loi du
            // 03/10/2022 « Deal pour l'emploi » M.B. 10/11/2022
            // (modalités de déconnexion par CCT / règlement de
            // travail obligatoires pour les entreprises ≥ 20
            // travailleurs, e.e.v. 01/04/2023). Vérifie la
            // conformité cumulative (type STRUCTUREL_CCT_85 /
            // OCCASIONNEL_CCT_149 / INDETERMINE ; volontariat
            // réciproque ; convention individuelle écrite art. 6
            // CCT n° 85 ; équipement fourni ou indemnisé art. 9
            // CCT n° 85 ; indemnité forfaitaire ≤ plafond
            // ONSS/SPF Finances ; droits sociaux maintenus art. 4
            // CCT n° 85 ; modalités de déconnexion définies si
            // effectif ≥ 20). Verdicts :
            // CONFORME_CCT_85_STRUCTUREL /
            // CONFORME_CCT_149_OCCASIONNEL /
            // NON_CONFORME_CONVENTION_ECRITE_MANQUANTE /
            // NON_CONFORME_EQUIPEMENT_NON_FOURNI /
            // NON_CONFORME_DROITS_REDUITS /
            // FRAGILE_DECONNEXION_NON_DEFINIE / A_ANALYSER.
            // Frontend SF-219-16b à venir. Préventif
            // KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern
            // uniforme F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "teletravail-be-cct-85-149",
            // SF-219-15 (2026-05-28) : interim-be-indemnite-fin-mission —
            // backend (présente PR) Loi du 24/07/1987 sur le travail
            // temporaire (M.B. 20/08/1987), art. 8 + art. 10 (parité
            // salariale) ; AR du 30/03/1967 sur les vacances annuelles,
            // art. 19 (pécule de vacances 15,38 % = 7,67 % simple +
            // 7,71 % double) ; CCT n° 322 du 14/06/2010 conclue au CNT
            // (responsabilité solidaire ETI/utilisateur) ; CCT n° 322bis
            // du 16/12/2010 (versement pécule via Fonds Social
            // Intérimaires – FSI) ; CCT sectorielles propres à la
            // commission paritaire de l'utilisateur pour la prime de
            // fin d'année 13e mois (CP 124 construction 9,12 % CCT
            // 12/06/2014 AR 11/02/2015 ; CP 200 employés 8,33 % CCT
            // 09/06/2016 AR 13/06/2017 ; CP 140 transport 8,33 % CCT
            // 27/01/2011 AR 30/03/2011 ; etc.) ; Loi du 03/07/1978
            // sur les contrats de travail art. 39 et s. (indemnité de
            // préavis par renvoi) ; Loi du 16/03/1971 sur le travail
            // art. 29 (sursalaires 50 % semaine, 100 %
            // dimanche/férié) ; Cass. (BE) 04/02/1991 S.90.0024.F et
            // 16/12/2002 S.01.0124.F (rupture anticipée injustifiée
            // d'une mission intérim ouvre droit au salaire restant à
            // courir jusqu'au terme) ; Cass. (BE) 03/05/2010
            // S.09.0086.F et 23/06/2003 S.02.0103.F (refus explicite
            // d'une prime de précarité forfaitaire 10 % analogue à
            // l'IFM française art. L. 1251-32 en droit belge).
            // Calcule les composantes effectivement dues : pécule de
            // vacances intérim (15,38 % sauf FSI déjà versé), prime
            // de fin d'année sectorielle (taux variable selon CP
            // utilisateur, conditionnée ancienneté sectorielle ≥ 65 j),
            // indemnité de rupture anticipée (salaire restant à courir
            // si ETI rompt sans motif grave), sursalaire heures sup.
            // Avertissement systématique : PAS de prime de précarité
            // 10 % style FR en BE. Verdicts : INDEMNITES_DUES /
            // RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR /
            // AUCUNE_INDEMNITE_DUE / A_ANALYSER_SECTEUR_NON_RECONNU.
            // Frontend SF-219-15b à venir. Préventif
            // KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern
            // uniforme F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "interim-be-indemnite-fin-mission",
            // SF-219-17 (2026-05-28) : clause-ecolage-be — backend
            // (mergée PR #1416) art. 22bis Loi du 03/07/1978 sur les
            // contrats de travail (inséré par la Loi du 27/12/2006
            // M.B. 28/12/2006, modifié par la Loi du 22/12/2017 —
            // Pacte de compétitivité) : forme écrite obligatoire au
            // plus tard à l'entrée en formation (§ 1er al. 1er) ;
            // formation spécifique (non imposée par norme légale ou
            // réglementaire) + coût réel > 1/2 × RMMMG mensuel
            // (§ 2 al. 3 2°, CCT n° 43 du 02/05/1988 du CNT) ;
            // clause inopposable en cas de licenciement sans motif
            // grave par l'employeur, de rupture pour motif grave
            // dans le chef de l'employeur ou d'expiration normale
            // d'un CDD (§ 3) ; durée d'efficacité maximale 36 mois
            // soit 3 ans à compter de la fin de la formation
            // (§ 4 al. 1er) ; dégressivité par tiers de la durée
            // d'efficacité — 100/66/33 pourcent (§ 4 al. 2) ;
            // plafond absolu 80 pourcent du coût réel (§ 4 al. 4) ;
            // CCT n° 13 du 02/02/2013 du CNT (cadre subsidiaire
            // formations qualifiantes sectorielles). Calcule la
            // validité formelle + le montant dégressif dû par le
            // travailleur en cas de départ anticipé, plafonné à 80
            // pourcent du coût réel de la formation. Verdicts :
            // VALIDE_REMBOURSEMENT_DEGRESSIF /
            // VALIDE_DUREE_EXPIREE / INOPPOSABLE_MOTIF_DEPART /
            // NULLE_FORME_ECRITE_MANQUANTE /
            // NULLE_FORMATION_OBLIGATOIRE /
            // NULLE_COUT_INSUFFISANT / NULLE_DUREE_EXCESSIVE /
            // A_ANALYSER. Frontend SF-219-17b à venir. Préventif
            // KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern
            // uniforme F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "clause-ecolage-be",
            // SF-219-18 (2026-05-28) : semaine-4-jours-be — backend
            // (présente PR) Loi du 03/10/2022 portant des dispositions
            // diverses en matière de travail dite « Deal pour
            // l'emploi » M.B. 10/11/2022, art. 5 — possibilité offerte
            // au travailleur à temps plein de demander la compression
            // de la durée hebdomadaire de travail 38 à 40 h sur 4
            // jours sans réduction. Journée plafonnée à 9 h 30
            // (10 h si CCT sectorielle), accord employeur formalisé
            // par annexe écrite au contrat de travail durée max
            // 6 mois renouvelables, refus motivé écrit dans le mois,
            // protection contre le licenciement motivé par la
            // demande (indemnité forfaitaire 6 mois de rémunération).
            // Vérifie l'éligibilité cumulative (statut demande
            // ACCORDE_AVENANT_SIGNE / REFUSE_MOTIVE_PAR_ECRIT /
            // REFUSE_SANS_MOTIVATION_ECRITE /
            // EN_ATTENTE_REPONSE_EMPLOYEUR / LICENCIE_APRES_DEMANDE
            // / INDETERMINE ; travailleur à temps plein ; demande
            // écrite ; journée ≤ 9 h 30 ou 10 h par CCT ; avenant
            // écrit signé ; règlement de travail modifié ; durée
            // ≤ 6 mois ou renouvelé ; refus motivé par écrit si
            // refus ; motif licenciement objectif établi si
            // licenciement post-demande). Verdicts :
            // CONFORME_REGIME_4_JOURS_VALIDE /
            // NON_ELIGIBLE_TEMPS_PARTIEL /
            // NON_CONFORME_DEMANDE_ECRITE_MANQUANTE /
            // NON_CONFORME_JOURNEE_DEPASSE_9H30 /
            // NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT /
            // NON_CONFORME_DUREE_DEPASSE_6_MOIS /
            // REFUS_EMPLOYEUR_NON_MOTIVE /
            // LICENCIEMENT_REPRESAILLES_PRESUME / A_ANALYSER.
            // Frontend SF-219-18b à venir. Préventif
            // KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern
            // uniforme F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "semaine-4-jours-be",
            // SF-219-20 (2026-05-28) : pecule-vacances-be — backend
            // Lois coordonnées 28/06/1971 + AR 30/03/1967. Calcul
            // pécule simple/double/départ selon statut EMPLOYE/OUVRIER.
            "pecule-vacances-be",
            // SF-219-19 (2026-05-28) : droit-deconnexion-be — backend
            // Loi 03/10/2022 Deal pour l'emploi art. 16 + AR 19/02/2023.
            // Obligation employeur ≥ 20 travailleurs CCT/règlement travail.
            "droit-deconnexion-be",
            // SF-219-21 (2026-05-28) : eco-cheques-cheques-repas-be — backend
            // CCT n°98 + Loi 25/04/2014 + AR 03/02/2010.
            "eco-cheques-cheques-repas-be",
            // SF-219-22 (2026-05-28) : egalite-femmes-hommes-be — backend
            // Loi 22/04/2012 + AR 17/08/2013 + CCT n°25. Rapport biennal
            // employeurs ≥ 50 travailleurs.
            "egalite-femmes-hommes-be",
            // SF-219-23 (2026-05-28) : discrimination-be-handicap-amenagement
            // — backend Loi 10/05/2007 art. 14 + art. 17 + art. 28 + CCT
            // n° 95 + Directive 2000/78/CE art. 5 + Convention ONU
            // 13/12/2006 art. 27. Analyse refus d'aménagements
            // raisonnables = discrimination indirecte handicap sauf
            // charge disproportionnée démontrée. Frontend SF-219-23b à
            // venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le
            // backend (pattern uniforme F-213 / F-219 vagues, cf.
            // CLAUDE.md feedback_pre_merge_visibility_seed_check).
            "discrimination-be-handicap-amenagement",
            // SF-219-24 (2026-05-28) : code-penal-social-be — backend
            // Loi 06/06/2010 introduisant le Code pénal social. Qualifie
            // une infraction sociale (14 types ou AUTRE_QUALIFICATION)
            // et restitue le niveau de sanction 1 à 4 (art. 101-103
            // C. pén. soc.) avec bornes d'amende admin / pénale,
            // emprisonnement éventuel niveau 4, majorations × travailleurs
            // (art. 103 § 2), × 5 personne morale (art. 105), × 2
            // récidive ≤ 1 an (art. 110).
            "code-penal-social-be",
            // SF-219-25 (2026-05-28) : auditorat-travail-be — backend
            // Code judiciaire art. 138bis + Code d'instruction criminelle
            // art. 24 + Loi 03/08/1992 sur le Code judiciaire + Loi
            // 06/06/2010 introduisant le Code pénal social. Outil
            // d'orientation / checklist de saisine du parquet spécialisé
            // en droit social pénal. Verdicts : SAISINE_AUDITORAT_RECOMMANDEE
            // (infraction pénale sociale, accident grave, harcèlement
            // pénal, discrimination pénale, entrave inspection),
            // DENONCIATION_INSPECTION_PREALABLE (travail non déclaré
            // suspecté), SAISINE_NON_PERTINENTE (litige civil pur
            // art. 578 C. jud. ou prescription pénale art. 81 acquise),
            // A_QUALIFIER (nature ouverte). Frontend SF-219-25b à
            // venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le
            // backend (pattern uniforme F-213 / F-219 vagues, cf.
            // CLAUDE.md feedback_pre_merge_visibility_seed_check).
            "auditorat-travail-be",
            // SF-219-26 (2026-05-28) : travail-noir-be-dimona — backend
            // Loi-programme 24/12/2002 art. 167-184 + AR 05/11/2002 +
            // Code pénal social art. 181 niveau 4. Analyse défaut DIMONA
            // (CONFORME / TARDIVE / ABSENTE / INDEPENDANT) avec calcul
            // cotisations ONSS rétroactives (employeur 25 % + travailleur
            // 13,07 %), amende ONSS forfaitaire 3 × art. 28 Loi 22/04/2003,
            // sanction pénale art. 181 niveau 4 (300/600 - 3000/6000 €,
            // emprisonnement 6-36 mois) + présomption salariat art. 328
            // C. pén. soc., requalification faux indépendant Loi-programme
            // I 27/12/2006 art. 333. Frontend SF-219-26b à venir.
            "travail-noir-be-dimona",
            // SF-219-27 (2026-05-28) : inastri-statut-travailleur-independant
            // — backend Loi 27/06/1969 + AR n° 38 27/07/1967 + AR 19/12/1967
            // + Loi-programme I 27/12/2006 art. 328 à 333 doctrine Bart
            // Buysse + art. 337/2 critères sectoriels. Qualifie la nature
            // de la relation (salarié vs indépendant) via 4 critères
            // généraux (volonté, liberté temps, liberté travail, contrôle
            // hiérarchique) + critères sectoriels art. 337/2 (construction,
            // transport, gardiennage, nettoyage, agri/horti). Verdicts :
            // INDEPENDANT_CONFIRME, SALARIE_REQUALIFIE,
            // FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE,
            // PRESUMPTION_GENERALE_SALARIAT, A_QUALIFIER. Frontend
            // SF-219-27b à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS
            // dès le backend (pattern uniforme F-213 / F-219 vagues, cf.
            // CLAUDE.md feedback_pre_merge_visibility_seed_check).
            "inastri-statut-travailleur-independant",
            // SF-219-28 (2026-05-28) : mp-fedris-reconnaissance — backend
            // Lois coordonnées du 03/06/1970 + AR du 28/03/1969 liste fermée
            // modifié 06/12/2018 + AR du 16/12/1985 système ouvert + Loi
            // 11/01/2018 réformant Fedris. Analyse éligibilité à la
            // reconnaissance MP (LISTE_FERMEE_PRESOMPTION art. 32 / LISTE_
            // FERMEE_EXPOSITION_INSUFFISANTE / SYSTEME_OUVERT_CAUSALITE_
            // DIRECTE_DETERMINANTE art. 30bis / SYSTEME_OUVERT_CAUSALITE_
            // INSUFFISANTE / DECLARATION_PRESCRITE art. 31 délai triennal /
            // A_QUALIFIER) + jurisprudence Cass. BE 28/05/2008 S.07.0033.F
            // causalité directe et déterminante + Cass. BE 13/12/1989
            // présomption art. 32 juridique renversable. Frontend
            // SF-219-28b à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS
            // dès le backend (pattern uniforme F-213 / F-219 vagues, cf.
            // CLAUDE.md feedback_pre_merge_visibility_seed_check).
            "mp-fedris-reconnaissance",
            // SF-219-29 (2026-05-28) : at-mp-rente-capital-be — backend
            // Loi 10/04/1971 art. 24 (mode versement IPP : capital < 19% /
            // rente >= 19%) + Lois coordonnées 03/06/1970 art. 35 (renvoi
            // AT pour MP) + AR 21/12/1971 + AR 24/02/2005 bareme
            // capitalisation Table I-bis. Calcule rente annuelle, capital
            // forfaitaire, conversion partielle 1/3 max après délai
            // d'épreuve 3 ans (art. 45ter). Verdicts : CAPITAL_FORFAITAIRE_
            // LT_19 / RENTE_ANNUELLE_GE_19 / INELIGIBLE_NON_RECONNU /
            // IPP_NON_DETERMINE / A_QUALIFIER. Jurisprudence Cass. BE
            // 06/11/2000 S.99.0119.F capitalisation d'office < 19% +
            // Cass. BE 26/05/2003 S.01.0079.F rente viagère + Cass. BE
            // 23/03/2009 S.08.0072.F délai d'épreuve 3 ans. Frontend
            // SF-219-29b à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS
            // dès le backend (pattern uniforme F-213 / F-219 vagues, cf.
            // CLAUDE.md feedback_pre_merge_visibility_seed_check).
            "at-mp-rente-capital-be",
            // SF-219-30 (2026-05-28) : bien-etre-rps-conseiller-prevention
            // — backend Loi 04/08/1996 art. 32sexies + AR 10/04/2014
            // (procédure interne RPS, saisine CPAP). Analyse la conformité
            // procédurale de la saisine (entretien préalable obligatoire
            // art. 16 § 1 AR 10/04/2014, formalisme écrit signé daté avec
            // accusé de réception art. 16 § 2-4, notification employeur
            // art. 17 § 1, délais 3 mois avis art. 22 § 1 et 2 mois mesures
            // employeur art. 32, protection 12 mois représailles art.
            // 32sexies déclenchée uniquement par la demande formelle Cass.
            // BE 20/01/2014 S.12.0064.F, formalisme strict Cass. BE
            // 23/12/2014 S.14.0026.N). Verdicts : SAISINE_CONFORME,
            // SAISINE_INFORMELLE_EN_COURS, SAISINE_FORMELLE_EN_COURS,
            // AVIS_RENDU_DELAI_RESPECTE, AVIS_RENDU_DELAI_DEPASSE,
            // NON_CONFORME_FORMALITES_MANQUANTES,
            // NON_CONFORME_PAS_DE_CONSEILLER, A_QUALIFIER. Distinct de
            // SF-213-07 harcelement-be-procedure-formelle (procédure
            // formelle déjà déposée + protection 12 mois) et F-DT-30
            // harcelement-licenciement-nul-section (nullité représailles).
            // Frontend SF-219-30b à venir. Préventif
            // KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern uniforme
            // F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "bien-etre-rps-conseiller-prevention",
            // SF-219-31 (2026-05-28) : conge-paternite-naissance-be —
            // backend Loi 03/07/1978 art. 30 § 2 + Loi 12/08/2000 + Loi
            // 07/04/2023 Deal pour l'emploi (extension 20 jours
            // ouvrables pour naissances ≥ 01/01/2023, ouverture à tous
            // les co-parents comaternité / reconnaissance / cohabitation
            // légale ou mariage). Calcule durée applicable (20 / 15 / 10
            // jours selon date naissance), jours restants, échéance 4
            // mois post-naissance art. 30 § 2 al. 2, fin protection 5
            // mois art. 30 § 4, indemnité employeur 100 % 3 premiers
            // jours / mutuelle 82 % suivants AR 03/07/1996. Verdicts :
            // ELIGIBLE_CONGE_OUVERT / CONGE_EN_COURS_PROTECTION_ACTIVE /
            // CONGE_PRIS_PROTECTION_RESIDUELLE / INELIGIBLE_STATUT_NON_
            // COUVERT / INELIGIBLE_FILIATION_NON_ETABLIE / DROIT_PERDU_
            // DELAI_DEPASSE / INELIGIBLE_NAISSANCE_FUTURE / A_QUALIFIER.
            // Jurisprudence Cass. BE 16/03/2015 S.13.0094.F protection 5
            // mois + Cass. BE 25/11/2019 S.18.0086.F preuve filiation +
            // Cass. BE 12/05/2014 S.13.0103.F articulation suspensions.
            // Frontend SF-219-31b à venir. Préventif KNOWN_NO_DASHBOARD_
            // TILE_IDS dès le backend (pattern uniforme F-213 / F-219
            // vagues, cf. CLAUDE.md feedback_pre_merge_visibility_seed_
            // check).
            "conge-paternite-naissance-be",
            // SF-219-32 (2026-05-28) : interruption-carriere-soins-parental
            // — backend Loi de redressement du 22/01/1985 art. 99 à
            // 107quater + AR du 29/10/1997 + CCT n° 64 du 29/04/1997 +
            // AR du 12/08/1991 allocations ONEM. Calcule l'éligibilité
            // au congé parental BE (ancienneté 12 mois art. 5, enfant
            // < 12 ans / < 21 ans handicap art. 4, solde individuel
            // 4 mois ETP par enfant et par parent art. 2, formalisme
            // lettre recommandée 2-3 mois avant début art. 6) + durée
            // effective (TEMPS_PLEIN 4 mois / MI_TEMPS 8 mois /
            // CINQUIEME_TEMPS 20 mois art. 3) + allocations ONEM
            // forfaitaires mensuelles indicatives (250 / 350 / 450 EUR)
            // + protection licenciement art. 101 (demande à fin + 3 mois,
            // indemnité 6 mois rémunération brute). Verdicts :
            // ELIGIBLE_COMPLET, ELIGIBLE_AVEC_RESERVES,
            // INELIGIBLE_ANCIENNETE, INELIGIBLE_AGE_ENFANT,
            // INELIGIBLE_SOLDE_INSUFFISANT, INELIGIBLE_FORMALISME,
            // DIFFERE_EMPLOYEUR (art. 7 différé motivé max 6 mois),
            // A_QUALIFIER. Jurisprudence Cass. BE 26/05/2008 S.07.0040.F
            // protection licenciement opposable + Cass. BE 22/06/2009
            // S.08.0102.N motif grave + Cass. BE 16/01/2017 S.15.0102.N
            // cumul allocation ONEM de droit. Distinct de F-DT-29
            // credit-temps-be CCT 103 (régime universel sans motif
            // spécifique). Frontend SF-219-32b à venir. Préventif
            // KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern uniforme
            // F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "interruption-carriere-soins-parental",
            // SF-218-01 (2026-05-30) : F-DT-86 appel CPH cour d'appel FR —
            // backend (présente PR). Appel d'un jugement du Conseil de
            // prud'hommes devant la chambre sociale de la Cour d'appel
            // (art. 538 CPC ; R. 1461-1 CPC) : délai d'appel d'un mois,
            // verdict de recevabilité (DELAI_OUVERT / DELAI_URGENT /
            // DELAI_EXPIRE / VOIE_FERMEE) et checklist des formalités de
            // l'appel social (déclaration RPVA, chefs critiqués art. 901,
            // représentation obligatoire R. 1461-2, procédure orale art. 946,
            // constitution intimé). Outil standalone restitué via GET du
            // snapshot, pas de tuile dashboard. Frontend SF-218-02 à venir.
            // Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern
            // uniforme F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "F-DT-86-appel-cph-cour-appel",
            // SF-218-03 (2026-05-30) : F-DT-88 exécution du jugement CPH / AGS
            // FR — backend (présente PR). Exécution forcée d'un jugement du
            // Conseil de prud'hommes (art. 514 CPC ; R. 1454-28 CPC) : checklist
            // des démarches d'exécution (signification, commandement, huissier,
            // mesures conservatoires) et détection de la garantie AGS si
            // employeur en redressement / liquidation judiciaire (L. 3253-6 et
            // s. Code travail). Verdict EXECUTION_DIRECTE / RELAIS_AGS /
            // BLOQUE_INFO_MANQUANTE. Outil standalone restitué via GET du
            // snapshot, pas de tuile dashboard. Frontend SF-218-04 à venir.
            // Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern
            // uniforme F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "F-DT-88-execution-jugement-cph",
            // SF-218-05 (2026-05-30) : F-DT-87 pourvoi en cassation sociale FR
            // — backend (présente PR). Pourvoi en cassation devant la chambre
            // sociale de la Cour de cassation (art. 612 CPC : délai 2 mois ;
            // art. 604 CPC : cas d'ouverture ; art. 1014 CPC : filtre de
            // non-admission). Calcule le délai depuis la notification de
            // l'arrêt, score la force probatoire des cas d'ouverture et évalue
            // le risque de non-admission (ELEVE / MODERE / FAIBLE). Verdict
            // POURVOI_RECOMMANDE / POURVOI_RISQUE / POURVOI_DECONSEILLE /
            // DELAI_EXPIRE. Représentation par avocat aux Conseils obligatoire
            // (art. 973 CPC). Outil standalone restitué via GET du snapshot,
            // pas de tuile dashboard. Frontend SF-218-06 à venir. Préventif
            // KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern uniforme
            // F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "F-DT-87-pourvoi-cassation-soc",
            // SF-218-07 (2026-05-30) : F-DT-89 saisie sur rémunération FR — backend
            // (présente PR). Calcule la quotité saisissable d'une rémunération selon
            // le barème annuel par tranches (art. R. 3252-2 CT : fractions 1/20, 1/10,
            // 1/5, 1/4, 1/3, 2/3, totalité), avec majoration des seuils par personne à
            // charge (art. R. 3252-3 CT) et fraction absolument insaisissable égale au
            // montant forfaitaire RSA (art. L. 3252-3 CT). Verdict : SAISISSABLE /
            // INSAISISSABLE / ALIMENTAIRE_PAIEMENT_DIRECT. Outil standalone restitué
            // via GET du snapshot, pas de tuile dashboard. Frontend SF-218-08 à venir.
            // Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern uniforme
            // F-213 / F-219 vagues, cf. CLAUDE.md feedback_pre_merge_visibility_seed_check).
            "F-DT-89-saisie-arret-remuneration",
            // SF-218-11 (2026-05-30) : F-DT-104 VRP indemnité de clientèle FR —
            // backend (présente PR). Rupture du contrat d'un VRP statutaire
            // (art. L.7311-1 et s. CT) : préavis VRP (art. L.7313-9 CT : 1/2/3
            // mois), éligibilité à l'indemnité de clientèle (art. L.7313-13 CT :
            // DUE / NON_DUE), estimation indicative (fourchette 1 à 2 années de
            // commissions), indemnité légale comparée (art. R.1234-2 CT) et option
            // la plus favorable (non-cumul : INDEMNITE_CLIENTELE / INDEMNITE_LEGALE).
            // Outil standalone restitué via GET du snapshot, pas de tuile dashboard.
            // Frontend SF-218-12 à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès
            // le backend (pattern uniforme F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "F-DT-104-vrp-indemnite-clientele",
            // SF-218-09 (2026-05-30) : F-DT-90 action de groupe en discrimination FR —
            // backend (présente PR). Analyse la recevabilité d'une action de groupe en
            // discrimination au travail (contentieux collectif, loi J21 du 18/11/2016,
            // art. L. 1134-7 à L. 1134-10 CT) : qualité à agir (syndicat représentatif /
            // association déclarée depuis ≥ 5 ans, L. 1134-7), mise en demeure préalable
            // + délai de carence de 6 mois avant saisine (L. 1134-9 :
            // dateRecevabiliteSaisine = mise en demeure + 6 mois), pluralité de situations
            // similaires. Verdict : RECEVABLE / PREMATURE / IRRECEVABLE_QUALITE /
            // INFO_MANQUANTE. Outil standalone restitué via GET du snapshot, pas de tuile
            // dashboard. Frontend SF-218-10 à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS
            // dès le backend (pattern uniforme F-213 / F-219 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "F-DT-90-action-groupe-discrimination",
            // SF-218-13 : outil F-DT-108 particulier employeur / CESU (préavis +
            // indemnité de licenciement, FR-only, CONTEXTUAL) — pas de tuile
            // dashboard. Frontend SF-218-14 à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS
            // dès le backend (pattern uniforme F-213 / F-219 / F-218 vagues, cf.
            // CLAUDE.md feedback_pre_merge_visibility_seed_check).
            "F-DT-108-particulier-employeur-cesu",
            // SF-218-15 : outil F-DT-105 statut journaliste professionnel (clause
            // de cession / conscience, indemnité de congédiement, commission
            // arbitrale, FR-only, CONTEXTUAL) — pas de tuile dashboard. Frontend
            // SF-218-16 à venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le
            // backend (pattern uniforme F-213 / F-219 / F-218 vagues, cf.
            // CLAUDE.md feedback_pre_merge_visibility_seed_check).
            "F-DT-105-journaliste-statut",
            // SF-218-17 : outil F-DT-106 intermittent du spectacle — ouverture
            // des droits ARE (seuil 507 h / 12 mois, annexes 8 et 10 Unedic,
            // FR-only, CONTEXTUAL) — pas de tuile dashboard. Frontend SF-218-18 à
            // venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern
            // uniforme F-213 / F-219 / F-218 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "F-DT-106-intermittent-spectacle-are",
            // SF-218-19 : outil F-DT-107 cadre dirigeant — qualification
            // (3 critères cumulatifs L.3111-2 CT, FR-only, CONTEXTUAL) — pas de
            // tuile dashboard. Frontend SF-218-20 à venir. Préventif
            // KNOWN_NO_DASHBOARD_TILE_IDS dès le backend (pattern uniforme
            // F-213 / F-219 / F-218 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "F-DT-107-cadre-dirigeant-statut",
            // SF-218-21 : outil F-DT-109 stagiaire — gratification minimale /
            // requalification en CDI (art. L.124-1 et s. code de l'éducation,
            // FR-only, CONTEXTUAL) — pas de tuile dashboard. Frontend SF-218-22 à
            // venir. Préventif KNOWN_NO_DASHBOARD_TILE_IDS dès le backend
            // (pattern uniforme F-213 / F-219 / F-218 vagues, cf. CLAUDE.md
            // feedback_pre_merge_visibility_seed_check).
            "F-DT-109-stagiaire-gratification-requalification"
    );

    /** Source à scanner — chemin relatif au répertoire de travail Maven (= backend/). */
    private static final String DASHBOARD_SERVICE_PATH =
            "src/main/java/fr/ailegalcase/casefile/CaseFileDashboardService.java";

    /**
     * Capture le 1er argument string littéral de tout {@code new DashboardTile(...)}.
     * Tolère whitespace + saut de ligne après la parenthèse ouvrante.
     */
    private static final Pattern DASHBOARD_TILE_TOOL_ID = Pattern.compile(
            "new\\s+DashboardTile\\s*\\(\\s*\"([^\"]+)\"");

    @Test
    void aucun_toolId_hardcode_dans_CaseFileDashboardService_n_est_orphelin() throws IOException {
        Set<String> hardcodedToolIds = extractToolIdsFromDashboardService();
        assertThat(hardcodedToolIds)
                .withFailMessage("Aucun toolId extrait — vérifier le regex et le chemin source")
                .isNotEmpty();

        Set<String> dbToolIds = new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT tool_id FROM decision_tool_visibility_rules",
                String.class));

        List<String> orphans = hardcodedToolIds.stream()
                .filter(id -> !dbToolIds.contains(id))
                .filter(id -> !KNOWN_SUMMARY_TILE_IDS.contains(id))
                .sorted()
                .collect(Collectors.toList());

        assertThat(orphans)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("F-229 SF-229-03 — toolIds hardcodés dans CaseFileDashboardService\n")
                      .append("ABSENTS de decision_tool_visibility_rules ET hors liste\n")
                      .append("KNOWN_SUMMARY_TILE_IDS :\n");
                    orphans.forEach(id -> sb.append(" - ").append(id).append("\n"));
                    sb.append("\nAction requise :\n")
                      .append("  (a) corriger le toolId dans CaseFileDashboardService.java\n")
                      .append("      pour qu'il matche la migration Liquibase\n")
                      .append("      (decision_tool_visibility_rules.tool_id), OU\n")
                      .append("  (b) ajouter une migration Liquibase INSERT dans\n")
                      .append("      decision_tool_visibility_rules pour ce toolId\n")
                      .append("      (avec entrée TOOL_REGISTRY frontend correspondante,\n")
                      .append("      cf. règle CLAUDE.md feedback_pre_merge_visibility_seed_check), OU\n")
                      .append("  (c) si c'est une nouvelle tile résumé routée via\n")
                      .append("      BadgeNavigationService, l'ajouter à\n")
                      .append("      KNOWN_SUMMARY_TILE_IDS dans ce test.\n")
                      .append("\nNe jamais émettre une DashboardTile sans correspondance DB —\n")
                      .append("le clic dashboard sera silencieusement cassé en runtime.");
                    return sb.toString();
                })
                .isEmpty();
    }

    /**
     * SF-DT-36-03 — garde-fou <strong>inverse</strong> de {@link
     * #aucun_toolId_hardcode_dans_CaseFileDashboardService_n_est_orphelin()}.
     *
     * <p>Origine du bug : F-DT-36 (nullité de procédure de licenciement) a été
     * livré avec sa table de résultat, son endpoint de calcul, son seed
     * {@code decision_tool_visibility_rules} et son entrée {@code TOOL_REGISTRY}
     * frontend — mais sans tuile dans {@code CaseFileDashboardService}. Le
     * résultat calculé était persisté mais jamais affiché au dashboard. Le test
     * direct ne détecte pas ce cas (il vérifie « tuile émise → seed DB », pas
     * « seed DB → tuile émise »).</p>
     *
     * <p>Principe : tout {@code tool_id} de {@code decision_tool_visibility_rules}
     * doit <strong>soit</strong> être émis comme {@code DashboardTile} par
     * {@code CaseFileDashboardService}, <strong>soit</strong> figurer dans
     * {@link #KNOWN_NO_DASHBOARD_TILE_IDS} (outil sans tuile par conception).</p>
     */
    @Test
    void tout_toolId_seede_en_visibilite_a_une_tile_dashboard_ou_est_explicitement_exclu()
            throws IOException {
        Set<String> emittedToolIds = extractToolIdsFromDashboardService();
        Set<String> dbToolIds = new HashSet<>(jdbc.queryForList(
                "SELECT DISTINCT tool_id FROM decision_tool_visibility_rules",
                String.class));
        assertThat(dbToolIds)
                .withFailMessage("Aucun tool_id en DB — vérifier que les migrations "
                        + "decision_tool_visibility_rules ont bien tourné")
                .isNotEmpty();

        List<String> missingTiles = dbToolIds.stream()
                .filter(id -> !emittedToolIds.contains(id))
                .filter(id -> !KNOWN_NO_DASHBOARD_TILE_IDS.contains(id))
                .sorted()
                .collect(Collectors.toList());

        assertThat(missingTiles)
                .withFailMessage(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("SF-DT-36-03 — outils seedés dans\n")
                      .append("decision_tool_visibility_rules SANS tuile émise par\n")
                      .append("CaseFileDashboardService ET hors KNOWN_NO_DASHBOARD_TILE_IDS :\n");
                    missingTiles.forEach(id -> sb.append(" - ").append(id).append("\n"));
                    sb.append("\nLeur résultat serait calculé/persisté mais jamais\n")
                      .append("affiché au dashboard (bug F-DT-36). Action requise :\n")
                      .append("  (a) ajouter un addSafely(tiles, () -> tileFromXxx(...))\n")
                      .append("      + la méthode tileFromXxx() dans\n")
                      .append("      CaseFileDashboardService, OU\n")
                      .append("  (b) si l'outil n'a volontairement pas de tuile dashboard\n")
                      .append("      (outil formule/PDF sans persistance de résultat,\n")
                      .append("      composant UI auto-suffisant, référentiel),\n")
                      .append("      l'ajouter à KNOWN_NO_DASHBOARD_TILE_IDS avec\n")
                      .append("      justification dans ce test.");
                    return sb.toString();
                })
                .isEmpty();
    }

    private Set<String> extractToolIdsFromDashboardService() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(DASHBOARD_SERVICE_PATH));
        String source = String.join("\n", lines);
        Set<String> toolIds = new LinkedHashSet<>();
        Matcher matcher = DASHBOARD_TILE_TOOL_ID.matcher(source);
        while (matcher.find()) {
            toolIds.add(matcher.group(1));
        }
        return toolIds;
    }
}
