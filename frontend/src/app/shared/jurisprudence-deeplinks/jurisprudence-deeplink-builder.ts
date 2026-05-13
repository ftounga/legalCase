/**
 * F-241 SF-241-01 — Constructeur de deeplinks vers les moteurs de recherche
 * jurispru publics (Doctrine / Lexis Plus / Lextenso).
 *
 * Pures fonctions stateless, testables isolément. Pas d'appel API, pas de
 * tracking — uniquement la génération d'URLs intelligentes depuis le texte
 * d'un point juridique extrait par le pipeline IA.
 *
 * L'utilisateur conserve son propre abonnement chez l'éditeur cible : ces
 * URLs ouvrent simplement la barre de recherche pré-remplie de l'éditeur.
 */

const MIN_KEYWORD_LENGTH = 4;
const MAX_KEYWORDS = 8;

/**
 * Liste de stop-words français standards. Volontairement courte (~80 mots)
 * pour couvrir les déterminants, prépositions, conjonctions et verbes
 * grammaticaux les plus fréquents sans sur-filtrer.
 */
const STOPWORDS_FR = new Set([
  'le', 'la', 'les', 'un', 'une', 'des', 'du', 'de', 'au', 'aux',
  'et', 'ou', 'mais', 'donc', 'or', 'ni', 'car',
  'à', 'en', 'dans', 'sur', 'sous', 'par', 'pour', 'avec', 'sans', 'vers', 'chez', 'entre',
  'qui', 'que', 'quoi', 'dont', 'où', 'quand', 'comment', 'pourquoi',
  'ce', 'cette', 'ces', 'cet', 'son', 'sa', 'ses', 'leur', 'leurs', 'mon', 'ma', 'mes', 'notre', 'votre',
  'est', 'sont', 'était', 'sera', 'été', 'être', 'avoir', 'eu', 'fait', 'fais', 'font',
  'pas', 'plus', 'moins', 'très', 'aussi', 'encore', 'déjà', 'toujours', 'jamais', 'tout', 'tous', 'toute', 'toutes',
  'cas', 'lors', 'ainsi', 'alors', 'puis', 'enfin',
  'lui', 'elle', 'eux', 'elles', 'nous', 'vous',
  'aux', 'leurs', 'lequel', 'laquelle', 'lesquels', 'lesquelles',
  'tel', 'telle', 'tels', 'telles',
]);

/**
 * Extrait les mots-clés significatifs d'un texte de point juridique.
 *
 * Algorithme :
 *  1. Découpe en tokens sur les séparateurs non-alphabétiques (préserve les accents)
 *  2. Lowercase + trim
 *  3. Filtre les tokens trop courts (< MIN_KEYWORD_LENGTH)
 *  4. Filtre les stop-words FR
 *  5. Dédoublonne en conservant l'ordre d'apparition
 *  6. Tronque à MAX_KEYWORDS
 */
export function extractKeywords(text: string | null | undefined): string[] {
  if (!text || text.trim().length === 0) return [];

  // Tokenisation : sépare sur tout ce qui n'est pas une lettre (avec accents Unicode)
  const tokens = text
    .toLowerCase()
    .split(/[^a-zàâäéèêëïîôöùûüÿç-]+/i)
    .filter(t => t.length >= MIN_KEYWORD_LENGTH)
    .filter(t => !STOPWORDS_FR.has(t));

  // Dédoublonne en conservant l'ordre d'apparition
  const seen = new Set<string>();
  const result: string[] = [];
  for (const token of tokens) {
    if (!seen.has(token)) {
      seen.add(token);
      result.push(token);
      if (result.length >= MAX_KEYWORDS) break;
    }
  }
  return result;
}

/**
 * Construit l'URL de recherche Doctrine FR ou BE selon le pays du workspace.
 * Pattern documenté dans la mini-spec — à valider en check manuel avant merge.
 */
export function buildDoctrineUrl(keywords: string[], country: 'FR' | 'BE' = 'FR'): string {
  const host = country === 'BE' ? 'www.doctrine.be' : 'www.doctrine.fr';
  const query = keywords.join(' ');
  return `https://${host}/search?q=${encodeURIComponent(query)}&type=jurisprudence`;
}

/**
 * Construit l'URL de recherche Lexis 360 Intelligence (Lexis+).
 * Pattern documenté dans la mini-spec — à valider en check manuel avant merge.
 */
export function buildLexisPlusUrl(keywords: string[]): string {
  const query = keywords.join(' ');
  return `https://www.lexis360intelligence.fr/recherche?q=${encodeURIComponent(query)}`;
}

/**
 * Construit l'URL de recherche Lextenso.
 * Pattern documenté dans la mini-spec — à valider en check manuel avant merge.
 */
export function buildLextensoUrl(keywords: string[]): string {
  const query = keywords.join(' ');
  return `https://www.lextenso.fr/recherche?searchText=${encodeURIComponent(query)}`;
}
