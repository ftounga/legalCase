// F-266 / F-281 — Helpers purs de construction du **contenu exporté** des
// conclusions (markdown augmenté pour l'export Word/PDF). Module sans Angular,
// testable en isolation et partagé entre le composant `conclusions-section`,
// la modale d'aperçu d'export (F-281) et leurs specs.
//
// Invariant fondamental : ces fonctions ne modifient JAMAIS le `content`
// markdown stocké du dossier — elles construisent une chaîne dédiée à
// l'export (en-tête de cabinet F-266, cartouche métadonnées F-281, corps,
// bloc signature F-281). Round-trip d'édition garanti.

/**
 * Échappe les caractères markdown structurants d'une chaîne destinée à être
 * insérée comme **texte** dans du markdown (et non interprétée). Empêche qu'un
 * `#` / `*` / `_` / `` ` `` issu d'un libellé (en-tête de cabinet, label de
 * juridiction, lieu de signature…) ne soit rendu comme un titre, du gras, etc.
 */
export function escapeMarkdown(value: string): string {
  return value.replace(/([\\`*_{}\[\]()#+\-.!>])/g, '\\$1');
}

/**
 * F-266 / SF-266-02 — Construit le contenu **exporté** à partir d'un en-tête de
 * cabinet (libellé libre, optionnel) et d'un corps markdown (qui peut déjà
 * inclure cartouche métadonnées + signature, F-281).
 *
 * <p>Quand l'en-tête est non vide, on le préfixe sous forme de **markdown
 * valide** (1ʳᵉ ligne = titre `#`, lignes suivantes = paragraphes, puis un
 * filet `---`) → il est tokenisé par le même pipeline d'export que le corps
 * (aucun marqueur brut visible). L'en-tête est traité comme du **texte** : ses
 * caractères markdown sont échappés. Quand l'en-tête est vide, on renvoie le
 * `body` **inchangé** (export neutre, comportement historique).</p>
 */
export function buildExportContent(header: string, body: string): string {
  const trimmed = (header ?? '').trim();
  if (trimmed.length === 0) {
    return body;
  }
  const lines = trimmed
    .split('\n')
    .map((l) => l.trim())
    .filter((l) => l.length > 0);
  const parts: string[] = [];
  // 1ʳᵉ ligne = titre de l'en-tête ; suivantes = paragraphes.
  parts.push(`# ${escapeMarkdown(lines[0])}`);
  for (let i = 1; i < lines.length; i++) {
    parts.push(escapeMarkdown(lines[i]));
  }
  parts.push('---');
  return parts.join('\n\n') + '\n\n' + body;
}

/**
 * F-281 — Métadonnées procédurales (juridiction / stade / position) à
 * réintégrer en **tête du fichier exporté**. Alimentées par le stade procédural
 * du dossier (F-243 — `ProcedureStageService`).
 */
export interface ProcedureMetadata {
  jurisdictionLabel?: string | null;
  stageLabel?: string | null;
  positionLabel?: string | null;
}

/**
 * F-281 — Construit le **cartouche de métadonnées** markdown placé en tête de
 * l'export : une ligne en gras par valeur renseignée (« **Juridiction :** … »,
 * « **Stade :** … », « **Position :** … ») suivie d'un filet `---`.
 *
 * <p>Si AUCUNE des trois valeurs n'est renseignée (F-243 non rempli), renvoie
 * `''` → aucun cartouche, export neutre conservé (dégradation propre, A3). Les
 * libellés sont échappés (texte, jamais interprété).</p>
 */
export function buildMetadataHeader(meta: ProcedureMetadata | null | undefined): string {
  if (!meta) {
    return '';
  }
  const rows: string[] = [];
  const add = (label: string, value: string | null | undefined): void => {
    const v = (value ?? '').trim();
    if (v.length > 0) {
      rows.push(`**${label} :** ${escapeMarkdown(v)}`);
    }
  };
  add('Juridiction', meta.jurisdictionLabel);
  add('Stade', meta.stageLabel);
  add('Position', meta.positionLabel);
  if (rows.length === 0) {
    return '';
  }
  // Chaque ligne devient un paragraphe markdown ; filet de séparation après.
  return rows.join('\n\n') + '\n\n---';
}

/**
 * F-281 — Construit le **bloc signature** markdown placé en **pied** de
 * l'export : un filet `---` puis « Fait à {lieu}, le {date} » et la ligne
 * `[Nom et qualité de l'avocat]` (placeholder volontairement laissé — l'app ne
 * connaît pas l'identité du signataire ; la garde SF-266-03 l'alerte).
 *
 * <p>Si `enabled` est faux, renvoie `''` (opt-in, A4). Un `lieu` / `date` vide
 * retombe sur le placeholder `[Lieu]` / `[Date]` (détecté par la garde
 * SF-266-03). `lieu` et `date` sont du **texte échappé**.</p>
 */
export function buildSignatureBlock(
  enabled: boolean,
  lieu: string | null | undefined,
  date: string | null | undefined,
): string {
  if (!enabled) {
    return '';
  }
  const lieuPart = (lieu ?? '').trim().length > 0
    ? escapeMarkdown((lieu ?? '').trim())
    : '[Lieu]';
  const datePart = (date ?? '').trim().length > 0
    ? escapeMarkdown((date ?? '').trim())
    : '[Date]';
  // Filet de séparation, ligne « Fait à … », saut, puis nom/qualité.
  return `---\n\nFait à ${lieuPart}, le ${datePart}\n\n[Nom et qualité de l'avocat]`;
}

/**
 * F-281 — Compose le **contenu complet d'export** : cartouche métadonnées (en
 * tête) + corps des conclusions (inchangé) + bloc signature (en pied), le tout
 * éventuellement préfixé de l'en-tête de cabinet (F-266).
 *
 * <p>Ordre du document final :
 * `[en-tête cabinet] → [métadonnées] → [corps] → [signature]`. Chaque fragment
 * vide est omis proprement (jamais de filet orphelin). Quand en-tête, méta et
 * signature sont tous vides, renvoie le `content` **strictement inchangé**
 * (comportement neutre historique — A1 / non-régression).</p>
 *
 * <p>Le `content` d'origine n'est jamais muté : cette fonction ne sert qu'à
 * alimenter l'argument des services d'export.</p>
 */
export function buildFullExportContent(
  cabinetHeader: string,
  metadataHeader: string,
  content: string,
  signatureBlock: string,
): string {
  const segments: string[] = [];
  const meta = (metadataHeader ?? '').trim();
  if (meta.length > 0) {
    segments.push(meta);
  }
  segments.push(content ?? '');
  const sig = (signatureBlock ?? '').trim();
  if (sig.length > 0) {
    segments.push(sig);
  }
  const body = segments.join('\n\n');
  // `buildExportContent` applique l'en-tête cabinet (ou renvoie `body` tel quel
  // si l'en-tête est vide → contenu neutre quand tout est vide).
  return buildExportContent(cabinetHeader, body);
}

/**
 * SF-266-03 — Détecte les emplacements « à compléter » laissés dans l'acte :
 * placeholders entre crochets posés par le générateur (« [Nom et qualité de
 * l'avocat] », « [à compléter] », « [Date] », « [Lieu] »…). Avant tout export
 * « déposable » (PDF/Word), on alerte l'avocat sur ces éléments non renseignés.
 *
 * <p>Heuristique générique : tout token « [ … ] » sur une seule ligne (≤ 80
 * caractères), contenant au moins une lettre, et **non immédiatement suivi de
 * `(`** (ce qui écarte les liens markdown « [libellé](url) »). Dédupliqué en
 * conservant l'ordre d'apparition. Pure fonction, sans dépendance.</p>
 *
 * @param content markdown de l'acte (le `content` stocké, pas l'export)
 * @returns liste ordonnée et dédupliquée des placeholders restants (crochets inclus)
 */
export function extractPlaceholders(content: string): string[] {
  if (!content) {
    return [];
  }
  const seen = new Set<string>();
  const result: string[] = [];
  const regex = /\[[^\]\n]{1,80}\](?!\()/g;
  let match: RegExpExecArray | null;
  while ((match = regex.exec(content)) !== null) {
    const token = match[0];
    // Au moins une lettre (écarte les renvois numériques « [1] »).
    if (!/\p{L}/u.test(token)) {
      continue;
    }
    if (!seen.has(token)) {
      seen.add(token);
      result.push(token);
    }
  }
  return result;
}
