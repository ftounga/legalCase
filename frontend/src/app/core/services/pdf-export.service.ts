import { Injectable } from '@angular/core';
import { TDocumentDefinitions } from 'pdfmake/interfaces';
import { CaseAnalysisResult } from '../models/case-analysis.model';
import { CaseFile } from '../models/case-file.model';
import { LEGALCASE_LOGO_BASE64 } from '../assets/logo-base64';

const PRIMARY = '#1A3A5C';
const ACCENT = '#C9973A';
const ERROR = '#C0392B';
const ERROR_BG = '#FFEBEE';
const TEXT = '#1C2B3A';
const TEXT_SECONDARY = '#6B7A8D';
const DIVIDER = '#E0E4EA';
const SURFACE = '#FFFFFF';
const BG = '#F5F6FA';

@Injectable({ providedIn: 'root' })
export class PdfExportService {

  export(caseFile: CaseFile, synthesis: CaseAnalysisResult): void {
    import('pdfmake/build/pdfmake').then(pdfMakeModule => {
      import('pdfmake/build/vfs_fonts').then(vfsFontsModule => {
        const pdfMake = (pdfMakeModule.default || pdfMakeModule) as any;
        const vfsFonts = (vfsFontsModule.default || vfsFontsModule) as any;
        pdfMake.vfs = vfsFonts.pdfMake ? vfsFonts.pdfMake.vfs : vfsFonts.vfs;

        const docDefinition = this.buildDocument(caseFile, synthesis) as TDocumentDefinitions;
        const fileName = this.buildFileName(caseFile.title, synthesis);
        pdfMake.createPdf(docDefinition).download(fileName);
      });
    });
  }

