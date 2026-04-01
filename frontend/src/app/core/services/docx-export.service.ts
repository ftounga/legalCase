import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseAnalysisResult } from '../models/case-analysis.model';
import { CaseFile } from '../models/case-file.model';

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

      const children: Paragraph[] = [
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

      // Faits établis
      if (synthesis.faits.length > 0) {
        children.push(
          new Paragraph({ text: 'Faits établis', heading: HeadingLevel.HEADING_1 })
        );
        for (const item of synthesis.faits) {
          const runs: TextRun[] = [new TextRun({ text: item.texte })];
          if (item.source) {
            runs.push(new TextRun({ text: ` [Source : ${item.source}` + (item.extrait ? ` — « ${item.extrait} »` : '') + ']', italics: true }));
          }
          children.push(new Paragraph({ children: runs, bullet: { level: 0 } }));
        }
      }

      // Points juridiques
      if (synthesis.pointsJuridiques.length > 0) {
        children.push(
          new Paragraph({ text: 'Points juridiques', heading: HeadingLevel.HEADING_1 })
        );
        for (const item of synthesis.pointsJuridiques) {
          const runs: TextRun[] = [new TextRun({ text: item.texte })];
          if (item.source) {
            runs.push(new TextRun({ text: ` [Source : ${item.source}` + (item.extrait ? ` — « ${item.extrait} »` : '') + ']', italics: true }));
          }
          children.push(new Paragraph({ children: runs, bullet: { level: 0 } }));
        }
      }

      // Risques
      if (synthesis.risques.length > 0) {
        children.push(
          new Paragraph({ text: 'Risques', heading: HeadingLevel.HEADING_1 })
        );
        for (const item of synthesis.risques) {
          const runs: TextRun[] = [new TextRun({ text: item.texte })];
          if (item.source) {
            runs.push(new TextRun({ text: ` [Source : ${item.source}` + (item.extrait ? ` — « ${item.extrait} »` : '') + ']', italics: true }));
          }
          children.push(new Paragraph({ children: runs, bullet: { level: 0 } }));
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
    const slug = title
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40);
    return `${slug}-synthese-v${synthesis.version}.docx`;
  }
}
