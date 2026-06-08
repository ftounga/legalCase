import { marked, Token, Tokens } from 'marked';

/**
 * F-259 / SF-259-03 — Util partagé de tokenisation Markdown pour l'export
 * Word (`docx`) et PDF (`pdfmake`) des conclusions.
 *
 * Le contenu des conclusions est généré en **Markdown** (`#`/`##`/`###`,
 * `**gras**`, `*italique*`, listes `-`/`1.`, `>` citations, `---` filets,
 * code). SF-259-02 l'a rendu proprement à l'écran ; cette util factorise la
 * partie commune aux deux exports : on tokenise le `content` via
 * `marked.lexer` (tokens structurés, plus fiable que parser du HTML) et on
 * fournit un helper qui aplatit les tokens inline (`strong`/`em`/`text`/…)
 * en *runs* `{ text, bold, italics, code }` que Word et PDF mappent ensuite
 * chacun vers leur format natif.
 *
 * Aucune dépendance à `docx` ni `pdfmake` ici : pur passage Markdown → modèle
 * intermédiaire neutre.
 */

/** Un fragment de texte inline avec ses attributs de style résolus. */
export interface InlineRun {
  text: string;
  bold: boolean;
  italics: boolean;
  /** Fragment issu d'un `codespan` (police mono à l'arrivée). */
  code: boolean;
}

/**
 * Tokenise un contenu Markdown en tokens de bloc `marked`.
 *
 * `marked.lexer` est synchrone et ne lève pas sur du texte brut (le texte
 * sans marqueur devient un ou plusieurs tokens `paragraph`). Un `content`
 * vide/null renvoie un tableau vide.
 */
export function lexMarkdown(content: string | null | undefined): Token[] {
  if (content == null || content.trim().length === 0) {
    return [];
  }
  return marked.lexer(content);
}

/**
 * Aplatit une liste de tokens inline (`strong`, `em`, `del`, `codespan`,
 * `text`, `link`, `br`, `escape`…) en *runs* `{ text, bold, italics, code }`,
 * en propageant récursivement les styles (gras + italique imbriqués se
 * combinent en `bold:true, italics:true`).
 *
 * Les marqueurs Markdown (`#`, `**`, `*`, `-`, `>`…) ne figurent JAMAIS dans
 * le `text` des runs : `marked` les a déjà consommés pour produire les
 * tokens. Les liens sont rendus comme leur texte (l'URL n'est pas conservée :
 * un document de conclusions est destiné à l'impression / l'en-tête cabinet).
 */
export function flattenInline(
  tokens: Token[] | undefined,
  inherited: { bold?: boolean; italics?: boolean; code?: boolean } = {},
): InlineRun[] {
  if (!tokens || tokens.length === 0) {
    return [];
  }

  const runs: InlineRun[] = [];
  const bold = inherited.bold ?? false;
  const italics = inherited.italics ?? false;
  const code = inherited.code ?? false;

  for (const token of tokens) {
    switch (token.type) {
      case 'strong':
        runs.push(...flattenInline((token as Tokens.Strong).tokens, { bold: true, italics, code }));
        break;
      case 'em':
        runs.push(...flattenInline((token as Tokens.Em).tokens, { bold, italics: true, code }));
        break;
      case 'del':
        runs.push(...flattenInline((token as Tokens.Del).tokens, { bold, italics, code }));
        break;
      case 'link':
        runs.push(...flattenInline((token as Tokens.Link).tokens, { bold, italics, code }));
        break;
      case 'codespan':
        runs.push({ text: (token as Tokens.Codespan).text, bold, italics, code: true });
        break;
      case 'br':
        runs.push({ text: '\n', bold, italics, code });
        break;
      case 'text': {
        const t = token as Tokens.Text;
        // Un token `text` peut lui-même contenir des sous-tokens inline
        // (ex. emphase à l'intérieur). On descend si présents.
        if (t.tokens && t.tokens.length > 0) {
          runs.push(...flattenInline(t.tokens, { bold, italics, code }));
        } else {
          runs.push({ text: decodeEntities(t.text), bold, italics, code });
        }
        break;
      }
      case 'escape':
        runs.push({ text: (token as Tokens.Escape).text, bold, italics, code });
        break;
      default: {
        // Token inline non géré explicitement : on retombe sur son `text`
        // s'il existe (jamais de marqueur résiduel — `marked` les a retirés).
        const anyToken = token as { text?: string };
        if (typeof anyToken.text === 'string' && anyToken.text.length > 0) {
          runs.push({ text: decodeEntities(anyToken.text), bold, italics, code });
        }
        break;
      }
    }
  }

  return mergeAdjacentRuns(runs);
}

/**
 * Aplatit le texte d'un token de bloc disposant de `tokens` inline (heading,
 * paragraph, list_item, text de blockquote). Pratique pour les cas où l'on
 * veut directement les runs d'un bloc.
 */
export function inlineRunsOf(
  token: { tokens?: Token[]; text?: string },
): InlineRun[] {
  if (token.tokens && token.tokens.length > 0) {
    return flattenInline(token.tokens);
  }
  if (typeof token.text === 'string' && token.text.length > 0) {
    return [{ text: decodeEntities(token.text), bold: false, italics: false, code: false }];
  }
  return [];
}

/** Décode les entités HTML que `marked` peut produire dans le texte brut. */
function decodeEntities(text: string): string {
  return text
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, '\'')
    .replace(/&#x27;/g, '\'')
    .replace(/&#x2F;/g, '/');
}

/**
 * Fusionne les runs adjacents partageant exactement les mêmes attributs de
 * style — évite d'émettre une multitude de `TextRun`/fragments contigus
 * identiques (ex. ponctuation séparée du mot).
 */
function mergeAdjacentRuns(runs: InlineRun[]): InlineRun[] {
  const merged: InlineRun[] = [];
  for (const run of runs) {
    const last = merged[merged.length - 1];
    if (
      last &&
      last.bold === run.bold &&
      last.italics === run.italics &&
      last.code === run.code
    ) {
      last.text += run.text;
    } else {
      merged.push({ ...run });
    }
  }
  return merged;
}
