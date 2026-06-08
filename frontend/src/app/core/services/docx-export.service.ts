import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AnalysisItem, CaseAnalysisResult } from '../models/case-analysis.model';
import { CaseFile } from '../models/case-file.model';
import { formatSourceRef } from '../utils/format-source-ref';
import { InlineRun, flattenInline, inlineRunsOf, lexMarkdown } from '../utils/markdown-tokens';
import { Token, Tokens } from 'marked';

@Injectable({ providedIn: 'root' })
export class DocxExportService {

  constructor(private snackBar: MatSnackBar) {}

  export(caseFile: CaseFile, synthesis: CaseAnalysisResult): void {
    import('docx').then(({ Document, Packer, Paragraph, TextRun, HeadingLevel }) => {
      const exportDate = new Date().toLocaleDateString('fr-FR', {
        day: '2-digit', month: 'long', year: 'numeric'
      });
      const typeLabel = synthesis.analysisType === 'ENRICHED' ? 'Synthèse enrichie' : 'Synthèse initiale';
      const versionLabel = `v${synthesis.version} — ${typeLabel} — Exporté le ${exportDate}`;

      const children = [
        new Paragraph({
          text: caseFile.title || 'Synthèse juridique',
          heading: HeadingLevel.TITLE,
        }),
        new Paragraph({
          children: [new TextRun({ text: versionLabel, italics: true })],
          spacing: { after: 400 },
        }),
      ];

      // Timeline
      if (synthesis.timeline.length > 0) {
        children.push(
          new Paragraph({ text: 'Timeline', heading: HeadingLevel.HEADING_1 })
        );
        for (const entry of synthesis.timeline) {
          children.push(
            new Paragraph({
              children: [
                new TextRun({ text: entry.date, bold: true }),
                new TextRun({ text: ` — ${entry.evenement}` }),
              ],
              bullet: { level: 0 },
            })
          );
        }
      }

      const buildItemRuns = (item: AnalysisItem) => {
        const runs = [new TextRun({ text: item.texte })];
        const src = formatSourceRef(item);
        if (src) runs.push(new TextRun({ text: ` [Source : ${src}]`, italics: true }));
        return runs;
      };

      // Faits établis
      if (synthesis.faits.length > 0) {
        children.push(
          new Paragraph({ text: 'Faits établis', heading: HeadingLevel.HEADING_1 })
        );
        for (const item of synthesis.faits) {
          children.push(new Paragraph({ children: buildItemRuns(item), bullet: { level: 0 } }));
        }
      }

      // Points juridiques
      if (synthesis.pointsJuridiques.length > 0) {
        children.push(
          new Paragraph({ text: 'Points juridiques', heading: HeadingLevel.HEADING_1 })
        );
        for (const item of synthesis.pointsJuridiques) {
          children.push(new Paragraph({ children: buildItemRuns(item), bullet: { level: 0 } }));
        }
      }

      // Risques
      if (synthesis.risques.length > 0) {
        children.push(
          new Paragraph({ text: 'Risques', heading: HeadingLevel.HEADING_1 })
        );
        for (const item of synthesis.risques) {
          children.push(new Paragraph({ children: buildItemRuns(item), bullet: { level: 0 } }));
        }
      }

      // Score de risque
      if (synthesis.riskLevel != null) {
        children.push(
          new Paragraph({ text: 'Score de risque', heading: HeadingLevel.HEADING_1 })
        );
        const riskLabels: Record<string, string> = { FAIBLE: 'Faible', MOYEN: 'Moyen', ELEVE: 'Élevé' };
        const riskLabelText = riskLabels[synthesis.riskLevel] ?? synthesis.riskLevel;
        const riskText = synthesis.riskScore != null
          ? `Niveau : ${riskLabelText} (${synthesis.riskScore}/100)`
          : `Niveau : ${riskLabelText}`;
        children.push(new Paragraph({ children: [new TextRun({ text: riskText })] }));
      }

      // Questions ouvertes
      if (synthesis.questionsOuvertes.length > 0) {
        children.push(
          new Paragraph({ text: 'Questions ouvertes', heading: HeadingLevel.HEADING_1 })
        );
        for (const question of synthesis.questionsOuvertes) {
          children.push(new Paragraph({ children: [new TextRun({ text: question })], bullet: { level: 0 } }));
        }
      }

      // Pièces manquantes (omit si vide)
      if (synthesis.piecesManquantes && synthesis.piecesManquantes.length > 0) {
        children.push(
          new Paragraph({ text: 'Pièces manquantes', heading: HeadingLevel.HEADING_1 })
        );
        for (const piece of synthesis.piecesManquantes) {
          children.push(new Paragraph({ children: [new TextRun({ text: piece })], bullet: { level: 0 } }));
        }
      }

      const doc = new Document({
        sections: [{ children }],
      });

      Packer.toBlob(doc).then(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.buildFileName(caseFile, synthesis);
        a.click();
        URL.revokeObjectURL(url);
      }).catch(() => {
        this.snackBar.open('Erreur lors de la génération du document Word.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      });
    }).catch(() => {
      this.snackBar.open('Erreur lors de la génération du document Word.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      });
    });
  }

  buildFileName(caseFile: CaseFile, synthesis: CaseAnalysisResult): string {
    const title = caseFile.title?.trim() || '';
    if (!title) {
      return `synthese-v${synthesis.version}.docx`;
    }
    return `${this.slugify(title)}-synthese-v${synthesis.version}.docx`;
  }

  /**
   * F-98 / SF-98-50 \u2014 Exporte une version de conclusions au format `.docx`.
   *
   * F-259 / SF-259-03 : le `content` est en **Markdown** (`#`/`##`/`###`,
   * `**gras**`, `*italique*`, listes, `>`, `---`, code). On le tokenise via
   * `marked.lexer` (util partag\u00e9 `markdown-tokens`) et on mappe chaque token
   * de bloc vers son \u00e9quivalent `docx` (heading 1/2/3, paragraphe \u00e0 runs
   * gras/italique, listes \u00e0 puces/num\u00e9rot\u00e9es, citation indent\u00e9e, filet,
   * code mono). Le document est **neutre** (aucun logo / cartouche LegalCase)
   * pour que l'avocat le reprenne sur son en-t\u00eate de cabinet. Plus aucun
   * marqueur Markdown (`#`/`**`/`*`/`-`) n'appara\u00eet dans le texte rendu.
   *
   * R\u00e9utilise le pattern F-95 : import dynamique de `docx`, g\u00e9n\u00e9ration d'un
   * blob via `Packer`, t\u00e9l\u00e9chargement d\u00e9clench\u00e9 par un `<a download>`. Un \u00e9chec
   * (import ou packing) affiche une `MatSnackBar` d'erreur, sans t\u00e9l\u00e9chargement.
   *
   * @param content        texte (Markdown) de la version de conclusions.
   * @param caseTitle      titre du dossier \u2014 sert au nommage du fichier.
   * @param versionNumber  num\u00e9ro de la version export\u00e9e.
   */
  exportConclusion(content: string, caseTitle: string, versionNumber: number): void {
    import('docx').then((docx) => {
      const { Document, Packer, LevelFormat, AlignmentType } = docx;
      const children = this.buildConclusionChildren(content, docx);

      const doc = new Document({
        // Numérotation référencée par les listes ordonnées (1. 2. 3.).
        numbering: {
          config: [{
            reference: 'conclusion-ordered',
            levels: [{
              level: 0,
              format: LevelFormat.DECIMAL,
              text: '%1.',
              alignment: AlignmentType.START,
            }],
          }],
        },
        sections: [{ children }],
      });

      Packer.toBlob(doc).then(blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = this.buildConclusionFileName(caseTitle, versionNumber);
        a.click();
        URL.revokeObjectURL(url);
      }).catch(() => {
        this.snackBar.open('Erreur lors de la g\u00e9n\u00e9ration du document Word.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      });
    }).catch(() => {
      this.snackBar.open('Erreur lors de la g\u00e9n\u00e9ration du document Word.', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      });
    });
  }

  /**
   * Nom de fichier d'une version de conclusions :
   * `{slug-du-dossier}-conclusions-v{N}.docx` (fallback `conclusions-vN.docx`
   * si le titre est vide). R\u00e9utilise le m\u00eame slug que `buildFileName`.
   */
  buildConclusionFileName(caseTitle: string, versionNumber: number): string {
    const title = caseTitle?.trim() || '';
    if (!title) {
      return `conclusions-v${versionNumber}.docx`;
    }
    return `${this.slugify(title)}-conclusions-v${versionNumber}.docx`;
  }

  /**
   * F-259 / SF-259-03 \u2014 Mappe le `content` Markdown d'une version de
   * conclusions vers les `children` `docx` (un `Paragraph` par bloc, avec un
   * `TextRun` par run inline). Visible (non priv\u00e9e) pour les tests unitaires.
   *
   * `docx` est pass\u00e9 en param\u00e8tre (d\u00e9j\u00e0 import\u00e9 dynamiquement par l'appelant)
   * pour \u00e9viter un second import et garder la m\u00e9thode testable en isolation.
   */
  buildConclusionChildren(content: string, docx: typeof import('docx')): import('docx').Paragraph[] {
    const tokens = lexMarkdown(content);
    const children: import('docx').Paragraph[] = [];
    for (const token of tokens) {
      children.push(...this.mapBlockToken(token, docx));
    }
    return children;
  }

  /** Mappe un token de bloc Markdown vers un ou plusieurs `Paragraph` docx. */
  private mapBlockToken(token: Token, docx: typeof import('docx')): import('docx').Paragraph[] {
    const { Paragraph, HeadingLevel } = docx;
    switch (token.type) {
      case 'heading': {
        const h = token as Tokens.Heading;
        const level = h.depth <= 1
          ? HeadingLevel.HEADING_1
          : h.depth === 2
            ? HeadingLevel.HEADING_2
            : HeadingLevel.HEADING_3;
        return [new Paragraph({ heading: level, children: this.runsToTextRuns(inlineRunsOf(h), docx) })];
      }
      case 'paragraph': {
        const p = token as Tokens.Paragraph;
        return [new Paragraph({ children: this.runsToTextRuns(inlineRunsOf(p), docx) })];
      }
      case 'list': {
        const list = token as Tokens.List;
        return list.items.map((item, idx) =>
          this.mapListItem(item, list.ordered, idx, docx));
      }
      case 'blockquote': {
        const bq = token as Tokens.Blockquote;
        // On aplatit les blocs internes de la citation en un paragraphe
        // indent\u00e9 / italique (style citation sobre).
        const runs = this.runsToTextRuns(flattenInline(bq.tokens), docx, { italics: true });
        return [new Paragraph({ children: runs, indent: { left: 360 }, spacing: { before: 60, after: 60 } })];
      }
      case 'hr':
        return [new Paragraph({ thematicBreak: true })];
      case 'code': {
        const c = token as Tokens.Code;
        const { TextRun } = docx;
        return [new Paragraph({
          children: [new TextRun({ text: c.text, font: 'Courier New' })],
          spacing: { before: 60, after: 60 },
        })];
      }
      case 'space':
        return [];
      default: {
        // Tout autre bloc (table, html\u2026) : on retombe sur son texte brut
        // sans marqueur (defensive). Vide \u2192 ignor\u00e9.
        const anyToken = token as { text?: string };
        if (typeof anyToken.text === 'string' && anyToken.text.trim().length > 0) {
          return [new Paragraph({ text: anyToken.text })];
        }
        return [];
      }
    }
  }

  /** Mappe un item de liste vers un `Paragraph` \u00e0 puce ou num\u00e9rot\u00e9. */
  private mapListItem(
    item: Tokens.ListItem,
    ordered: boolean,
    index: number,
    docx: typeof import('docx'),
  ): import('docx').Paragraph {
    const { Paragraph } = docx;
    const runs = this.runsToTextRuns(inlineRunsOf(item), docx);
    if (ordered) {
      return new Paragraph({ children: runs, numbering: { reference: 'conclusion-ordered', level: 0 } });
    }
    return new Paragraph({ children: runs, bullet: { level: 0 } });
  }

  /**
   * Convertit des runs inline `{ text, bold, italics, code }` en `TextRun`
   * docx (`strong`\u2192`bold:true`, `em`\u2192`italics:true`, combin\u00e9 si imbriqu\u00e9,
   * `code`\u2192police mono). `forced` permet d'imposer un style (ex. citation en
   * italique). Aucun marqueur Markdown ne subsiste dans le texte.
   */
  private runsToTextRuns(
    runs: InlineRun[],
    docx: typeof import('docx'),
    forced: { bold?: boolean; italics?: boolean } = {},
  ): import('docx').TextRun[] {
    const { TextRun } = docx;
    if (runs.length === 0) {
      return [new TextRun({ text: '' })];
    }
    return runs.map((run) => new TextRun({
      text: run.text,
      bold: run.bold || forced.bold || undefined,
      italics: run.italics || forced.italics || undefined,
      font: run.code ? 'Courier New' : undefined,
    }));
  }

  /** Slug ASCII minuscule, accents retir\u00e9s, tronqu\u00e9 \u00e0 40 caract\u00e8res. */
  private slugify(value: string): string {
    return value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40);
  }
}
