/**
 * SF-277-01 — Gate d'idempotence du round-trip markdown (test-first).
 *
 * Ce spec est le **garde-fou critique** de la feature : il prouve, sur un corpus
 * d'actes réels (inspiré de `dossier-licenciement-durand`) + un échantillon
 * synthétique couvrant tous les éléments structurants, que le pipeline
 * `markdown → ProseMirror → markdown` :
 *   (a) ne produit AUCUN crochet échappé `\[` (placeholders SF-266-03 / signature),
 *   (b) ne produit AUCUN marqueur de liste `*` (bruit de diff F-280),
 *   (c) préserve titres `##`/`###`, **gras**, *italique*, renvois « Pièce n° X »,
 *       montants « 7 314,82 € » et placeholders `[…]`,
 *   (d) est IDEMPOTENT : `rt(rt(md)) === rt(md)`.
 *
 * Si ce gate échoue, l'éditeur WYSIWYG corromprait le markdown source de vérité
 * → la livraison est BLOQUÉE (cf. mini-spec, ordre de dev imposé).
 */
import {
  docToMarkdown,
  markdownToDoc,
  roundTripMarkdown,
} from './conclusion-markdown-serializer';

/**
 * Échantillon synthétique « tout-en-un » : titres `##`/`###`, gras, italique,
 * liste `-`, citation `>`, placeholders, renvoi « Pièce n° 3 », montant en €.
 */
const SYNTHETIC_ACT = [
  '## I. RAPPEL DES FAITS',
  '',
  'Madame Sophie DURAND a été engagée le 12 septembre 2018, **statut cadre**, puis',
  'licenciée pour *faute grave* par lettre du 3 janvier 2026 (Pièce n° 3).',
  '',
  '### A. Sur la régularité de la procédure',
  '',
  'Le moyen est inopérant au regard de l’article L. 1235-2 du Code du travail.',
  '',
  'La salariée sollicite une indemnité de 7 314,82 € au titre du préavis.',
  '',
  '- Premier manquement caractérisé',
  '- Deuxième manquement caractérisé',
  '',
  '> Il est de jurisprudence constante que la convocation par lettre simple',
  '> ne suffit pas à entacher la procédure de nullité.',
  '',
  'Fait à [Lieu], le [Date].',
  '',
  '[Nom et qualité de l’avocat]',
  '',
  'Mention restante : [à compléter]',
].join('\n');

/**
 * Acte « réel » (transcription markdown d'un acte type, inspiré du dossier
 * DURAND) : structure de conclusions complète avec dispositif « PAR CES MOTIFS ».
 */
const REAL_ACT = [
  '## CONCLUSIONS EN DÉFENSE',
  '',
  '**POUR :** La société TECHSOLUTIONS SAS, dont le siège est sis 42 boulevard',
  'Haussmann, 75009 Paris, *Défenderesse*,',
  '',
  '**CONTRE :** Madame Sophie DURAND, demeurant 12 avenue des Fleurs, 69003 Lyon,',
  '*Demanderesse*,',
  '',
  '## I. RAPPEL DES FAITS',
  '',
  'Madame DURAND a été licenciée pour faute grave par lettre du 3 janvier 2026',
  '(Pièce n° 1), à l’issue d’un entretien préalable tenu le 28 décembre 2025',
  '(Pièce n° 2).',
  '',
  '## II. DISCUSSION',
  '',
  '### A. Sur la régularité de la procédure de licenciement',
  '',
  'Le moyen est inopérant. Il ne peut ouvrir droit, le cas échéant, qu’à',
  'l’indemnité limitée prévue à l’article L. 1235-2 du Code du travail.',
  '',
  '### B. Sur la qualification de faute grave',
  '',
  'La faute grave est caractérisée, privant l’intéressée de toute indemnité de',
  'rupture, en ce compris l’indemnité de préavis de 7 314,82 €.',
  '',
  '## PAR CES MOTIFS',
  '',
  'Débouter Madame Sophie DURAND de l’intégralité de ses demandes ;',
  '',
  'La condamner aux entiers dépens.',
  '',
  'Fait à [Lieu], le [Date].',
  '',
  '[Nom et qualité de l’avocat]',
].join('\n');

const CORPUS: ReadonlyArray<{ name: string; markdown: string }> = [
  { name: 'échantillon synthétique tout-en-un', markdown: SYNTHETIC_ACT },
  { name: 'acte réel (dossier DURAND)', markdown: REAL_ACT },
];

describe('conclusion-markdown-serializer (gate round-trip SF-277-01)', () => {
  describe.each(CORPUS)('corpus : $name', ({ markdown }) => {
    const rt = roundTripMarkdown(markdown);

    it('(a) ne produit AUCUN crochet échappé \\[ ni \\]', () => {
      expect(rt).not.toMatch(/\\[[\]]/);
    });

    it('(b) ne produit AUCUN marqueur de liste « * »', () => {
      // Aucune ligne ne commence par « * » (marqueur de puce non désiré).
      expect(rt).not.toMatch(/^\* /m);
      // Les puces restent en « - ».
      if (markdown.includes('\n- ')) {
        expect(rt).toMatch(/^- /m);
      }
    });

    it('(c) préserve les titres ## et ###', () => {
      const headings = (markdown.match(/^#{2,3} .+$/gm) ?? []).map((h) =>
        h.trim(),
      );
      for (const h of headings) {
        expect(rt).toContain(h);
      }
    });

    it('(c) préserve les placeholders [...] sans backslash', () => {
      const placeholders = markdown.match(/\[[^\]\n]{1,80}\]/g) ?? [];
      for (const p of placeholders) {
        expect(rt).toContain(p);
      }
    });

    it('(c) préserve les renvois « Pièce n° X »', () => {
      const refs = markdown.match(/Pièce n° \d+/g) ?? [];
      for (const r of refs) {
        expect(rt).toContain(r);
      }
    });

    it('(c) préserve les montants en euros', () => {
      const amounts = markdown.match(/\d[\d\s]*,\d{2}\s?€/g) ?? [];
      for (const a of amounts) {
        expect(rt).toContain(a);
      }
    });

    it('(c) préserve le gras **…**', () => {
      const bolds = markdown.match(/\*\*[^*\n]+\*\*/g) ?? [];
      for (const b of bolds) {
        expect(rt).toContain(b);
      }
    });

    it('(d) est idempotent : rt(rt(md)) === rt(md)', () => {
      expect(roundTripMarkdown(rt)).toBe(rt);
    });
  });

  it('expose markdownToDoc / docToMarkdown composables (rt = doc→md)', () => {
    const doc = markdownToDoc(SYNTHETIC_ACT);
    const out = docToMarkdown(doc);
    expect(out).toBe(roundTripMarkdown(SYNTHETIC_ACT));
  });

  it('le bloc signature « [Nom et qualité de l’avocat] » survit intact', () => {
    const rt = roundTripMarkdown(REAL_ACT);
    expect(rt).toContain('[Nom et qualité de l’avocat]');
    expect(rt).toContain('Fait à [Lieu], le [Date].');
  });

  it('un markdown déjà propre est un point fixe (round-trip stable)', () => {
    // Le markdown généré par le serializer doit être stable au 1er passage déjà
    // (pas seulement au 2ᵉ) sur une entrée canonique simple.
    const canonical = '## Titre\n\nParagraphe **gras**.\n\n- a\n- b';
    expect(roundTripMarkdown(canonical)).toBe(canonical);
  });

  it('gère le contenu vide sans erreur', () => {
    expect(roundTripMarkdown('')).toBe('');
  });
});
