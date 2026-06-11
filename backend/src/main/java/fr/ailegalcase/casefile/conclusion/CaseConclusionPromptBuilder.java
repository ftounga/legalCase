package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * F-98 — assemble le prompt système et le message utilisateur du générateur de
 * conclusions.
 *
 * <p>Le prompt système porte les instructions de rédaction (stables, donc cachables
 * par {@code AnthropicService.analyzeWithSystemCache}). Le message utilisateur porte
 * les données du dossier : stade procédural, synthèse, pièces numérotées, verdicts des
 * outils décisionnels et pistes stratégiques retenues.</p>
 *
 * <p>Le prompt système de base dépend de la cellule de matrice
 * ({@link CombinationKey}) : il est fourni par le {@link ConclusionPromptProvider}
 * correspondant, résolu via {@link ConclusionPromptRegistry}. La consigne de style
 * F-98-47 est appliquée par-dessus, identiquement pour toutes les cellules.</p>
 */
@Component
public class CaseConclusionPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(CaseConclusionPromptBuilder.class);

    /**
     * F-98 / SF-98-47 — en-tête de la consigne d'adaptation de style, injectée au prompt
     * système quand le cabinet dispose d'au moins une signature de style active.
     */
    static final String STYLE_INSTRUCTION_HEADER =
            "Adopte le style rédactionnel suivant, appris des conclusions de l'avocat :";

    /**
     * F-242 / SF-242-01 — garde anti-hallucination transverse sur la jurisprudence,
     * appliquée à toutes les cellules de matrice (symétrique du « n'invente aucun
     * chiffre » des prompts de base). L'IA ne doit citer que les références qui lui ont
     * été explicitement fournies dans la section JURISPRUDENCE À L'APPUI.
     */
    static final String JURISPRUDENCE_GUARD =
            "Garde jurisprudence : ne cite aucune référence de jurisprudence (arrêt, "
                    + "décision, numéro de pourvoi) qui ne figure pas dans l'une des trois "
                    + "sections « JURISPRUDENCE À L'APPUI » (F-242, références saisies par "
                    + "l'avocat), « JURISPRUDENCE APPLICABLE PAR OUTIL » (F-JU-02, arrêts "
                    + "curatés par LegalCase pour les outils décisionnels utilisés) ou "
                    + "« JURISPRUDENCE ADVERSE À RÉFUTER » (F-98, citations invoquées par la "
                    + "partie adverse). Si l'une de ces sections contient des références, "
                    + "appuie-toi exclusivement sur celles-ci ; sinon, n'invente aucun arrêt "
                    + "ni décision. Cas particulier de la section « JURISPRUDENCE ADVERSE À "
                    + "RÉFUTER » : ces arrêts ne doivent JAMAIS être cités « avec autorité » "
                    + "comme s'ils soutenaient ta thèse — ils ne servent QU'À être réfutés "
                    + "(arrêt introuvable, portée dénaturée). N'expose jamais le statut "
                    + "technique d'une citation (« suspecte », « non trouvée ») ni un nom de "
                    + "fichier dans l'acte. Pertinence : même si une référence figure dans "
                    + "« JURISPRUDENCE APPLICABLE PAR OUTIL », ne l'utilise QUE si elle éclaire "
                    + "réellement le moyen ou le fait débattu ; ne plaque pas un arrêt non topique.";

    /**
     * F-98 / SF-98-55 — garde de qualité rédactionnelle commune à toutes les cellules
     * (mécanisme symétrique de {@link #JURISPRUDENCE_GUARD}). Elle transforme en
     * garanties de trame ce que le modèle ne faisait que « par chance » : interdiction
     * du jargon interne dans l'acte, syllogisme avec visa des articles, dispositif
     * complet. Les verdicts d'outils et la jurisprudence fournis sont une matière
     * première INTERNE — jamais un contenu à recopier tel quel.
     */
    static final String REDACTION_QUALITY_GUARD =
            "Garde de qualité rédactionnelle (impérative) :\n"
                    + "1. Aucun jargon interne. Les « verdicts des outils décisionnels » et la "
                    + "« jurisprudence applicable par outil » te sont fournis comme matière première "
                    + "INTERNE. Ne mentionne JAMAIS dans le texte un outil de LegalCase, son intitulé "
                    + "d'outil, son code (ex. « F-DT-08 ») ni un score brut (ex. « 2 critères sur 7 », "
                    + "« niveau de risque ÉLEVÉ », « INVALIDE »). Traduis-les en moyens et arguments de "
                    + "droit, comme le ferait un avocat.\n"
                    + "2. Syllogisme. Pour chaque moyen : énonce la règle de droit en VISANT l'article "
                    + "applicable (ex. « art. L. 1235-3 du Code du travail »), applique-la aux faits, "
                    + "rattache-la aux pièces (Pièce n° X), puis tire la conséquence juridique.\n"
                    + "3. Dispositif complet. Dans le « PAR CES MOTIFS », reprends les chefs chiffrés "
                    + "fournis et, lorsque le stade et la juridiction le justifient, n'omets pas les "
                    + "postes systématiques : article 700 du Code de procédure civile, dépens, "
                    + "exécution provisoire, intérêts au taux légal et leur capitalisation "
                    + "(art. 1343-2 du Code civil), et astreinte sur la remise des documents. "
                    + "Actualisation « sauf à parfaire » (F-273) : les montants évoluent jusqu'à "
                    + "l'audience. Assortis de la réserve « sauf à parfaire à la date de l'audience » "
                    + "(ou « sauf mémoire ») les seuls chefs réellement ÉVOLUTIFS — rappels de salaire "
                    + "et indemnités fonction du temps écoulé ou du salaire, intérêts qui continuent de "
                    + "courir ; n'appose JAMAIS cette réserve sur un montant définitivement arrêté "
                    + "(préjudice forfaitaire, somme liquide non évolutive). Pour les intérêts, précise "
                    + "le point de départ pertinent par chef (mise en demeure, saisine, ou décision "
                    + "selon la nature de la créance — art. 1231-6 et 1231-7 du Code civil). N'invente "
                    + "AUCUNE date ni AUCUN montant non fondé par le dossier : cette réserve est "
                    + "qualitative, elle n'autorise aucun recalcul ni réécriture des chiffres fournis.\n"
                    + "4. Faits et procédure : expose une chronologie claire et rappelle le cadre "
                    + "procédural pertinent. Reste sobre et strictement juridique.\n"
                    + "5. Demandes subsidiaires. Lorsque la logique juridique le justifie, structure le "
                    + "dispositif en « À titre principal » puis « À titre subsidiaire » : plaide "
                    + "subsidiairement les chefs qui restent dus même si la demande principale est "
                    + "écartée (ex. indemnités légales de rupture — indemnité de licenciement, indemnité "
                    + "compensatrice de préavis, congés payés afférents — dues indépendamment du caractère "
                    + "réel et sérieux du motif ; rappels de salaire incontestables). N'invente AUCUN chef "
                    + "non étayé par les faits, les pièces ou les verdicts fournis : à défaut d'élément "
                    + "fondant une demande subsidiaire, n'en ajoute pas.\n"
                    + "6. Identité des parties. Reprends les identités et adresses fournies dans la "
                    + "section « IDENTITÉ DES PARTIES » ; à défaut mets « [à compléter] », n'invente "
                    + "jamais d'adresse ni d'identité de partie.\n"
                    + "7. Signature. Ne signe JAMAIS avec un nom d'avocat inventé : termine par un "
                    + "emplacement de signature neutre « [Nom et qualité de l'avocat] » que l'avocat "
                    + "complétera.\n"
                    + "8. Réfutation des moyens adverses (SF-261-02). Pour chaque moyen listé dans la "
                    + "section « MOYENS ADVERSES À RÉFUTER » : réfute-le explicitement — démontre qu'il "
                    + "est mal fondé, contredit par les faits et les pièces, ou que sa base juridique "
                    + "est inopérante. N'invente AUCUN moyen adverse non listé dans cette section.\n"
                    + "9. Prudence du pronostic (F-270). N'exprime JAMAIS une probabilité ou un "
                    + "pourcentage chiffré de succès ou d'issue favorable, ni une « chance de gagner ». "
                    + "L'appréciation de l'aléa judiciaire reste qualitative et assortie de la réserve "
                    + "que l'issue dépend de la juridiction saisie et de la formation de jugement.\n"
                    + "10. Conclusions récapitulatives (F-271, art. 768 CPC). Lorsqu'une section "
                    + "« BASE À CONSOLIDER (jeu de conclusions précédent) » est fournie, ce texte est le "
                    + "dernier jeu de conclusions de l'avocat (ses éditions manuelles incluses) : tu "
                    + "produis un jeu RÉCAPITULATIF qui REPREND l'intégralité de ses chefs de demande et "
                    + "de ses moyens, puis les enrichit et les actualise au vu des éléments nouveaux "
                    + "(synthèse, moyens et jurisprudence adverses, pièces). N'ABANDONNE aucun chef de "
                    + "demande ni moyen présent dans la base sans qu'un élément du dossier ne le justifie "
                    + "explicitement — en procédure écrite, un chef non repris est réputé abandonné. "
                    + "Respecte la formulation et le style des passages que l'avocat a rédigés. Cette "
                    + "section est une matière première INTERNE : ne la cite ni ne la commente, consolide-la.";

    /**
     * F-272 / SF-272-01 — positions de <strong>défense</strong> pour lesquelles l'ordre
     * <em>in limine litis</em> de l'article 74 CPC s'applique : le défendeur de première
     * instance, l'intimé en appel et le défendeur au pourvoi. Un demandeur / appelant /
     * requérant ne soulève pas d'exception de procédure <em>in limine litis</em> (il forme
     * une demande), donc la garde ne s'applique qu'à ces positions.
     */
    private static final Set<String> DEFENCE_POSITIONS =
            Set.of("DEFENDEUR", "INTIME", "DEFENDEUR_POURVOI");

    /**
     * F-272 / SF-272-01 — garde d'ossature procédurale du défendeur, conditionnée à la
     * <strong>France</strong> et à une <strong>position de défense</strong>
     * ({@link #DEFENCE_POSITIONS}). Elle impose l'ordre <em>in limine litis</em> de
     * l'article 74 du Code de procédure civile : les exceptions de procédure doivent être
     * soulevées AVANT toute défense au fond et toute fin de non-recevoir, sous peine
     * d'irrecevabilité (forclusion). Les vices de procédure / nullités déjà fournis par les
     * outils décisionnels (matière première INTERNE — non-régression anti-jargon SF-98-55)
     * sont positionnés au bon rang de l'acte, sans rubrique vide quand rien n'est fondé.
     *
     * <p>Mécanisme symétrique de {@link #JURISPRUDENCE_GUARD} et
     * {@link #REDACTION_QUALITY_GUARD} : une garde transverse appliquée par-dessus le
     * prompt de base, jamais dupliquée provider par provider. Transverse aux 3 domaines FR
     * (l'art. 74 CPC ne dépend pas du domaine).</p>
     */
    static final String PROCEDURE_ORDER_GUARD =
            "Ordre des moyens de procédure (impératif — défendeur, procédure civile française) :\n"
                    + "1. In limine litis (article 74 du Code de procédure civile). Si une exception "
                    + "de procédure est fondée (incompétence, nullité de forme ou de fond, litispendance, "
                    + "connexité, exception dilatoire — articles 73 et suivants), soulève-la dans une "
                    + "section « EXCEPTIONS DE PROCÉDURE » placée AVANT toute fin de non-recevoir et AVANT "
                    + "toute défense au fond : à défaut, l'exception est irrecevable (forclusion).\n"
                    + "2. Fins de non-recevoir (article 122 du Code de procédure civile). Place ensuite, "
                    + "dans une section « FINS DE NON-RECEVOIR », les moyens tirés de la prescription, du "
                    + "défaut de qualité ou d'intérêt à agir, de l'autorité de la chose jugée, lorsqu'ils "
                    + "sont fondés.\n"
                    + "3. Défense au fond. Ne discute le fond (réfutation moyen par moyen, demandes "
                    + "reconventionnelles) qu'APRÈS ces deux étapes.\n"
                    + "4. Tissage des vices de procédure. Les éventuels vices de procédure ou cas de "
                    + "nullité qui te sont fournis comme verdicts d'outils doivent être positionnés au "
                    + "bon rang ci-dessus (exception de procédure ou nullité de l'acte attaqué), traduits "
                    + "en moyens de droit visant l'article applicable — jamais sous leur libellé interne.\n"
                    + "5. Signalement si non applicable. N'ajoute les sections « EXCEPTIONS DE PROCÉDURE » "
                    + "et « FINS DE NON-RECEVOIR » QUE si une exception ou une fin de non-recevoir est "
                    + "réellement fondée par les faits, les pièces ou les verdicts fournis. À défaut, "
                    + "n'ajoute pas de rubrique vide et n'invente aucun moyen de procédure.";

    private final ObjectMapper objectMapper;
    private final ConclusionPromptRegistry promptRegistry;

    public CaseConclusionPromptBuilder(ObjectMapper objectMapper,
                                       ConclusionPromptRegistry promptRegistry) {
        this.objectMapper = objectMapper;
        this.promptRegistry = promptRegistry;
    }

    /**
     * F-98 / SF-98-47 — assemble le prompt système de la cellule {@code key} en y
     * intégrant, le cas échéant, une consigne d'adaptation au style rédactionnel appris
     * du cabinet.
     *
     * <p>Le prompt de base provient de la cellule de matrice correspondant à {@code key}
     * (registre {@link ConclusionPromptRegistry}). Quand {@code styleSignatures} contient
     * au moins une description de style non vide, le prompt système reprend ces
     * descriptions sous une consigne « adopte le style rédactionnel suivant... ». Sans
     * signature exploitable, le prompt système est le prompt de base seul.</p>
     *
     * @param key             cellule de matrice du dossier (jamais {@code null})
     * @param styleSignatures descriptions de style actives du workspace (jamais
     *                        {@code null} ; les entrées {@code null} / vides sont ignorées)
     * @return le prompt système, enrichi de la consigne de style si applicable
     * @throws IllegalStateException si aucune cellule ne couvre {@code key}
     */
    public String buildSystemPrompt(CombinationKey key, List<String> styleSignatures) {
        String basePrompt = promptRegistry.systemPrompt(key)
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun provider de prompt pour la combinaison " + key));
        // F-242 — garde anti-hallucination jurisprudence, appliquée à toutes les cellules.
        StringBuilder sb = new StringBuilder(basePrompt);
        sb.append('\n').append(JURISPRUDENCE_GUARD).append('\n');
        // SF-98-55 — garde de qualité rédactionnelle commune (anti-jargon, syllogisme, dispositif).
        sb.append('\n').append(REDACTION_QUALITY_GUARD).append('\n');
        // SF-272-01 — ossature in limine litis (art. 74 CPC), uniquement défendeur FR.
        if (appliesProcedureOrderGuard(key)) {
            sb.append('\n').append(PROCEDURE_ORDER_GUARD).append('\n');
        }
        List<String> usable = sanitizeSignatures(styleSignatures);
        if (!usable.isEmpty()) {
            sb.append(STYLE_INSTRUCTION_HEADER).append('\n');
            for (String signature : usable) {
                sb.append("- ").append(signature.strip()).append('\n');
            }
        }
        return sb.toString();
    }

    /**
     * F-272 / SF-272-01 — détermine si la garde {@link #PROCEDURE_ORDER_GUARD} s'applique
     * à la cellule {@code key} : uniquement les conclusions du <strong>défendeur en
     * procédure française</strong> ({@code country == FRANCE} et position de défense). Le
     * régime belge (Code judiciaire) a sa propre logique et reste hors périmètre (« 3
     * domaines FR » de la directive).
     *
     * @param key cellule de matrice (jamais {@code null})
     * @return {@code true} si la garde d'ordre in limine litis doit être injectée
     */
    static boolean appliesProcedureOrderGuard(CombinationKey key) {
        if (key == null) {
            return false;
        }
        return ProcedureStageCatalog.FRANCE.equals(key.country())
                && DEFENCE_POSITIONS.contains(key.position());
    }

    /** Filtre les signatures de style exploitables (non nulles, non vides). */
    private static List<String> sanitizeSignatures(List<String> styleSignatures) {
        if (styleSignatures == null || styleSignatures.isEmpty()) {
            return List.of();
        }
        List<String> usable = new java.util.ArrayList<>();
        for (String signature : styleSignatures) {
            if (signature != null && !signature.isBlank()) {
                usable.add(signature);
            }
        }
        return usable;
    }

    /**
     * Assemble le message utilisateur à partir des données du dossier.
     *
     * @param input agrégat des intrants du dossier (jamais {@code null} ; ses champs
     *              listes peuvent être vides)
     * @return le message utilisateur prêt pour l'appel IA
     */
    public String buildUserMessage(ConclusionPromptInput input) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== DOSSIER ===\n");
        sb.append("Intitulé du dossier : ").append(nullSafe(input.caseTitle())).append('\n');
        sb.append("Stade procédural : ").append(nullSafe(input.jurisdictionLabel()))
                .append(" — ").append(nullSafe(input.stageLabel()))
                .append(" — ").append(nullSafe(input.positionLabel())).append('\n');

        // F-271 — base récapitulative : le dernier jeu de conclusions (éditions de l'avocat
        // incluses) sert de socle à consolider (art. 768 CPC). Section absente si pas de base.
        appendPreviousRecap(sb, input.previousRecapContent());

        appendSynthesis(sb, input.analysisResultJson());

        appendPartiesIdentity(sb, input.analysisResultJson());

        sb.append("\n=== PIÈCES NUMÉROTÉES DU DOSSIER ===\n");
        if (input.pieces() == null || input.pieces().isEmpty()) {
            sb.append("Aucune pièce numérotée identifiée.\n");
        } else {
            for (ConclusionPromptInput.NumberedPiece p : input.pieces()) {
                sb.append("Pièce n° ").append(p.number()).append(" — ")
                        .append(nullSafe(p.label()));
                if (p.type() != null) {
                    sb.append(" (").append(p.type()).append(')');
                }
                sb.append('\n');
            }
        }

        sb.append("\n=== VERDICTS DES OUTILS DÉCISIONNELS REMPLIS ===\n");
        if (input.toolTiles() == null || input.toolTiles().isEmpty()) {
            sb.append("Aucun outil décisionnel rempli sur ce dossier.\n");
        } else {
            for (DashboardTile t : input.toolTiles()) {
                sb.append("- ").append(nullSafe(t.label())).append(" : ")
                        .append(nullSafe(t.primaryValue()));
                if (t.secondaryValue() != null && !t.secondaryValue().isBlank()) {
                    sb.append(" — ").append(t.secondaryValue());
                }
                sb.append('\n');
            }
        }

        sb.append("\n=== PISTES STRATÉGIQUES RETENUES ===\n");
        if (input.retainedStrategies() == null || input.retainedStrategies().isEmpty()) {
            sb.append("Aucune piste stratégique retenue.\n");
        } else {
            for (ConclusionPromptInput.RetainedStrategy s : input.retainedStrategies()) {
                sb.append("- ").append(nullSafe(s.texte()));
                if (s.baseJuridique() != null && !s.baseJuridique().isBlank()) {
                    sb.append(" [Base juridique : ").append(s.baseJuridique()).append(']');
                }
                sb.append('\n');
            }
        }

        appendJurisprudenceCitations(sb, input.jurisprudenceCitations());
        appendToolJurisprudenceCitations(sb, input.toolJurisprudenceByTool());
        // SF-261-02 — moyens adverses (arguments) AVANT la jurisprudence adverse (citations).
        appendAdverseMoyensToRefute(sb, input.adverseMoyens());
        appendAdverseJurisprudenceToRefute(sb, input.adverseToRefute());

        return sb.toString();
    }

    /**
     * F-271 — section « BASE À CONSOLIDER » : le {@code content} de la dernière version
     * DONE des conclusions du dossier (éditions manuelles de l'avocat incluses). Sert de
     * socle au jeu récapitulatif (art. 768 CPC). La section est <strong>absente</strong>
     * (no-op) à la première génération, ou si le content précédent est nul / vide.
     */
    private void appendPreviousRecap(StringBuilder sb, String previousRecapContent) {
        if (previousRecapContent == null || previousRecapContent.isBlank()) {
            return;
        }
        sb.append("\n=== BASE À CONSOLIDER (jeu de conclusions précédent) ===\n");
        sb.append("Le texte ci-dessous est le DERNIER jeu de conclusions de l'avocat sur ce "
                + "dossier (ses éditions manuelles incluses). Produis un jeu RÉCAPITULATIF qui "
                + "REPREND tous ses chefs de demande et moyens, puis les enrichit et les actualise "
                + "au vu des éléments nouveaux. N'abandonne aucun chef sans justification (art. 768 "
                + "CPC). Ne recopie pas servilement : consolide.\n");
        sb.append(previousRecapContent.strip()).append('\n');
    }

    /**
     * F-261 / SF-261-02 — section des moyens (arguments) de la partie adverse extraits
     * de ses écritures (documents marqués {@code adverse_pleadings}), à réfuter moyen par
     * moyen. La section est <strong>absente</strong> si la liste est vide (no-op).
     *
     * <p>Complémentaire de {@link #appendAdverseJurisprudenceToRefute} : les MOYENS
     * (thèses + fondements + pièces invoqués par l'adversaire) coexistent avec la
     * « JURISPRUDENCE ADVERSE À RÉFUTER » (citations douteuses), sans doublon.</p>
     */
    private void appendAdverseMoyensToRefute(StringBuilder sb, List<AdverseMoyen> moyens) {
        if (moyens == null || moyens.isEmpty()) {
            return;
        }
        sb.append("\n=== MOYENS ADVERSES À RÉFUTER ===\n");
        sb.append("Moyens soutenus par la partie adverse dans ses écritures : réfute "
                + "chacun explicitement — démontre qu'il est mal fondé, contredit par les "
                + "faits et les pièces, ou que sa base juridique est inopérante.\n");
        int index = 1;
        for (AdverseMoyen m : moyens) {
            if (m == null || m.these() == null || m.these().isBlank()) {
                continue;
            }
            sb.append("Moyen ").append(index).append(" — Thèse adverse : ").append(m.these().strip());
            if (m.fondements() != null && !m.fondements().isEmpty()) {
                sb.append(" — Fondements invoqués : ").append(String.join(", ", m.fondements()));
            }
            if (m.piecesInvoquees() != null && !m.piecesInvoquees().isEmpty()) {
                sb.append(" — Pièces invoquées : ").append(String.join(", ", m.piecesInvoquees()));
            }
            sb.append('\n');
            index++;
        }
    }

    /**
     * F-98 / SF-98-56 — section des citations de jurisprudence invoquées par la partie
     * adverse et jugées douteuses (F-179, statut SUSPECT/NOT_FOUND), que l'avocat a
     * marquées comme « adverses à réfuter ». La section est <strong>absente</strong> si
     * la liste est vide (pas de rubrique vide / « néant »).
     *
     * <p>Anti-jargon (non-régression SF-98-55) : on n'expose ni nom de fichier ni statut
     * technique — seulement la référence, l'explication (réalité de l'arrêt) et la
     * position que l'adversaire prête à l'arrêt.</p>
     */
    private void appendAdverseJurisprudenceToRefute(
            StringBuilder sb,
            List<ConclusionPromptInput.AdverseCitationToRefute> adverse) {
        if (adverse == null || adverse.isEmpty()) {
            return;
        }
        sb.append("\n=== JURISPRUDENCE ADVERSE À RÉFUTER ===\n");
        sb.append("Citations de jurisprudence invoquées par la partie adverse et jugées "
                + "douteuses : démontre, pour chacune, pourquoi l'adversaire se trompe "
                + "(arrêt introuvable / portée dénaturée), sans l'admettre comme fondé.\n");
        for (ConclusionPromptInput.AdverseCitationToRefute c : adverse) {
            if (c == null) {
                continue;
            }
            sb.append("- ").append(nullSafe(c.reference()));
            if (c.explication() != null && !c.explication().isBlank()) {
                sb.append(" — ").append(c.explication().strip());
            }
            if (c.positionAlleguee() != null && !c.positionAlleguee().isBlank()) {
                sb.append(" — Position prêtée par l'adversaire : ").append(c.positionAlleguee().strip());
            }
            sb.append('\n');
        }
    }

    /**
     * F-JU-02 / SF-JU-02-01 — section automatique des arrêts mappés F-JU-01
     * par outil décisionnel utilisé sur le dossier. Section absente si vide.
     */
    private void appendToolJurisprudenceCitations(
            StringBuilder sb,
            List<fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool> byTool) {
        if (byTool == null || byTool.isEmpty()) {
            return;
        }
        sb.append("\n=== JURISPRUDENCE APPLICABLE PAR OUTIL ===\n");
        sb.append("(Arrêts curatés par LegalCase pour les outils décisionnels utilisés — citation indicative, l'avocat reste seul juge.)\n");
        for (fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool entry : byTool) {
            if (entry == null || entry.citations() == null || entry.citations().isEmpty()) {
                continue;
            }
            // SF-98-55 — libellé lisible, jamais le code brut (ex. « f-dt-08-… ») : défense
            // en profondeur contre la reprise du jargon interne dans l'acte produit.
            sb.append("\nSujet : ").append(humanizeToolId(entry.toolId()));
            sb.append('\n');
            for (var citation : entry.citations()) {
                sb.append("  - ").append(nullSafe(citation.arretRef()));
                if (citation.chapeauOfficiel() != null && !citation.chapeauOfficiel().isBlank()) {
                    sb.append(" — « ").append(citation.chapeauOfficiel().strip()).append(" »");
                }
                sb.append('\n');
            }
        }
    }

    /**
     * F-242 / SF-242-01 — ajoute la section des citations de jurisprudence d'appui,
     * regroupées par point juridique : pour chaque point, son texte (snapshot) puis ses
     * références numérotées avec leur portée. L'ordre des points suit l'ordre d'arrivée
     * des citations (déjà trié par index de point juridique côté repository).
     *
     * <p>Sans citation, la section indique explicitement l'absence de référence — pour
     * que la garde anti-hallucination du prompt système soit sans ambiguïté.</p>
     */
    private void appendJurisprudenceCitations(
            StringBuilder sb,
            List<ConclusionPromptInput.JurisprudenceCitationForPrompt> citations) {
        sb.append("\n=== JURISPRUDENCE À L'APPUI ===\n");
        if (citations == null || citations.isEmpty()) {
            sb.append("Aucune référence de jurisprudence fournie.\n");
            return;
        }
        // Regroupement par point juridique (index + snapshot du texte), ordre d'arrivée préservé.
        java.util.Map<String, List<ConclusionPromptInput.JurisprudenceCitationForPrompt>> byPoint =
                new java.util.LinkedHashMap<>();
        for (ConclusionPromptInput.JurisprudenceCitationForPrompt c : citations) {
            if (c == null) {
                continue;
            }
            String groupKey = c.pointJuridiqueIndex() + "|" + nullSafe(c.pointJuridiqueTexte());
            byPoint.computeIfAbsent(groupKey, k -> new java.util.ArrayList<>()).add(c);
        }
        for (List<ConclusionPromptInput.JurisprudenceCitationForPrompt> group : byPoint.values()) {
            ConclusionPromptInput.JurisprudenceCitationForPrompt first = group.get(0);
            sb.append("\nPoint juridique : ").append(nullSafe(first.pointJuridiqueTexte())).append('\n');
            for (ConclusionPromptInput.JurisprudenceCitationForPrompt c : group) {
                sb.append("  - ").append(nullSafe(c.reference()));
                if (c.portee() != null && !c.portee().isBlank()) {
                    sb.append(" — Portée : ").append(c.portee().strip());
                }
                sb.append('\n');
            }
        }
    }

    /**
     * Extrait les sections faits / points juridiques / risques du JSON de synthèse et
     * les ajoute au message. Fail-open : un JSON absent ou illisible n'interrompt pas
     * l'assemblage (la section synthèse est simplement marquée indisponible).
     */
    private void appendSynthesis(StringBuilder sb, String analysisResultJson) {
        sb.append("\n=== SYNTHÈSE DU DOSSIER ===\n");
        if (analysisResultJson == null || analysisResultJson.isBlank()) {
            sb.append("Synthèse indisponible.\n");
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(analysisResultJson);
            appendTextList(sb, "Faits", root.path("faits"));
            appendTextList(sb, "Points juridiques", root.path("points_juridiques"));
            appendTextList(sb, "Risques", root.path("risques"));
        } catch (Exception ex) {
            log.warn("Synthèse JSON illisible pour la génération de conclusions : {}", ex.getMessage());
            sb.append("Synthèse indisponible (format inattendu).\n");
        }
    }

    /**
     * Ajoute une liste de la synthèse. Chaque élément peut être une chaîne (format legacy)
     * ou un objet portant un champ {@code texte}.
     */
    private void appendTextList(StringBuilder sb, String title, JsonNode arrayNode) {
        sb.append('\n').append(title).append(" :\n");
        if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) {
            sb.append("  (aucun)\n");
            return;
        }
        for (JsonNode item : arrayNode) {
            String texte;
            if (item.isTextual()) {
                texte = item.asText();
            } else {
                texte = item.path("texte").asText("");
            }
            if (!texte.isBlank()) {
                sb.append("  - ").append(texte).append('\n');
            }
        }
    }

    /**
     * F-98 / SF-98-61 — extrait l'identité et l'adresse des parties du sous-objet
     * {@code travail_extracted_data} du JSON d'analyse (clés {@code nom_salarie},
     * {@code prenom_salarie}, {@code adresse_salarie}, {@code nom_employeur},
     * {@code adresse_employeur}) et les injecte dans une section dédiée, afin que le LLM
     * reprenne les vraies adresses au lieu d'un placeholder « [adresse] ».
     *
     * <p>Fail-open (comme {@link #appendSynthesis}) : un JSON absent / illisible n'ajoute
     * simplement aucune section. La section est <strong>absente</strong> si aucune des
     * cinq identités n'est présente (pas de rubrique vide). Chaque champ n'est rendu que
     * s'il est présent.</p>
     */
    private void appendPartiesIdentity(StringBuilder sb, String analysisResultJson) {
        if (analysisResultJson == null || analysisResultJson.isBlank()) {
            return;
        }
        try {
            JsonNode travail = objectMapper.readTree(analysisResultJson)
                    .path("travail_extracted_data");
            if (travail.isMissingNode() || !travail.isObject()) {
                return;
            }
            String prenomSalarie = textOrBlank(travail, "prenom_salarie");
            String nomSalarie = textOrBlank(travail, "nom_salarie");
            String adresseSalarie = textOrBlank(travail, "adresse_salarie");
            String nomEmployeur = textOrBlank(travail, "nom_employeur");
            String adresseEmployeur = textOrBlank(travail, "adresse_employeur");

            boolean hasAny = !prenomSalarie.isBlank() || !nomSalarie.isBlank()
                    || !adresseSalarie.isBlank() || !nomEmployeur.isBlank()
                    || !adresseEmployeur.isBlank();
            if (!hasAny) {
                return;
            }

            sb.append("\n=== IDENTITÉ DES PARTIES ===\n");
            if (!prenomSalarie.isBlank() || !nomSalarie.isBlank() || !adresseSalarie.isBlank()) {
                sb.append("Salarié :");
                String nomComplet = (prenomSalarie + " " + nomSalarie).strip();
                if (!nomComplet.isBlank()) {
                    sb.append(' ').append(nomComplet);
                }
                if (!adresseSalarie.isBlank()) {
                    sb.append(" — Adresse : ").append(adresseSalarie);
                }
                sb.append('\n');
            }
            if (!nomEmployeur.isBlank() || !adresseEmployeur.isBlank()) {
                sb.append("Employeur :");
                if (!nomEmployeur.isBlank()) {
                    sb.append(' ').append(nomEmployeur);
                }
                if (!adresseEmployeur.isBlank()) {
                    sb.append(" — Adresse : ").append(adresseEmployeur);
                }
                sb.append('\n');
            }
        } catch (Exception ex) {
            log.warn("Identité des parties illisible pour la génération de conclusions : {}",
                    ex.getMessage());
        }
    }

    /** Lit un champ texte du nœud, en chaîne vide si absent / null / non textuel. */
    private static String textOrBlank(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || !value.isTextual()) {
            return "";
        }
        return value.asText().strip();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    /**
     * SF-98-55 — transforme un {@code toolId} interne en libellé lisible pour le prompt,
     * afin de ne jamais exposer le code brut (ex. {@code f-dt-08-licenciement-validite}
     * → « Licenciement validité »). Retire un éventuel préfixe pays/domaine
     * {@code f-xx-NN-}, remplace les tirets par des espaces et met une capitale initiale.
     * Robuste au {@code null}/vide (→ chaîne vide).
     */
    static String humanizeToolId(String toolId) {
        if (toolId == null || toolId.isBlank()) {
            return "";
        }
        String cleaned = toolId.trim()
                .replaceFirst("(?i)^f-[a-z]+-\\d+-", "")
                .replace('-', ' ')
                .replace('_', ' ')
                .trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    /**
     * F-98 / SF-98-01 — agrégat des intrants du dossier pour la construction du prompt.
     *
     * @param caseTitle               intitulé du dossier
     * @param jurisdictionLabel       libellé humain de la juridiction
     * @param stageLabel              libellé humain du stade
     * @param positionLabel           libellé humain de la position
     * @param analysisResultJson      JSON brut de la synthèse {@code DONE} la plus récente
     * @param pieces                  pièces numérotées du dossier (ordre stable)
     * @param toolTiles               verdicts des outils décisionnels remplis
     * @param retainedStrategies      pistes stratégiques au statut {@code RETAINED}
     * @param jurisprudenceCitations  F-242 — citations de jurisprudence d'appui saisies
     *                                par l'avocat (ordre stable par point juridique)
     * @param toolJurisprudenceByTool F-JU-02 — arrêts curatés par outil décisionnel
     * @param adverseToRefute         F-98 / SF-98-56 — citations adverses marquées à
     *                                réfuter (ne porte ni nom de fichier ni statut technique)
     * @param adverseMoyens           F-261 / SF-261-02 — moyens (arguments) de la partie
     *                                adverse extraits de ses écritures, à réfuter moyen par
     *                                moyen ; complémentaire de {@code adverseToRefute}
     *                                (citations). Vide hors travail FR ou sans document adverse.
     */
    public record ConclusionPromptInput(
            String caseTitle,
            String jurisdictionLabel,
            String stageLabel,
            String positionLabel,
            String analysisResultJson,
            List<NumberedPiece> pieces,
            List<DashboardTile> toolTiles,
            List<RetainedStrategy> retainedStrategies,
            List<JurisprudenceCitationForPrompt> jurisprudenceCitations,
            List<fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool> toolJurisprudenceByTool,
            List<AdverseCitationToRefute> adverseToRefute,
            List<AdverseMoyen> adverseMoyens,
            String previousRecapContent) {

        /**
         * F-98 / SF-261-02 — constructeur back-compat (pré-F-271, sans base récapitulative).
         * La base récapitulative est absente ({@code null}) → génération from-scratch.
         */
        public ConclusionPromptInput(
                String caseTitle, String jurisdictionLabel, String stageLabel, String positionLabel,
                String analysisResultJson, List<NumberedPiece> pieces, List<DashboardTile> toolTiles,
                List<RetainedStrategy> retainedStrategies,
                List<JurisprudenceCitationForPrompt> jurisprudenceCitations,
                List<fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool> toolJurisprudenceByTool,
                List<AdverseCitationToRefute> adverseToRefute,
                List<AdverseMoyen> adverseMoyens) {
            this(caseTitle, jurisdictionLabel, stageLabel, positionLabel, analysisResultJson,
                    pieces, toolTiles, retainedStrategies, jurisprudenceCitations,
                    toolJurisprudenceByTool, adverseToRefute, adverseMoyens, null);
        }

        /** F-JU-02 / SF-JU-02-01 — constructeur back-compat (pré-F-JU-02). */
        public ConclusionPromptInput(
                String caseTitle, String jurisdictionLabel, String stageLabel, String positionLabel,
                String analysisResultJson, List<NumberedPiece> pieces, List<DashboardTile> toolTiles,
                List<RetainedStrategy> retainedStrategies,
                List<JurisprudenceCitationForPrompt> jurisprudenceCitations) {
            this(caseTitle, jurisdictionLabel, stageLabel, positionLabel, analysisResultJson,
                    pieces, toolTiles, retainedStrategies, jurisprudenceCitations,
                    java.util.List.of(), java.util.List.of(), java.util.List.of());
        }

        /** F-98 / SF-98-56 — constructeur back-compat (pré-SF-98-56, avec outils F-JU-02). */
        public ConclusionPromptInput(
                String caseTitle, String jurisdictionLabel, String stageLabel, String positionLabel,
                String analysisResultJson, List<NumberedPiece> pieces, List<DashboardTile> toolTiles,
                List<RetainedStrategy> retainedStrategies,
                List<JurisprudenceCitationForPrompt> jurisprudenceCitations,
                List<fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool> toolJurisprudenceByTool) {
            this(caseTitle, jurisdictionLabel, stageLabel, positionLabel, analysisResultJson,
                    pieces, toolTiles, retainedStrategies, jurisprudenceCitations,
                    toolJurisprudenceByTool, java.util.List.of(), java.util.List.of());
        }

        /** F-98 / SF-98-56 — constructeur back-compat (pré-SF-261-02, sans moyens adverses). */
        public ConclusionPromptInput(
                String caseTitle, String jurisdictionLabel, String stageLabel, String positionLabel,
                String analysisResultJson, List<NumberedPiece> pieces, List<DashboardTile> toolTiles,
                List<RetainedStrategy> retainedStrategies,
                List<JurisprudenceCitationForPrompt> jurisprudenceCitations,
                List<fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool> toolJurisprudenceByTool,
                List<AdverseCitationToRefute> adverseToRefute) {
            this(caseTitle, jurisdictionLabel, stageLabel, positionLabel, analysisResultJson,
                    pieces, toolTiles, retainedStrategies, jurisprudenceCitations,
                    toolJurisprudenceByTool, adverseToRefute, java.util.List.of());
        }

        /** Pièce numérotée du dossier. */
        public record NumberedPiece(int number, String label, String type) {
        }

        /** Piste stratégique retenue par l'avocat. */
        public record RetainedStrategy(String texte, String baseJuridique) {
        }

        /**
         * F-242 / SF-242-01 — citation de jurisprudence d'appui rattachée à un point
         * juridique de la synthèse.
         *
         * @param pointJuridiqueIndex index du point juridique dans la synthèse
         * @param pointJuridiqueTexte snapshot du texte du point juridique
         * @param reference           libellé de la référence
         * @param portee              ligne de portée ({@code null} si non renseignée)
         */
        public record JurisprudenceCitationForPrompt(
                int pointJuridiqueIndex,
                String pointJuridiqueTexte,
                String reference,
                String portee) {
        }

        /**
         * F-98 / SF-98-56 — citation de jurisprudence adverse marquée à réfuter.
         *
         * <p>N'expose <strong>ni</strong> nom de fichier <strong>ni</strong> statut
         * technique (anti-jargon SF-98-55) : seules la référence, l'explication (réalité
         * de l'arrêt) et la position que l'adversaire prête à l'arrêt sont transmises.</p>
         *
         * @param reference        libellé de la référence invoquée par l'adversaire
         * @param explication      explication du verdict (réalité de l'arrêt), ou {@code null}
         * @param positionAlleguee position que l'adversaire prête à l'arrêt, ou {@code null}
         */
        public record AdverseCitationToRefute(
                String reference,
                String explication,
                String positionAlleguee) {
        }
    }
}
