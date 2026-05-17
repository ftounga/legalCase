import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AnalysisItem, CaseAnalysisResult } from '../models/case-analysis.model';
import { CaseFile } from '../models/case-file.model';
import { formatSourceRef } from '../utils/format-source-ref';

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
   * Construit un `Document` `docx` \u00e0 partir du `content` texte de la version :
   * les lignes d'en-t\u00eate de section (enti\u00e8rement en MAJUSCULES, courtes \u2014 type
   * `POUR`, `CONTRE`, `FAITS ET PROC\u00c9DURE`, `DISCUSSION`, `PAR CES MOTIFS`)
   * sont rendues en titres ; les autres lignes en paragraphes. Les lignes vides
   * sont pr\u00e9serv\u00e9es comme paragraphes vides pour conserver l'a\u00e9ration du texte.
   *
   * R\u00e9utilise le pattern F-95 : import dynamique de `docx`, g\u00e9n\u00e9ration d'un
   * blob via `Packer`, t\u00e9l\u00e9chargement d\u00e9clench\u00e9 par un `<a download>`. Un \u00e9chec
   * (import ou packing) affiche une `MatSnackBar` d'erreur, sans t\u00e9l\u00e9chargement.
   *
   * @param content        texte de la version de conclusions.
   * @param caseTitle      titre du dossier \u2014 sert au nommage du fichier.
   * @param versionNumber  num\u00e9ro de la version export\u00e9e.
   */
  exportConclusion(content: string, caseTitle: string, versionNumber: number): void {
    import('docx').then(({ Document, Packer, Paragraph, HeadingLevel }) => {
      const lines = (content ?? '').split('\n');
      const children = lines.map((line) =>
        this.isSectionHeading(line)
          ? new Paragraph({ text: line.trim(), heading: HeadingLevel.HEADING_1 })
          : new Paragraph({ text: line }),
      );

      const doc = new Document({
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
   * Vrai si la ligne est un en-t\u00eate de section : une fois les espaces et la
   * ponctuation retir\u00e9s, elle ne contient que des lettres en MAJUSCULES, au
   * moins une lettre, et reste courte (\u2264 60 caract\u00e8res). Robuste pour la
   * structure produite par `CaseConclusionPromptBuilder` (`POUR`, `CONTRE`,
   * `FAITS ET PROC\u00c9DURE`, `DISCUSSION`, `PAR CES MOTIFS`\u2026).
   */
  private isSectionHeading(line: string): boolean {
    const trimmed = line.trim();
    if (trimmed.length === 0 || trimmed.length > 60) {
      return false;
    }
    // Garde uniquement les lettres (Unicode) pour comparer casse et pr\u00e9sence.
    const letters = trimmed.replace(/[^\p{L}]/gu, '');
    if (letters.length === 0) {
      return false;
    }
    return letters === letters.toUpperCase() && letters !== letters.toLowerCase();
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
