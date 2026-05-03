package fr.ailegalcase.blog.service;

import fr.ailegalcase.blog.entity.BlogTopic;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Construit les prompts Sonnet (rédaction d'article) et Haiku (vérification
 * citations juridiques). Centralisé ici pour testabilité et stabilité des
 * formulations entre runs.
 */
@Component
public class BlogPromptBuilder {

    public PromptPair buildSonnetPrompt(BlogTopic topic) {
        String country = topic.getCountryScope();
        String category = topic.getCategory();
        String domainContext = DomainContextMapper.contextFor(category);
        String description = topic.getDescription() != null ? topic.getDescription() : "";

        String system = """
                Tu es un rédacteur juridique francophone expert pour un blog B2B destiné aux avocats.
                Tu maîtrises le %s et les spécificités du droit %s.

                Audience : avocats francophones %s.
                Ton : professionnel, précis, pédagogique.
                Pas de "Je suis une IA", pas de mentions de Claude / Anthropic.

                Citations juridiques précises (article + texte de loi).
                Si une citation n'est pas certaine, indique-le dans legalCitations[].context = "À vérifier".

                Réponds UNIQUEMENT avec un objet JSON valide, sans markdown ni texte autour.
                """.formatted(domainContext, countryLabel(country), countryLabel(country));

        String user = """
                Génère un article de blog SEO à partir du topic suivant :
                - Titre : %s
                - Slug : %s
                - Description : %s
                - Catégorie : %s
                - Pays : %s

                Format de réponse JSON strict :
                {
                  "metaTitle": "≤ 60 caractères, accroche SEO",
                  "metaDescription": "140-160 caractères, accroche meta",
                  "subtitle": "≤ 200 caractères, sous-titre engageant",
                  "introduction": "markdown 150-250 mots, accroche + plan",
                  "sections": [
                    {"heading": "string H2 sans le ##", "content": "markdown 300-500 mots"}
                  ],
                  "conclusion": "markdown 100-200 mots, synthèse + ouverture",
                  "ctaBlock": "markdown 50-100 mots avec lien {{LEGALCASE_CTA}}",
                  "legalCitations": [
                    {"reference": "Code du travail, art. L1234-X", "context": "courte phrase d'usage dans l'article"}
                  ]
                }

                Contraintes :
                - 4 à 6 sections H2 (jamais moins de 3, jamais plus de 8).
                - Chaque section : 300-500 mots, ton accessible mais juridiquement rigoureux.
                - Mentionne des références légales précises et vérifiables.
                - Pas de #/## dans heading (uniquement le texte).
                - ctaBlock contient obligatoirement la chaîne littérale {{LEGALCASE_CTA}}.

                Réponds UNIQUEMENT avec le JSON.
                """.formatted(
                        topic.getTitle(),
                        topic.getSlug(),
                        description,
                        category,
                        countryLabel(country));

        return new PromptPair(system, user);
    }

    public PromptPair buildHaikuPrompt(List<LegalCitationToVerify> citations) {
        String system = """
                Tu es un assistant de vérification de citations juridiques pour un blog B2B avocats francophone.
                Pour chaque citation fournie, tu dois la classifier en une des 3 catégories :
                - KNOWN : citation exacte et vérifiable (article de loi, jurisprudence connue, texte officiel)
                - UNVERIFIABLE : citation plausible mais que tu ne peux pas confirmer avec certitude
                - LIKELY_HALLUCINATION : citation incorrecte, inventée ou très douteuse

                Réponds UNIQUEMENT avec un objet JSON :
                {"verdicts": [{"reference": "...", "verdict": "KNOWN|UNVERIFIABLE|LIKELY_HALLUCINATION", "rationale": "courte explication"}]}

                Pas de markdown, pas de texte autour du JSON.
                """;

        String citationsJson = citations.stream()
                .map(c -> "  - reference=\"" + c.reference() + "\" | context=\"" + c.context() + "\"")
                .collect(Collectors.joining("\n"));

        String user = """
                Citations à vérifier :
                %s

                Renvoie le JSON avec un verdict par citation.
                """.formatted(citationsJson.isEmpty() ? "(aucune)" : citationsJson);

        return new PromptPair(system, user);
    }

    private static String countryLabel(String countryScope) {
        return switch (countryScope) {
            case "FR" -> "France";
            case "BE" -> "Belgique";
            default -> countryScope;
        };
    }

    public record PromptPair(String system, String user) {}

    public record LegalCitationToVerify(String reference, String context) {}
}
