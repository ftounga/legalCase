/**
 * SF-277-01 — Sérialiseur markdown **custom** pour l'éditeur WYSIWYG des
 * conclusions, et utilitaires de round-trip.
 *
 * <p><b>Pourquoi un sérialiseur custom ?</b> Le markdown des conclusions est la
 * <b>source de vérité</b> consommée par 5 systèmes (export Word/PDF, sommaire
 * F-276, co-rédaction F-265, diff F-280, alerte placeholders SF-266-03). Un
 * aller-retour <code>markdown → ProseMirror → markdown</code> infidèle casse
 * silencieusement ces consommateurs. Le sérialiseur canonique
 * (<code>defaultMarkdownSerializer</code>) a deux défauts identifiés au spike
 * (cf. <code>SF-277-00-coherence.md</code> §spike) :</p>
 *
 * <ol>
 *   <li>🔴 il <b>échappe</b> les crochets des placeholders
 *       (<code>[Nom et qualité de l'avocat]</code> → <code>\[…\]</code>),
 *       ce qui corrompt la garde placeholders SF-266-03 et le bloc signature ;</li>
 *   <li>🟠 il sérialise les listes à puces avec <code>*</code> au lieu de
 *       <code>-</code>, produisant du bruit de diff F-280 à chaque sauvegarde.</li>
 * </ol>
 *
 * <p>Ce module dérive du sérialiseur par défaut avec deux corrections :</p>
 * <ul>
 *   <li><b>marqueur de liste forcé à <code>-</code></b> (override du nœud
 *       <code>bullet_list</code>) ;</li>
 *   <li><b>dé-échappement des crochets</b> en post-traitement
 *       (<code>\[</code> / <code>\]</code> → <code>[</code> / <code>]</code>),
 *       l'inverse exact de l'échappement appliqué à du texte littéral. Sûr car
 *       les liens markdown (<code>[label](url)</code>) ne sont jamais échappés
 *       par le sérialiseur, et les placeholders sont du texte littéral.</li>
 * </ul>
 *
 * <p>Le parser reste le <code>defaultMarkdownParser</code> : parser et
 * sérialiseur <b>partagent le même schéma</b> <code>prosemirror-markdown</code>,
 * ce qui garantit un round-trip bien défini.</p>
 *
 * <p>Module <b>pur</b> (aucune dépendance Angular), testable en isolation et
 * couvert par le <b>gate d'idempotence</b>
 * (<code>conclusion-markdown-serializer.spec.ts</code>).</p>
 */
import type { Node as ProseMirrorNode } from 'prosemirror-model';
import {
  MarkdownSerializer,
  defaultMarkdownParser,
  defaultMarkdownSerializer,
} from 'prosemirror-markdown';

/**
 * Sérialiseur markdown custom : identique au sérialiseur par défaut, sauf le
 * nœud <code>bullet_list</code> qui force le marqueur <code>-</code> (aligné sur
 * le markdown généré par le LLM, évite le bruit de diff F-280).
 */
const conclusionMarkdownSerializer = new MarkdownSerializer(
  {
    ...defaultMarkdownSerializer.nodes,
    // Override : marqueur de liste à puces = `-` (et non `*`).
    bullet_list(state, node) {
      state.renderList(node, '  ', () => (node.attrs['bullet'] || '-') + ' ');
    },
  },
  defaultMarkdownSerializer.marks,
);

/**
 * Dé-échappe les crochets <code>\[</code> / <code>\]</code> produits par le
 * sérialiseur sur du texte littéral. Inverse exact de l'échappement markdown
 * appliqué aux caractères structurants, restreint aux crochets — les
 * placeholders <code>[…]</code> et le bloc signature restent intacts, la garde
 * SF-266-03 (regex <code>[…]</code>) continue de les détecter.
 */
function unescapeBrackets(markdown: string): string {
  return markdown.replace(/\\([[\]])/g, '$1');
}

/**
 * Parse un markdown en document ProseMirror (schéma
 * <code>prosemirror-markdown</code>). Lève si le markdown est non parsable
 * (l'appelant retombe alors sur le textarea source — cf. fallback du composant).
 */
export function markdownToDoc(markdown: string): ProseMirrorNode {
  // `defaultMarkdownParser.parse` peut retourner `null` sur entrée pathologique :
  // on remonte une erreur explicite pour activer le fallback côté composant.
  const doc = defaultMarkdownParser.parse(markdown ?? '');
  if (!doc) {
    throw new Error('Markdown non parsable (document ProseMirror nul).');
  }
  return doc;
}

/**
 * Sérialise un document ProseMirror en markdown <b>propre</b> : marqueur de
 * liste <code>-</code>, crochets non échappés. C'est ce markdown qui réalimente
 * <code>draftContent</code> (inchangé en aval : save, diff, sommaire, export).
 */
export function docToMarkdown(doc: ProseMirrorNode): string {
  const serialized = conclusionMarkdownSerializer.serialize(doc);
  return unescapeBrackets(serialized);
}

/**
 * Round-trip <code>markdown → ProseMirror → markdown</code> via le pipeline
 * custom. C'est la transformation appliquée dès qu'un acte entre dans l'éditeur
 * WYSIWYG. Garantie (gate CI) : <b>idempotente</b>
 * (<code>roundTripMarkdown(roundTripMarkdown(md)) === roundTripMarkdown(md)</code>)
 * et fidèle (titres, gras, listes <code>-</code>, citations, placeholders,
 * renvois « Pièce n° X », montants préservés ; aucun <code>\[</code> ni
 * marqueur <code>*</code> en sortie).
 */
export function roundTripMarkdown(markdown: string): string {
  return docToMarkdown(markdownToDoc(markdown));
}
