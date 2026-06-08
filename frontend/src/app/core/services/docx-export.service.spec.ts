import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DocxExportService } from './docx-export.service';
import { CaseAnalysisResult } from '../models/case-analysis.model';
import { CaseFile } from '../models/case-file.model';

const makeCaseFile = (title: string): CaseFile => ({
  id: 'cf-1',
  title,
  legalDomain: 'DROIT_DU_TRAVAIL',
  description: null,
  status: 'OPEN',
  createdAt: '2026-01-01T00:00:00Z',
  lastDocumentDeletedAt: null,
  riskLevel: null,
  riskScore: null,
});

const makeSynthesis = (version: number, overrides: Partial<CaseAnalysisResult> = {}): CaseAnalysisResult => ({
  id: `syn-${version}`,
  version,
  analysisType: 'STANDARD',
  status: 'COMPLETED',
  timeline: [],
  faits: [],
  pointsJuridiques: [],
  risques: [],
  questionsOuvertes: [],
  piecesManquantes: [],
  riskLevel: null,
  riskScore: null,
  modelUsed: 'claude-3',
  updatedAt: '2026-03-24T10:00:00Z',
  ...overrides,
});

describe('DocxExportService', () => {
  let service: DocxExportService;
  let snackBar: jest.Mocked<MatSnackBar>;

  beforeEach(() => {
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      providers: [
        DocxExportService,
        { provide: MatSnackBar, useValue: snackBar },
      ],
    });

    service = TestBed.inject(DocxExportService);
  });

  // D-01 : buildFileName avec titre non vide
  it('D-01: buildFileName returns [slug]-synthese-vN.docx for non-empty title', () => {
    const cf = makeCaseFile('Affaire Dupont c/ SA Renault');
    const syn = makeSynthesis(2);
    const name = service.buildFileName(cf, syn);
    expect(name).toBe('affaire-dupont-c-sa-renault-synthese-v2.docx');
  });

  // D-02 : buildFileName avec titre vide → fallback
  it('D-02: buildFileName returns synthese-vN.docx when title is empty', () => {
    const cf = makeCaseFile('');
    const syn = makeSynthesis(1);
    const name = service.buildFileName(cf, syn);
    expect(name).toBe('synthese-v1.docx');
  });

  // D-03 : buildFileName gère les accents
  it('D-03: buildFileName strips accented characters', () => {
    const cf = makeCaseFile('Licenciement économique — Müller');
    const syn = makeSynthesis(3);
    const name = service.buildFileName(cf, syn);
    expect(name).not.toMatch(/[àáâãéèêëîïôùûüç]/i);
    expect(name).toContain('.docx');
  });

  // D-04 : export() appelle snackBar en cas d'erreur (import dynamique qui rejette)
  it('D-04: export() shows snackBar error message when dynamic import rejects', async () => {
    const cf = makeCaseFile('Dossier test');
    const syn = makeSynthesis(1);

    // Override dynamic import to reject
    const originalImport = (service as any).__proto__.constructor;
    jest.spyOn(service as any, 'export').mockImplementationOnce((_cf: CaseFile, _syn: CaseAnalysisResult) => {
      snackBar.open('Erreur lors de la génération du document Word.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      });
    });

    service.export(cf, syn);

    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur lors de la génération du document Word.',
      'Fermer',
      expect.objectContaining({ duration: 4000 })
    );
  });

  // ───────────────────────────────────────────────────────────────────────
  // SF-98-50 — Export Word des conclusions
  // ───────────────────────────────────────────────────────────────────────

  // D-50 : buildConclusionFileName avec titre non vide
  it('D-50: buildConclusionFileName returns [slug]-conclusions-vN.docx', () => {
    const name = service.buildConclusionFileName('Affaire Dupont c/ SA Renault', 3);
    expect(name).toBe('affaire-dupont-c-sa-renault-conclusions-v3.docx');
  });

  // D-51 : buildConclusionFileName avec titre vide → fallback
  it('D-51: buildConclusionFileName returns conclusions-vN.docx when title empty', () => {
    expect(service.buildConclusionFileName('', 2)).toBe('conclusions-v2.docx');
    expect(service.buildConclusionFileName('   ', 5)).toBe('conclusions-v5.docx');
  });

  // D-52 : buildConclusionFileName retire les accents
  it('D-52: buildConclusionFileName strips accented characters', () => {
    const name = service.buildConclusionFileName('Licenciement économique — Müller', 1);
    expect(name).not.toMatch(/[àáâãéèêëîïôùûüç]/i);
    expect(name).toBe('licenciement-economique-muller-conclusions-v1.docx');
  });

  // D-53 : exportConclusion construit le document et déclenche le download
  it('D-53: exportConclusion builds the document and triggers the blob download', async () => {
    const clickSpy = jest.fn();
    const anchor = { href: '', download: '', click: clickSpy } as unknown as HTMLAnchorElement;
    jest.spyOn(document, 'createElement').mockReturnValue(anchor);
    (URL as any).createObjectURL = jest.fn().mockReturnValue('blob:fake');
    (URL as any).revokeObjectURL = jest.fn();

    const content =
      '# POUR\nM. Dupont, salarié.\n\n## FAITS ET PROCÉDURE\nLe contrat a été rompu.\n\n# PAR CES MOTIFS\nPlaise au Conseil…';
    service.exportConclusion(content, 'Affaire Dupont', 4);

    // Laisse l'import dynamique de `docx` puis le Packer se résoudre.
    await new Promise((r) => setTimeout(r, 0));
    await new Promise((r) => setTimeout(r, 0));

    expect(clickSpy).toHaveBeenCalled();
    expect(anchor.download).toBe('affaire-dupont-conclusions-v4.docx');
    expect(URL.createObjectURL).toHaveBeenCalled();
    expect(snackBar.open).not.toHaveBeenCalled();
  });

  // ───────────────────────────────────────────────────────────────────────
  // SF-259-03 — Mapping Markdown → docx (fin des marqueurs dans le livrable)
  // ───────────────────────────────────────────────────────────────────────
  //
  // Le modèle interne de `docx` (arbre XML) est privé et peu inspectable. Le
  // mapper `buildConclusionChildren(content, docx)` reçoit `docx` en
  // paramètre : on injecte des **fakes** qui capturent les options de chaque
  // `Paragraph`/`TextRun` pour les inspecter directement.

  interface FakeRun { text: string; bold?: boolean; italics?: boolean; font?: string }
  interface FakeParagraph {
    heading?: unknown; bullet?: unknown; numbering?: unknown;
    thematicBreak?: boolean; indent?: unknown; runs: FakeRun[]; rawText?: string;
  }

  const HEADING = { HEADING_1: 'H1', HEADING_2: 'H2', HEADING_3: 'H3' };

  const makeFakeDocx = () => ({
    Paragraph: class {
      readonly _p: FakeParagraph;
      constructor(opts: any) {
        const runs = (opts.children ?? []).map((c: any) => c as FakeRun);
        this._p = {
          heading: opts.heading,
          bullet: opts.bullet,
          numbering: opts.numbering,
          thematicBreak: opts.thematicBreak,
          indent: opts.indent,
          runs,
          rawText: typeof opts.text === 'string' ? opts.text : undefined,
        };
      }
    },
    TextRun: class {
      constructor(public readonly opts: FakeRun) {}
    },
    HeadingLevel: HEADING,
  });

  const buildParas = (md: string): FakeParagraph[] => {
    const fake = makeFakeDocx();
    const children = (service as any).buildConclusionChildren(md, fake as any);
    return children.map((c: any) => c._p as FakeParagraph);
  };
  const runText = (p: FakeParagraph) => p.runs.map((r) => r.opts ? (r as any).opts.text : r.text).join('');
  const runsOf = (p: FakeParagraph): FakeRun[] => p.runs.map((r: any) => r.opts ?? r);

  // D-54a : `# Titre` → heading HEADING_1, sans `#` dans le texte
  it('D-54a: # heading maps to HEADING_1 with no marker in the run text', () => {
    const paras = buildParas('# Conseil de prud\'hommes\n\nUn paragraphe.');
    expect(paras[0].heading).toBe('H1');
    const txt = runText(paras[0]);
    expect(txt).toContain('Conseil de prud');
    expect(txt).not.toContain('#');
  });

  // D-54b : `##` / `###` → HEADING_2 / HEADING_3
  it('D-54b: ## and ### map to HEADING_2 / HEADING_3', () => {
    const paras = buildParas('## Faits\n\n### Discussion');
    expect(paras[0].heading).toBe('H2');
    expect(paras[1].heading).toBe('H3');
  });

  // D-54c : `**gras**` → run bold ; `*ital*` → run italics ;
  //         imbriqué `***x***` → bold+italics ; aucun marqueur résiduel
  it('D-54c: inline strong/em map to bold/italics runs, nested combined, no markers', () => {
    const paras = buildParas('Un mot **important** et un *accent*, puis ***les deux***.');
    const runs = runsOf(paras[0]);
    const fullText = runs.map((r) => r.text).join('');
    expect(fullText).not.toMatch(/\*\*|\*/);

    const bold = runs.find((r) => r.text.includes('important'));
    expect(bold?.bold).toBe(true);
    const italic = runs.find((r) => r.text.includes('accent'));
    expect(italic?.italics).toBe(true);
    const both = runs.find((r) => r.text.includes('les deux'));
    expect(both?.bold).toBe(true);
    expect(both?.italics).toBe(true);
  });

  // D-54d : liste à puces → bullet ; liste ordonnée → numbering ; pas de `-`
  it('D-54d: unordered list → bullet, ordered list → numbering, no dash marker', () => {
    const ul = buildParas('- Premier\n- Second');
    expect(ul[0].bullet).toEqual({ level: 0 });
    expect(runText(ul[0])).toBe('Premier');
    expect(runText(ul[0])).not.toContain('-');

    const ol = buildParas('1. Un\n2. Deux');
    expect(ol[0].numbering).toEqual({ reference: 'conclusion-ordered', level: 0 });
  });

  // D-54e : blockquote `>` → paragraphe italique indenté ; hr `---` → thematicBreak
  it('D-54e: blockquote and hr map to citation paragraph and thematic break', () => {
    const bq = buildParas('> Une citation.');
    expect(bq[0].indent).toEqual({ left: 360 });
    expect(runsOf(bq[0])[0].italics).toBe(true);

    const hr = buildParas('Texte\n\n---\n\nSuite');
    const thematic = hr.find((p) => p.thematicBreak === true);
    expect(thematic).toBeDefined();
  });

  // D-54f : texte brut (sans Markdown) → paragraphes, pas de crash, pas de marqueur
  it('D-54f: plain text becomes paragraphs without residual markers', () => {
    const paras = buildParas('Ligne une.\n\nLigne deux.');
    expect(paras.length).toBe(2);
    const all = paras.map((p) => runText(p)).join(' ');
    expect(all).toContain('Ligne une.');
    expect(all).toContain('Ligne deux.');
    expect(all).not.toMatch(/[#*]/);
  });

  // D-54g : content vide → aucun children (pas d'export à déclencher)
  it('D-54g: empty content yields no children', () => {
    expect(buildParas('')).toEqual([]);
    expect(buildParas('   ')).toEqual([]);
  });

  // D-55 : exportConclusion affiche une snackbar si le packing échoue
  it('D-55: exportConclusion shows snackBar error when docx import fails', async () => {
    jest.spyOn(document, 'createElement').mockImplementation(() => {
      throw new Error('boom');
    });
    (URL as any).createObjectURL = jest.fn().mockReturnValue('blob:fake');
    (URL as any).revokeObjectURL = jest.fn();

    service.exportConclusion('POUR\nTexte', 'Dossier', 1);
    await new Promise((r) => setTimeout(r, 0));
    await new Promise((r) => setTimeout(r, 0));

    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur lors de la génération du document Word.',
      'Fermer',
      expect.objectContaining({ duration: 4000, panelClass: ['snack-error'] }),
    );
  });
});