  buildDocument(caseFile: CaseFile, synthesis: CaseAnalysisResult): object {
    const isEnriched = synthesis.analysisType === 'ENRICHED';
    const exportDate = new Date().toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric'
    });
    const versionLabel = isEnriched
      ? `v${synthesis.version} — Synthèse enrichie`
      : `v${synthesis.version} — Synthèse initiale`;

    const content: object[] = [
      ...this.buildCoverPage(caseFile.title, isEnriched, versionLabel, exportDate),
      { text: '', pageBreak: 'after' },
      ...this.buildSections(synthesis),
    ];

    return {
      pageSize: 'A4',
      pageMargins: [48, 64, 48, 64],
      content,
      footer: (currentPage: number, pageCount: number) =>
        this.buildFooter(currentPage, pageCount),
      defaultStyle: {
        font: 'Roboto',
        fontSize: 10,
        color: TEXT,
        lineHeight: 1.4,
      },
      styles: this.buildStyles(),
    };
  }

  private buildCoverPage(
    title: string,
    isEnriched: boolean,
    versionLabel: string,
    exportDate: string
  ): object[] {
    return [
      { text: '', margin: [0, 60, 0, 0] },
      {
        image: LEGALCASE_LOGO_BASE64,
        width: 200,
        alignment: 'center',
        margin: [0, 0, 0, 32],
      },
      {
        canvas: [
          { type: 'line', x1: 80, y1: 0, x2: 420, y2: 0, lineWidth: 2, lineColor: ACCENT },
        ],
        margin: [0, 0, 0, 24],
      },
      {
        text: title || 'Synthèse juridique',
        style: 'coverTitle',
        alignment: 'center',
        margin: [0, 0, 0, 20],
      },
      {
        table: {
          widths: ['*'],
          body: [[
            {
              text: isEnriched ? 'Synthèse enrichie' : 'Synthèse initiale',
              alignment: 'center',
              color: SURFACE,
              bold: true,
              fontSize: 12,
              fillColor: isEnriched ? ACCENT : PRIMARY,
              margin: [0, 6, 0, 6],
            }
          ]]
        },
        layout: 'noBorders',
        margin: [120, 0, 120, 20],
      },
      {
        text: versionLabel,
        style: 'coverMeta',
        alignment: 'center',
        margin: [0, 0, 0, 4],
      },
      {
        text: `Exporté le ${exportDate}`,
        style: 'coverMeta',
        alignment: 'center',
        margin: [0, 0, 0, 48],
      },
      {
        canvas: [
          { type: 'line', x1: 0, y1: 0, x2: 500, y2: 0, lineWidth: 0.5, lineColor: DIVIDER },
        ],
        margin: [0, 0, 0, 16],
      },
      {
        text: 'Ce document est confidentiel et destiné exclusivement à l\'usage professionnel de son destinataire.',
        style: 'coverDisclaimer',
        alignment: 'center',
      },
    ];
  }

  private buildSections(synthesis: CaseAnalysisResult): object[] {
    const sections: object[] = [];

    if (synthesis.timeline.length > 0) {
      sections.push(
        this.buildSectionHeader('Chronologie', 'timeline', synthesis.timeline.length, 'événement'),
        this.buildTimelineTable(synthesis.timeline),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    if (synthesis.faits.length > 0) {
      sections.push(
        this.buildSectionHeader('Faits', 'faits', synthesis.faits.length, 'fait'),
        ...this.buildNumberedList(synthesis.faits, ACCENT),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    if (synthesis.pointsJuridiques.length > 0) {
      sections.push(
        this.buildSectionHeader('Points juridiques', 'juridique', synthesis.pointsJuridiques.length, 'point'),
        ...this.buildNumberedList(synthesis.pointsJuridiques, PRIMARY),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    if (synthesis.risques.length > 0) {
      sections.push(
        this.buildSectionHeader('Risques', 'risques', synthesis.risques.length, 'risque'),
        ...this.buildRisquesList(synthesis.risques),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    if (synthesis.questionsOuvertes.length > 0) {
      sections.push(
        this.buildSectionHeader('Questions ouvertes', 'questions', synthesis.questionsOuvertes.length, 'question'),
        ...this.buildNumberedList(synthesis.questionsOuvertes, TEXT_SECONDARY),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    return sections;
  }

  private buildSectionHeader(label: string, _key: string, count: number, unit: string): object {
    return {
      table: {
        widths: ['*', 'auto'],
        body: [[
          { text: label, color: SURFACE, bold: true, fontSize: 13, margin: [12, 8, 0, 8] },
          {
            text: `${count} ${count > 1 ? unit + 's' : unit}`,
            color: SURFACE,
            fontSize: 9,
            italics: true,
            margin: [0, 10, 12, 8],
            alignment: 'right',
          }
        ]]
      },
      layout: 'noBorders',
      fillColor: PRIMARY,
      margin: [0, 0, 0, 8],
    };
  }

  private buildTimelineTable(timeline: { date: string; evenement: string }[]): object {
    const rows = timeline.map((entry, i) => [
      {
        text: entry.date,
        bold: true,
        color: PRIMARY,
        fontSize: 9,
        fillColor: i % 2 === 0 ? BG : SURFACE,
        margin: [8, 5, 4, 5],
      },
      {
        text: entry.evenement,
        fontSize: 10,
        fillColor: i % 2 === 0 ? BG : SURFACE,
        margin: [4, 5, 8, 5],
      }
    ]);

    return {
      table: {
        widths: [90, '*'],
        headerRows: 1,
        body: [
          [
            { text: 'Date', bold: true, color: SURFACE, fillColor: ACCENT, margin: [8, 5, 4, 5], fontSize: 9 },
            { text: 'Événement', bold: true, color: SURFACE, fillColor: ACCENT, margin: [4, 5, 8, 5], fontSize: 9 },
          ],
          ...rows
        ]
      },
      layout: {
        hLineWidth: () => 0.5,
        vLineWidth: () => 0,
        hLineColor: () => DIVIDER,
      },
      margin: [0, 0, 0, 0],
    };
  }

  private buildNumberedList(items: string[], bulletColor: string): object[] {
    return items.map((item, i) => ({
      columns: [
        {
          text: `${i + 1}.`,
          width: 24,
          bold: true,
          color: bulletColor,
          fontSize: 10,
          margin: [0, 3, 0, 3],
        },
        {
          text: item,
          fontSize: 10,
          margin: [0, 3, 0, 3],
        }
      ],
      margin: [8, 0, 0, 4],
    }));
  }

  private buildRisquesList(risques: string[]): object[] {
    return risques.map(risque => ({
      table: {
        widths: [16, '*'],
        body: [[
          {
            text: '▲',
            color: ERROR,
            bold: true,
            fontSize: 10,
            fillColor: ERROR_BG,
            margin: [8, 5, 4, 5],
            border: [false, false, false, false],
          },
          {
            text: risque,
            fontSize: 10,
            fillColor: ERROR_BG,
            margin: [4, 5, 8, 5],
            border: [false, false, false, false],
          }
        ]]
      },
      layout: 'noBorders',
      margin: [0, 0, 0, 4],
    }));
  }

  private buildFooter(currentPage: number, pageCount: number): object {
    return {
      margin: [48, 8, 48, 0],
      columns: [
        {
          stack: [
            { canvas: [{ type: 'line', x1: 0, y1: 0, x2: 500, y2: 0, lineWidth: 0.5, lineColor: DIVIDER }] },
            { text: 'AI LegalCase — Confidentiel', fontSize: 8, color: TEXT_SECONDARY, margin: [0, 4, 0, 0] },
          ]
        },
        {
          text: `Page ${currentPage} / ${pageCount}`,
          fontSize: 8,
          color: TEXT_SECONDARY,
          alignment: 'right',
          margin: [0, 13, 0, 0],
        }
      ]
    };
  }

  private buildStyles(): object {
    return {
      coverTitle: {
        fontSize: 24,
        bold: true,
        color: PRIMARY,
      },
      coverMeta: {
        fontSize: 11,
        color: TEXT_SECONDARY,
      },
      coverDisclaimer: {
        fontSize: 8,
        color: TEXT_SECONDARY,
        italics: true,
      },
    };
  }

  buildFileName(title: string, synthesis: CaseAnalysisResult): string {
    const slug = (title || 'export')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40);
    const date = new Date().toISOString().slice(0, 10);
    return `synthese-${slug}-v${synthesis.version}-${date}.pdf`;
  }
}
