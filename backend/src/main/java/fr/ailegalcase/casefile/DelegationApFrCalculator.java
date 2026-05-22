package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;

/**
 * SF-216-09 : calculateur statique de recevabilité d'une délégation d'autorité
 * parentale (art. 376-1 Cciv + art. L. 228-1 CASF + Cass. 1ère civ. 22/11/2005).
 *
 * <p>Arbre décisionnel :</p>
 * <ol>
 *   <li>Si l'enfant est majeur (ageEnfant >= 18) →
 *       {@link VerdictRecevabiliteDelegationApEnum#IRRECEVABLE_ENFANT_MAJEUR}
 *       (l'autorité parentale s'éteint à 18 ans, art. 371-1 Cciv — la délégation
 *       est sans objet).</li>
 *   <li>Si {@code typeDelegation == VOLONTAIRE_CONJOINTE} :
 *     <ul>
 *       <li>Avec accord des deux parents → {@link
 *           VerdictRecevabiliteDelegationApEnum#RECEVABLE_VOLONTAIRE}, voie
 *           non contentieuse (acte notarié ou déclaration JAF).</li>
 *       <li>Sans accord → bascule vers la voie judiciaire tiers (art. 376-1 al. 2)
 *           ou {@link VerdictRecevabiliteDelegationApEnum#IRRECEVABLE_VOIE_INADEQUATE}
 *           si aucune autre voie n'est documentée.</li>
 *     </ul>
 *   </li>
 *   <li>Si {@code typeDelegation == JUDICIAIRE_DESINTERET} :
 *     <ul>
 *       <li>Désintérêt > 1 an documenté → {@link
 *           VerdictRecevabiliteDelegationApEnum#RECEVABLE_JUDICIAIRE_DESINTERET}.</li>
 *       <li>Désintérêt non caractérisé → bascule sur tiers (si motivation
 *           présente) ou irrecevable.</li>
 *     </ul>
 *   </li>
 *   <li>Si {@code typeDelegation == JUDICIAIRE_TIERS} → {@link
 *       VerdictRecevabiliteDelegationApEnum#RECEVABLE_JUDICIAIRE_TIERS}
 *       (requête JAF sur l'art. 376-1 al. 2).</li>
 * </ol>
 *
 * <p>Alertes contextuelles :</p>
 * <ul>
 *   <li>Enfant &lt; 18 mois : délégation rare (la jurisprudence retient
 *       difficilement l'intérêt de l'enfant).</li>
 *   <li>Danger caractérisé : orienter en parallèle vers le retrait d'autorité
 *       parentale (SF-216-11/12, art. 378+ Cciv).</li>
 *   <li>Association habilitée tierce : vérifier l'habilitation art. L. 228-1
 *       CASF.</li>
 * </ul>
 *
 * <p>Gate country : uniquement FRANCE. En Belgique, l'équivalent est la
 * tutelle officieuse / délégation d'autorité parentale art. 475ter Code civil
 * BE — outil distinct hors périmètre F-216.</p>
 */
public final class DelegationApFrCalculator {

    static final String BASE_JURIDIQUE =
            "art. 376-1 Cciv (délégation d'autorité parentale)"
                    + " + art. 371-1 Cciv (durée AP)"
                    + " + art. L. 228-1 CASF (associations habilitées)"
                    + " + Cass. 1ère civ., 22/11/2005 (conditions délégation judiciaire)";

    /** Âge sous lequel la délégation est rare (jurisprudence). */
    static final int AGE_BAS_INTERET_ENFANT_MOIS = 18;

    /** Majorité civile — au-delà, la délégation est sans objet (art. 371-1). */
    static final int AGE_MAJORITE = 18;

    private DelegationApFrCalculator() {}

