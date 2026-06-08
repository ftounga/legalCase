import { flattenInline, inlineRunsOf, lexMarkdown } from './markdown-tokens';
import { Tokens } from 'marked';

/**
 * F-259 / SF-259-03 — util partagé de tokenisation Markdown (Word + PDF).
 * On verrouille ici la logique inline (gras/italique/imbriqué) commune aux
 * deux exports, indépendamment de `docx`/`pdfmake`.
 */
describe('markdown-tokens', () => {
  // MT-01 : lexMarkdown — content vide/null → tableau vide
  it('MT-01: lexMarkdown returns [] for empty/blank/null content', () => {
    expect(lexMarkdown('')).toEqual([]);
    expect(lexMarkdown('   ')).toEqual([]);
    expect(lexMarkdown(null)).toEqual([]);
    expect(lexMarkdown(undefined)).toEqual([]);
  });

  // MT-02 : lexMarkdown — heading depth + paragraphe
  it('MT-02: lexMarkdown produces heading + paragraph tokens', () => {
    const tokens = lexMarkdown('# Titre\n\nUn paragraphe.');
    const heading = tokens.find((t) => t.type === 'heading') as Tokens.Heading;
    expect(heading.depth).toBe(1);
    expect(tokens.some((t) => t.type === 'paragraph')).toBe(true);
  });

  // MT-03 : flattenInline — strong → bold, em → italics
  it('MT-03: strong/em flatten to bold/italics runs', () => {
    const [para] = lexMarkdown('Du **gras** et de l\'*italique*.');
    const runs = inlineRunsOf(para as Tokens.Paragraph);
    expect(runs.find((r) => r.text.includes('gras'))?.bold).toBe(true);
    expect(runs.find((r) => r.text.includes('italique'))?.italics).toBe(true);
    // Aucun marqueur résiduel.
    expect(runs.map((r) => r.text).join('')).not.toMatch(/[*]/);
  });

  // MT-04 : imbrication gras+italique → run combiné bold+italics
  it('MT-04: nested bold+italic combine on one run', () => {
    const [para] = lexMarkdown('***les deux*** ici.');
    const runs = inlineRunsOf(para as Tokens.Paragraph);
    const both = runs.find((r) => r.text.includes('les deux'));
    expect(both?.bold).toBe(true);
    expect(both?.italics).toBe(true);
  });

  // MT-05 : codespan → run code:true
  it('MT-05: codespan flattens to a code run', () => {
    const [para] = lexMarkdown('Voir `art. L1234-1` du code.');
    const runs = inlineRunsOf(para as Tokens.Paragraph);
    const code = runs.find((r) => r.text.includes('L1234-1'));
    expect(code?.code).toBe(true);
  });

  // MT-06 : runs adjacents de même style fusionnés
  it('MT-06: adjacent runs with same style are merged', () => {
    const runs = flattenInline([
      { type: 'text', raw: 'a', text: 'a' } as Tokens.Text,
      { type: 'text', raw: 'b', text: 'b' } as Tokens.Text,
    ]);
    expect(runs.length).toBe(1);
    expect(runs[0].text).toBe('ab');
  });

  // MT-07 : texte brut sans Markdown → un run simple, pas de marqueur
  it('MT-07: plain text yields one neutral run', () => {
    const [para] = lexMarkdown('Texte simple.');
    const runs = inlineRunsOf(para as Tokens.Paragraph);
    expect(runs).toEqual([{ text: 'Texte simple.', bold: false, italics: false, code: false }]);
  });
});