    /**
     * Calcule la recevabilité et la voie procédurale d'une délégation d'AP.
     *
     * @param req     requête validée (gates pays/domaine vérifiés par le service)
     * @param country pays du workspace ("FRANCE" attendu)
     * @return résultat du calcul
     * @throws IllegalArgumentException si {@code country != FRANCE}.
     */
    public static DelegationApFrResult compute(DelegationApFrRequest req, String country) {

        if (!"FRANCE".equals(country)) {
            throw new IllegalArgumentException(
                    "Outil F-FA-XX-delegation-ap applicable uniquement en France (art. 376-1 Cciv).");
        }

        List<String> etapes = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        List<String> alertes = new ArrayList<>();

        int age = req.ageEnfant() == null ? 0 : req.ageEnfant();
        boolean accord = Boolean.TRUE.equals(req.accordParents());
        boolean danger = Boolean.TRUE.equals(req.dangerCaracterise());
        boolean desinteret = Boolean.TRUE.equals(req.desinteretParentPlusUnAn());
        boolean decisionsPrec = Boolean.TRUE.equals(req.decisionsJudiciairesPrecedentes());
        TypeDelegationApEnum demande = req.typeDelegation();
        TiersLienFamilialEnum tiers = req.tiersLienFamilial();

        // ── 1. Cas d'irrecevabilité : enfant majeur ────────────────────────────
        if (age >= AGE_MAJORITE) {
            messages.add("L'autorité parentale s'éteint à la majorité (art. 371-1 Cciv). "
                    + "Une délégation d'AP est sans objet pour un enfant majeur.");
            return new DelegationApFrResult(
                    VerdictRecevabiliteDelegationApEnum.IRRECEVABLE_ENFANT_MAJEUR,
                    demande,
                    etapes,
                    0,
                    BASE_JURIDIQUE,
                    messages,
                    alertes);
        }

        // ── 2. Détermination de la voie procédurale effective ──────────────────
        VerdictRecevabiliteDelegationApEnum verdict;
        TypeDelegationApEnum voie;
        int dureeJours;

        if (demande == TypeDelegationApEnum.VOLONTAIRE_CONJOINTE) {
            if (accord) {
                verdict = VerdictRecevabiliteDelegationApEnum.RECEVABLE_VOLONTAIRE;
                voie = TypeDelegationApEnum.VOLONTAIRE_CONJOINTE;
                dureeJours = 60;
                etapes.add("Rédiger l'acte de délégation devant notaire OU saisir le JAF par "
                        + "déclaration conjointe (art. 376-1 al. 1 Cciv).");
                etapes.add("Réunir les pièces : acte de naissance de l'enfant, justificatifs "
                        + "d'identité des deux parents et du tiers, lettre d'acceptation du "
                        + "tiers, motivation écrite (intérêt supérieur de l'enfant).");
                etapes.add("Si tiers = association habilitée : joindre l'arrêté préfectoral "
                        + "d'habilitation (art. L. 228-1 CASF).");
                etapes.add("Homologation par ordonnance du JAF — audience non contradictoire "
                        + "courte, délai moyen 6 à 8 semaines.");
                etapes.add("Notification de la décision aux deux parents, au tiers délégataire "
                        + "et inscription en marge de l'acte d'état civil de l'enfant.");
                messages.add("La délégation volontaire conjointe est révocable à tout moment "
                        + "par requête conjointe ou décision du JAF dans l'intérêt de l'enfant "
                        + "(art. 376-1 al. 3 Cciv).");
            } else {
                // Sans accord, la voie volontaire conjointe est fermée — bascule judiciaire tiers.
                verdict = VerdictRecevabiliteDelegationApEnum.RECEVABLE_JUDICIAIRE_TIERS;
                voie = TypeDelegationApEnum.JUDICIAIRE_TIERS;
                dureeJours = 180;
                alertes.add("Voie demandée VOLONTAIRE_CONJOINTE inapplicable : accord des deux "
                        + "parents non documenté. Bascule possible vers la voie judiciaire tiers "
                        + "(art. 376-1 al. 2) — requête contentieuse au JAF.");
                etapes.add("Saisir le JAF par requête (art. 376-1 al. 2 Cciv) au nom du tiers "
                        + "candidat à la délégation.");
                etapes.add("Motiver la requête sur l'intérêt supérieur de l'enfant : situation "
                        + "de fait stabilisée, lien affectif avec le tiers, défaut d'exercice "
                        + "effectif de l'AP par les parents.");
                etapes.add("Notifier la requête aux deux parents (procédure contradictoire).");
                etapes.add("Audience JAF contradictoire — éventuellement audition du mineur "
                        + "doué de discernement (art. 388-1 Cciv).");
                messages.add("Délégation judiciaire tiers : succès subordonné à la preuve "
                        + "concrète de l'intérêt de l'enfant et de la stabilité de la situation "
                        + "(Cass. 1ère civ., 22/11/2005).");
            }
        } else if (demande == TypeDelegationApEnum.JUDICIAIRE_DESINTERET) {
            if (desinteret) {
                verdict = VerdictRecevabiliteDelegationApEnum.RECEVABLE_JUDICIAIRE_DESINTERET;
                voie = TypeDelegationApEnum.JUDICIAIRE_DESINTERET;
                dureeJours = 240;
                etapes.add("Documenter le désintérêt manifeste depuis plus d'un an : absence "
                        + "de contact, défaut de pension alimentaire, absence d'exercice "
                        + "concret de l'AP (preuves : attestations, mises en demeure, "
                        + "constats d'huissier, courriers restés sans réponse).");
                etapes.add("Saisir le JAF par requête fondée sur l'art. 376-1 al. 2 Cciv "
                        + "(désintérêt manifeste).");
                etapes.add("Notifier la requête au parent défaillant (signification par "
                        + "huissier — adresse dernière connue + procédure de domicile inconnu "
                        + "si nécessaire, art. 659 CPC).");
                etapes.add("Audience contradictoire — l'autre parent peut être entendu et "
                        + "contester. Audition du mineur si demandé (art. 388-1 Cciv).");
                etapes.add("Jugement statuant sur la délégation + sur les modalités d'exercice "
                        + "de l'AP par le tiers.");
                messages.add("Désintérêt manifeste > 1 an : critère jurisprudentiel apprécié "
                        + "in concreto (Cass. 1ère civ., 22/11/2005). La délégation est "
                        + "révocable si le parent défaillant manifeste à nouveau son intérêt.");
            } else {
                verdict = VerdictRecevabiliteDelegationApEnum.IRRECEVABLE_VOIE_INADEQUATE;
                voie = TypeDelegationApEnum.JUDICIAIRE_DESINTERET;
                dureeJours = 0;
                alertes.add("Voie JUDICIAIRE_DESINTERET demandée mais le désintérêt > 1 an "
                        + "n'est pas documenté. Examiner la voie JUDICIAIRE_TIERS (art. 376-1 "
                        + "al. 2 — tiers exerçant déjà l'AP de fait) ou compléter le dossier "
                        + "de preuves du désintérêt avant saisine.");
                etapes.add("Rassembler les preuves concrètes du désintérêt parental : "
                        + "attestations de tiers, courriers restés sans réponse, défaut "
                        + "de pension, absence de visite.");
                messages.add("Le désintérêt manifeste s'apprécie sur la durée (≥ 1 an) ET "
                        + "l'intensité (absence totale, non simple éloignement).");
            }
        } else {
            // typeDelegation == JUDICIAIRE_TIERS
            verdict = VerdictRecevabiliteDelegationApEnum.RECEVABLE_JUDICIAIRE_TIERS;
            voie = TypeDelegationApEnum.JUDICIAIRE_TIERS;
            dureeJours = 180;
            etapes.add("Saisir le JAF par requête au nom du tiers (art. 376-1 al. 2 Cciv).");
            etapes.add("Documenter l'exercice de fait de l'AP par le tiers : prise en charge "
                    + "quotidienne, scolarité, soins, hébergement durable.");
            etapes.add("Notifier la requête aux deux parents (procédure contradictoire).");
            etapes.add("Audience JAF — éventuelle audition du mineur doué de discernement "
                    + "(art. 388-1 Cciv).");
            etapes.add("Jugement délimitant les actes d'AP délégués (intégralité ou partage "
                    + "art. 377-1 Cciv).");
            messages.add("La délégation judiciaire au profit d'un tiers peut être partielle "
                    + "(certains actes seulement) ou totale, selon l'intérêt de l'enfant.");
        }

        // ── 3. Alertes contextuelles ───────────────────────────────────────────
        if (age * 12 < AGE_BAS_INTERET_ENFANT_MOIS && age == 0) {
            // ageEnfant = 0 → moins d'un an. Délégation rare.
            alertes.add("Enfant en bas âge (moins de 18 mois) : la jurisprudence retient "
                    + "rarement l'intérêt de la délégation à ce stade (lien biologique "
                    + "encore fortement protégé). Vérifier l'absolue nécessité avant saisine.");
        }
        if (danger) {
            alertes.add("Danger caractérisé pour l'enfant détecté : orienter en parallèle vers "
                    + "le RETRAIT d'autorité parentale (art. 378 et 378-1 Cciv — voir outil "
                    + "F-FA-XX-retrait-ap) plutôt que la seule délégation. Saisine éventuelle "
                    + "du procureur de la République et du JE en assistance éducative.");
        }
        if (tiers == TiersLienFamilialEnum.ASSOCIATION_HABILITEE) {
            alertes.add("Tiers = association : vérifier l'habilitation préfectorale de "
                    + "l'association à recevoir une délégation d'AP (art. L. 228-1 CASF). "
                    + "Sans habilitation, la délégation est irrecevable.");
        }
        if (decisionsPrec) {
            messages.add("Décisions judiciaires antérieures concernant l'enfant signalées : "
                    + "joindre l'historique procédural complet (jugements JAF, décisions JE) "
                    + "pour éclairer le JAF.");
        }

        return new DelegationApFrResult(
                verdict,
                voie,
                etapes,
                dureeJours,
                BASE_JURIDIQUE,
                messages,
                alertes);
    }
}
