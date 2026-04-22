import { Injectable } from '@angular/core';
import { TDocumentDefinitions } from 'pdfmake/interfaces';
import { AnalysisItem, CaseAnalysisResult, CompensationEstimate, PensionAlimentaireEstimate, PrestationCompensatoireEstimate, LiquidationCommunaute } from '../models/case-analysis.model';
import { formatSourceRef } from '../utils/format-source-ref';
import { CaseFile } from '../models/case-file.model';
import { ProcedureCheck } from '../models/procedure-check.model';
import { PrudhomeFiche } from '../models/prudhome-fiche.model';
import { TribunalTravailFiche } from '../models/tribunal-travail-fiche.model';
import { ImmigrationChecklist } from '../models/immigration-checklist.model';
import { RecoursResponse } from '../models/immigration-recours.model';
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
        ...this.buildNumberedListFromItems(synthesis.faits, ACCENT),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    if (synthesis.pointsJuridiques.length > 0) {
      sections.push(
        this.buildSectionHeader('Points juridiques', 'juridique', synthesis.pointsJuridiques.length, 'point'),
        ...this.buildNumberedListFromItems(synthesis.pointsJuridiques, PRIMARY),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    if (synthesis.risques.length > 0) {
      sections.push(
        this.buildSectionHeader('Risques', 'risques', synthesis.risques.length, 'risque'),
        ...this.buildRisquesListFromItems(synthesis.risques),
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

    // Fix F-DT-09-BE : compensationEstimate est désormais aussi renseigné côté BE
    // (pour alimenter les alertes F-IA-03 du comparateur), mais ses valeurs Macron
    // ne doivent pas apparaître dans le PDF d'un dossier BE où seul le panneau BE
    // (CCT 109) est pertinent.
    if (synthesis.compensationEstimate && !synthesis.belgianCompensationEstimate) {
      sections.push(
        ...this.buildCompensationSection(synthesis.compensationEstimate),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    if (synthesis.pensionAlimentaireEstimate) {
      sections.push(
        ...this.buildPensionAlimentaireSection(synthesis.pensionAlimentaireEstimate),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    if (synthesis.prestationCompensatoireEstimate) {
      sections.push(
        ...this.buildPrestationCompensatoireSection(synthesis.prestationCompensatoireEstimate),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    if (synthesis.liquidationCommunaute) {
      sections.push(
        ...this.buildLiquidationCommunauteSection(synthesis.liquidationCommunaute),
        { text: '', margin: [0, 0, 0, 16] }
      );
    }

    return sections;
  }

  private buildCompensationSection(est: CompensationEstimate): object[] {
    const typeLabels: Record<string, string> = {
      LICENCIEMENT: 'Licenciement',
      LICENCIEMENT_ECONOMIQUE: 'Licenciement économique',
      RUPTURE_CONVENTIONNELLE: 'Rupture conventionnelle',
    };
    const typeLabel = typeLabels[est.typeRupture] ?? est.typeRupture;

    const parts: string[] = [];
    if (est.ancienneteAnnees > 0) parts.push(`${est.ancienneteAnnees} an${est.ancienneteAnnees > 1 ? 's' : ''}`);
    if (est.ancienneteMois > 0)   parts.push(`${est.ancienneteMois} mois`);
    const ancienneteLabel = parts.length > 0 ? parts.join(' ') : "moins d'1 an";

    const fmt = (n: number) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(n);
    const plafondMin = fmt(Math.round(est.plafondMinMois * est.salaireReference));
    const plafondMax = fmt(Math.round(est.plafondMaxMois * est.salaireReference));

    const rows: object[] = [
      [
        { text: `Indemnité légale de ${typeLabel.toLowerCase()}`, bold: true, fontSize: 11, color: TEXT },
        { text: fmt(est.indemnite), bold: true, fontSize: 12, color: PRIMARY, alignment: 'right' }
      ],
      [
        { text: `Ancienneté : ${ancienneteLabel}`, fontSize: 10, color: TEXT_SECONDARY },
        { text: `Salaire de référence : ${fmt(est.salaireReference)}/mois`, fontSize: 10, color: TEXT_SECONDARY, alignment: 'right' }
      ],
      [
        { text: 'Plafond prud\'homal (barème Macron)', bold: true, fontSize: 11, color: TEXT, margin: [0, 8, 0, 0] },
        { text: `${est.plafondMinMois} — ${est.plafondMaxMois} mois`, bold: true, fontSize: 12, color: PRIMARY, alignment: 'right', margin: [0, 8, 0, 0] }
      ],
      [
        { text: `Soit : ${plafondMin} — ${plafondMax}`, fontSize: 10, color: TEXT_SECONDARY },
        { text: '', fontSize: 10 }
      ],
    ];

    const result: object[] = [
      {
        table: {
          widths: ['*', 'auto'],
          body: [[
            { text: 'Indemnités estimées', color: SURFACE, bold: true, fontSize: 13, margin: [12, 8, 0, 8] },
            { text: typeLabel, color: SURFACE, fontSize: 9, italics: true, margin: [0, 10, 12, 8], alignment: 'right' }
          ]]
        },
        layout: { fillColor: () => ACCENT, hLineColor: () => ACCENT, vLineColor: () => ACCENT },
        margin: [0, 0, 0, 8],
      },
      {
        table: { widths: ['*', 'auto'], body: rows },
        layout: 'noBorders',
        margin: [8, 0, 8, 0],
      },
    ];

    if (est.donneesPartielles) {
      result.push({
        text: '⚠ Données partielles — vérifier l\'ancienneté et le salaire de référence',
        fontSize: 9, color: ACCENT, italics: true, margin: [8, 6, 8, 0]
      });
    }

    result.push({
      text: 'Estimation indicative — données extraites par l\'IA, non certifiées',
      fontSize: 9, color: TEXT_SECONDARY, italics: true, margin: [8, 4, 8, 0]
    });

    return result;
  }

  private buildPensionAlimentaireSection(est: PensionAlimentaireEstimate): object[] {
    const fmt = (n: number) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(n);
    const gardeLabel = est.modeGarde === 'ALTERNEE' ? 'alternée' : 'exclusive';
    const baremeLabel = est.pays === 'BELGIQUE' ? 'CGKR Belgique' : 'UNAF France';

    const rows: object[] = [
      [
        { text: 'Fourchette mensuelle', bold: true, fontSize: 11, color: TEXT },
        { text: `${fmt(est.montantMin)} – ${fmt(est.montantMax)} / mois`, bold: true, fontSize: 12, color: PRIMARY, alignment: 'right' }
      ],
      [
        { text: `Revenus net débiteur : ${fmt(est.revenus)}/mois`, fontSize: 10, color: TEXT_SECONDARY },
        { text: `${est.nbEnfants} enfant(s) · garde ${gardeLabel}`, fontSize: 10, color: TEXT_SECONDARY, alignment: 'right' }
      ],
      [
        { text: `Barème : ${baremeLabel}`, fontSize: 10, color: TEXT_SECONDARY },
        { text: '', fontSize: 10 }
      ],
    ];

    const result: object[] = [
      {
        table: {
          widths: ['*', 'auto'],
          body: [[
            { text: 'Pension alimentaire indicative', color: SURFACE, bold: true, fontSize: 13, margin: [12, 8, 0, 8] },
            { text: `${est.nbEnfants} enfant(s)`, color: SURFACE, fontSize: 9, italics: true, margin: [0, 10, 12, 8], alignment: 'right' }
          ]]
        },
        layout: { fillColor: () => ACCENT, hLineColor: () => ACCENT, vLineColor: () => ACCENT },
        margin: [0, 0, 0, 8],
      },
      {
        table: { widths: ['*', 'auto'], body: rows },
        layout: 'noBorders',
        margin: [8, 0, 8, 0],
      },
    ];

    if (est.donneesPartielles) {
      result.push({
        text: '⚠ Données partielles — vérifier les revenus et le nombre d\'enfants',
        fontSize: 9, color: ACCENT, italics: true, margin: [8, 6, 8, 0]
      });
    }

    result.push({
      text: 'Estimation indicative — ne constitue pas un avis juridique',
      fontSize: 9, color: TEXT_SECONDARY, italics: true, margin: [8, 4, 8, 0]
    });

    return result;
  }

  private buildPrestationCompensatoireSection(est: PrestationCompensatoireEstimate): object[] {
    const fmt = (n: number) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(n);
    const baremeLabel = est.pays === 'BELGIQUE' ? 'Belgique (coeff. 0.25)' : 'France art. 271 Cciv (coeff. 0.30)';

    const rows: object[] = [
      [
        { text: 'Fourchette de capital', bold: true, fontSize: 11, color: TEXT },
        { text: `${fmt(est.montantMin)} – ${fmt(est.montantMax)}`, bold: true, fontSize: 12, color: PRIMARY, alignment: 'right' }
      ],
      [
        { text: `Écart de revenus : ${fmt(est.ecartRevenus)}/mois`, fontSize: 10, color: TEXT_SECONDARY },
        { text: `Durée mariage : ${est.dureeMarriage} an${est.dureeMarriage > 1 ? 's' : ''}`, fontSize: 10, color: TEXT_SECONDARY, alignment: 'right' }
      ],
      [
        { text: `Barème : ${baremeLabel}`, fontSize: 10, color: TEXT_SECONDARY },
        { text: '', fontSize: 10 }
      ],
    ];

    const result: object[] = [
      {
        table: {
          widths: ['*', 'auto'],
          body: [[
            { text: 'Prestation compensatoire indicative', color: SURFACE, bold: true, fontSize: 13, margin: [12, 8, 0, 8] },
            { text: 'Capital', color: SURFACE, fontSize: 9, italics: true, margin: [0, 10, 12, 8], alignment: 'right' }
          ]]
        },
        layout: { fillColor: () => ACCENT, hLineColor: () => ACCENT, vLineColor: () => ACCENT },
        margin: [0, 0, 0, 8],
      },
      {
        table: { widths: ['*', 'auto'], body: rows },
        layout: 'noBorders',
        margin: [8, 0, 8, 0],
      },
    ];

    if (est.donneesPartielles) {
      result.push({
        text: '⚠ Données partielles — vérifier les revenus et la durée du mariage',
        fontSize: 9, color: ACCENT, italics: true, margin: [8, 6, 8, 0]
      });
    }

    result.push({
      text: 'Estimation indicative — fixée discrétionnairement par le juge (art. 271 Cciv)',
      fontSize: 9, color: TEXT_SECONDARY, italics: true, margin: [8, 4, 8, 0]
    });

    return result;
  }

  private buildLiquidationCommunauteSection(liq: LiquidationCommunaute): object[] {
    const fmt = (n: number) => new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(n);
    const regimeLabels: Record<string, string> = {
      COMMUNAUTE_LEGALE:     'Communauté légale',
      SEPARATION_BIENS:      'Séparation de biens',
      PARTICIPATION_ACQUETS: 'Participation aux acquêts',
    };
    const regimeLabel = liq.regimeMatrimonial ? (regimeLabels[liq.regimeMatrimonial] ?? liq.regimeMatrimonial) : 'Non détecté';

    const sections = [
      { titre: 'Actif commun',          items: liq.actifCommun },
      { titre: 'Biens propres époux A', items: liq.biensPropresEpouxA },
      { titre: 'Biens propres époux B', items: liq.biensPropresEpouxB },
      { titre: 'Passif commun',         items: liq.passifCommun },
    ];

    const body: object[][] = [];
    for (const section of sections) {
      body.push([
        { text: section.titre, bold: true, fontSize: 10, color: PRIMARY, colSpan: 2, margin: [0, 6, 0, 2] },
        {}
      ]);
      if (section.items.length === 0) {
        body.push([
          { text: 'Aucun bien détecté', fontSize: 9, color: TEXT_SECONDARY, italics: true },
          { text: '', fontSize: 9 }
        ]);
      } else {
        for (const item of section.items) {
          body.push([
            { text: item.libelle, fontSize: 10, color: TEXT },
            { text: item.valeur != null ? fmt(item.valeur) : '—', fontSize: 10, color: TEXT_SECONDARY, alignment: 'right' }
          ]);
        }
      }
    }

    return [
      {
        table: {
          widths: ['*', 'auto'],
          body: [[
            { text: 'Liquidation de communauté', color: SURFACE, bold: true, fontSize: 13, margin: [12, 8, 0, 8] },
            { text: regimeLabel, color: SURFACE, fontSize: 9, italics: true, margin: [0, 10, 12, 8], alignment: 'right' }
          ]]
        },
        layout: { fillColor: () => ACCENT, hLineColor: () => ACCENT, vLineColor: () => ACCENT },
        margin: [0, 0, 0, 8],
      },
      {
        table: { widths: ['*', 'auto'], body },
        layout: {
          hLineWidth: () => 0.3,
          vLineWidth: () => 0,
          hLineColor: () => DIVIDER,
        },
        margin: [8, 0, 8, 0],
      },
      {
        text: 'Inventaire extrait par l\'IA — à vérifier et compléter avec les actes notariaux',
        fontSize: 9, color: TEXT_SECONDARY, italics: true, margin: [8, 6, 8, 0]
      },
    ];
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

  /** F-146 SF-146-03 : variante qui expose la source (sourceRef ou legacy) en ligne secondaire. */
  private buildNumberedListFromItems(items: AnalysisItem[], bulletColor: string): object[] {
    return items.map((item, i) => {
      const src = formatSourceRef(item);
      const textStack: object[] = [{ text: item.texte, fontSize: 10 }];
      if (src) textStack.push({ text: src, fontSize: 8, italics: true, color: TEXT_SECONDARY, margin: [0, 2, 0, 0] });
      return {
        columns: [
          { text: `${i + 1}.`, width: 24, bold: true, color: bulletColor, fontSize: 10, margin: [0, 3, 0, 3] },
          { stack: textStack, margin: [0, 3, 0, 3] }
        ],
        margin: [8, 0, 0, 4],
      };
    });
  }

  /** F-146 SF-146-03 : rend la liste de risques avec source optionnelle en ligne secondaire. */
  private buildRisquesListFromItems(items: AnalysisItem[]): object[] {
    return items.map(item => {
      const src = formatSourceRef(item);
      const stack: object[] = [{ text: item.texte, fontSize: 10, fillColor: ERROR_BG }];
      if (src) stack.push({ text: src, fontSize: 8, italics: true, color: TEXT_SECONDARY, fillColor: ERROR_BG, margin: [0, 2, 0, 0] });
      return {
        table: {
          widths: [16, '*'],
          body: [[
            { text: '▲', color: ERROR, bold: true, fontSize: 10, fillColor: ERROR_BG, margin: [8, 5, 4, 5], border: [false, false, false, false] },
            { stack, margin: [4, 5, 8, 5], border: [false, false, false, false] }
          ]]
        },
        layout: 'noBorders',
        margin: [0, 0, 0, 4],
      };
    });
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

  exportChecklist(caseFile: CaseFile, checks: ProcedureCheck[]): void {
    import('pdfmake/build/pdfmake').then(pdfMakeModule => {
      import('pdfmake/build/vfs_fonts').then(vfsFontsModule => {
        const pdfMake = (pdfMakeModule.default || pdfMakeModule) as any;
        const vfsFonts = (vfsFontsModule.default || vfsFontsModule) as any;
        pdfMake.vfs = vfsFonts.pdfMake ? vfsFonts.pdfMake.vfs : vfsFonts.vfs;

        const docDefinition = this.buildChecklistDocument(caseFile, checks) as TDocumentDefinitions;
        const fileName = this.buildChecklistFileName(caseFile.title);
        pdfMake.createPdf(docDefinition).download(fileName);
      });
    });
  }

  buildChecklistDocument(caseFile: CaseFile, checks: ProcedureCheck[]): object {
    const exportDate = new Date().toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric'
    });
    const sorted = [...checks].sort((a, b) => a.ordre - b.ordre);
    const verified = sorted.filter(c => c.statut === 'VERIFIED').length;
    const nonCompliant = sorted.filter(c => c.statut === 'NON_COMPLIANT').length;
    const toCheck = sorted.filter(c => c.statut === 'TO_CHECK').length;

    return {
      pageSize: 'A4',
      pageMargins: [48, 64, 48, 64],
      content: [
        { text: '', margin: [0, 40, 0, 0] },
        {
          image: LEGALCASE_LOGO_BASE64,
          width: 180,
          alignment: 'center',
          margin: [0, 0, 0, 24],
        },
        {
          canvas: [{ type: 'line', x1: 80, y1: 0, x2: 420, y2: 0, lineWidth: 2, lineColor: ACCENT }],
          margin: [0, 0, 0, 20],
        },
        {
          text: caseFile.title || 'Dossier',
          style: 'coverTitle',
          alignment: 'center',
          margin: [0, 0, 0, 8],
        },
        {
          text: 'Checklist procédurale',
          fontSize: 13,
          color: TEXT_SECONDARY,
          alignment: 'center',
          margin: [0, 0, 0, 4],
        },
        {
          text: `Exportée le ${exportDate}`,
          fontSize: 10,
          color: TEXT_SECONDARY,
          alignment: 'center',
          margin: [0, 0, 0, 24],
        },
        {
          table: {
            widths: ['*', '*', '*'],
            body: [[
              { text: `${verified} vérifié(s)`, alignment: 'center', bold: true, color: '#27AE60', fontSize: 11, margin: [0, 6, 0, 6] },
              { text: `${nonCompliant} non conforme(s)`, alignment: 'center', bold: true, color: ERROR, fontSize: 11, margin: [0, 6, 0, 6] },
              { text: `${toCheck} à vérifier`, alignment: 'center', bold: true, color: ACCENT, fontSize: 11, margin: [0, 6, 0, 6] },
            ]]
          },
          layout: {
            hLineWidth: () => 0.5,
            vLineWidth: () => 0.5,
            hLineColor: () => DIVIDER,
            vLineColor: () => DIVIDER,
          },
          margin: [0, 0, 0, 24],
        },
        {
          canvas: [{ type: 'line', x1: 0, y1: 0, x2: 500, y2: 0, lineWidth: 0.5, lineColor: DIVIDER }],
          margin: [0, 0, 0, 16],
        },
        ...this.buildChecklistItems(sorted),
      ],
      footer: (currentPage: number, pageCount: number) => this.buildFooter(currentPage, pageCount),
      defaultStyle: { font: 'Roboto', fontSize: 10, color: TEXT, lineHeight: 1.4 },
      styles: this.buildStyles(),
    };
  }

  private buildChecklistItems(checks: ProcedureCheck[]): object[] {
    const STATUS_CONFIG: Record<string, { label: string; color: string; bg: string }> = {
      VERIFIED:      { label: '✔ Vérifié',       color: '#27AE60', bg: '#F0FBF4' },
      NON_COMPLIANT: { label: '✖ Non conforme',  color: ERROR,     bg: ERROR_BG  },
      TO_CHECK:      { label: '? À vérifier',    color: ACCENT,    bg: '#FDF6EC' },
    };

    return checks.map(check => {
      const cfg = STATUS_CONFIG[check.statut] ?? STATUS_CONFIG['TO_CHECK'];
      const descriptionCell: object[] = [
        { text: `${check.ordre}. ${check.description}`, fontSize: 10 },
      ];
      if (check.raison) {
        descriptionCell.push({
          text: `Raison IA : ${check.raison}`,
          fontSize: 9,
          italics: true,
          color: TEXT_SECONDARY,
          margin: [0, 4, 0, 0],
        } as object);
      }
      return {
        table: {
          widths: [110, '*'],
          body: [[
            {
              text: cfg.label,
              bold: true,
              color: cfg.color,
              fillColor: cfg.bg,
              fontSize: 9,
              margin: [8, 6, 8, 6],
              border: [false, false, false, false],
            },
            {
              stack: descriptionCell,
              fillColor: cfg.bg,
              margin: [4, 6, 8, 6],
              border: [false, false, false, false],
            },
          ]]
        },
        layout: 'noBorders',
        margin: [0, 0, 0, 4],
      };
    });
  }

  exportPrudhomeFiche(fiche: PrudhomeFiche, caseFileTitle: string): void {
    import('pdfmake/build/pdfmake').then(pdfMakeModule => {
      import('pdfmake/build/vfs_fonts').then(vfsFontsModule => {
        const pdfMake = (pdfMakeModule.default || pdfMakeModule) as any;
        const vfsFonts = (vfsFontsModule.default || vfsFontsModule) as any;
        pdfMake.vfs = vfsFonts.pdfMake ? vfsFonts.pdfMake.vfs : vfsFonts.vfs;

        const docDefinition = this.buildPrudhomeFicheDocument(fiche, caseFileTitle) as TDocumentDefinitions;
        const fileName = this.buildPrudhomeFicheFileName(caseFileTitle);
        pdfMake.createPdf(docDefinition).download(fileName);
      });
    });
  }

  buildPrudhomeFicheDocument(fiche: PrudhomeFiche, caseFileTitle: string): object {
    const exportDate = new Date().toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric'
    });
    const fmt = (n: number | null) =>
      n != null
        ? new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(n)
        : '—';

    const content: object[] = [
      { text: '', margin: [0, 40, 0, 0] },
      {
        image: LEGALCASE_LOGO_BASE64,
        width: 180,
        alignment: 'center',
        margin: [0, 0, 0, 24],
      },
      {
        canvas: [{ type: 'line', x1: 80, y1: 0, x2: 420, y2: 0, lineWidth: 2, lineColor: ACCENT }],
        margin: [0, 0, 0, 20],
      },
      {
        text: caseFileTitle || 'Dossier',
        style: 'coverTitle',
        alignment: 'center',
        margin: [0, 0, 0, 8],
      },
      {
        text: 'Fiche prud\'homale',
        fontSize: 13,
        color: TEXT_SECONDARY,
        alignment: 'center',
        margin: [0, 0, 0, 4],
      },
      {
        text: `Générée le ${exportDate}`,
        fontSize: 10,
        color: TEXT_SECONDARY,
        alignment: 'center',
        margin: [0, 0, 0, 4],
      },
      {
        text: 'Document pré-rempli à environ 60-70% — À vérifier avant tout usage',
        fontSize: 9,
        color: ACCENT,
        italics: true,
        alignment: 'center',
        margin: [0, 0, 0, 24],
      },
      {
        canvas: [{ type: 'line', x1: 0, y1: 0, x2: 500, y2: 0, lineWidth: 0.5, lineColor: DIVIDER }],
        margin: [0, 0, 0, 16],
      },

      // Demandeur
      this.buildPrudhomeSectionHeader('Demandeur'),
      {
        table: {
          widths: [120, '*'],
          body: [
            [{ text: 'Nom', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.demandeur?.nom || '—', fontSize: 10 }],
            [{ text: 'Prénom', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.demandeur?.prenom || '—', fontSize: 10 }],
            [{ text: 'Adresse', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.demandeur?.adresse || '—', fontSize: 10 }],
            [{ text: 'Téléphone', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.demandeur?.telephone || '—', fontSize: 10 }],
            [{ text: 'Email', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.demandeur?.email || '—', fontSize: 10 }],
            [{ text: 'Profession', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.demandeur?.profession || '—', fontSize: 10 }],
          ]
        },
        layout: { hLineWidth: () => 0.5, vLineWidth: () => 0, hLineColor: () => DIVIDER },
        margin: [0, 0, 0, 16],
      },

      // Défendeur
      this.buildPrudhomeSectionHeader('Défendeur'),
      {
        table: {
          widths: [120, '*'],
          body: [
            [{ text: 'Nom / Raison sociale', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.defendeur?.nom || '—', fontSize: 10 }],
            [{ text: 'Adresse', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.defendeur?.adresse || '—', fontSize: 10 }],
            [{ text: 'SIRET', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.defendeur?.siret || '—', fontSize: 10 }],
            [{ text: 'Représentant légal', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.defendeur?.representant || '—', fontSize: 10 }],
          ]
        },
        layout: { hLineWidth: () => 0.5, vLineWidth: () => 0, hLineColor: () => DIVIDER },
        margin: [0, 0, 0, 16],
      },

      // Demandes
      this.buildPrudhomeSectionHeader('Demandes chiffrées'),
      (fiche.demandes?.length ?? 0) > 0
        ? {
            table: {
              widths: ['*', 100],
              headerRows: 1,
              body: [
                [
                  { text: 'Intitulé', bold: true, color: SURFACE, fillColor: PRIMARY, fontSize: 9, margin: [8, 5, 4, 5] },
                  { text: 'Montant', bold: true, color: SURFACE, fillColor: PRIMARY, fontSize: 9, margin: [4, 5, 8, 5], alignment: 'right' },
                ],
                ...(fiche.demandes ?? []).map((d, i) => [
                  { text: d.label, fontSize: 10, fillColor: i % 2 === 0 ? BG : SURFACE, margin: [8, 5, 4, 5] },
                  { text: fmt(d.montant), fontSize: 10, fillColor: i % 2 === 0 ? BG : SURFACE, margin: [4, 5, 8, 5], alignment: 'right' },
                ]),
              ]
            },
            layout: { hLineWidth: () => 0.5, vLineWidth: () => 0, hLineColor: () => DIVIDER },
            margin: [0, 0, 0, 16],
          }
        : { text: 'Aucune demande renseignée.', fontSize: 10, color: TEXT_SECONDARY, italics: true, margin: [0, 0, 0, 16] },

      // Faits
      this.buildPrudhomeSectionHeader('Exposé des faits'),
      {
        text: fiche.faitsTexte || '—',
        fontSize: 10,
        margin: [0, 0, 0, 16],
      },

      // Moyens de droit
      this.buildPrudhomeSectionHeader('Moyens de droit'),
      {
        text: fiche.moyensDroitTexte || '—',
        fontSize: 10,
        margin: [0, 0, 0, 16],
      },
    ];

    // Bordereau de pièces
    if ((fiche.piecesList?.length ?? 0) > 0) {
      content.push(
        this.buildPrudhomeSectionHeader('Bordereau de pièces'),
        ...( fiche.piecesList ?? []).map(p => ({
          text: `${p.numero}. ${p.nom}`,
          fontSize: 10,
          margin: [8, 2, 0, 2],
        }))
      );
    }

    return {
      pageSize: 'A4',
      pageMargins: [48, 64, 48, 64],
      content,
      footer: (currentPage: number, pageCount: number) => this.buildFooter(currentPage, pageCount),
      defaultStyle: { font: 'Roboto', fontSize: 10, color: TEXT, lineHeight: 1.4 },
      styles: this.buildStyles(),
    };
  }

  private buildPrudhomeSectionHeader(label: string): object {
    return {
      table: {
        widths: ['*'],
        body: [[
          { text: label, color: SURFACE, bold: true, fontSize: 12, margin: [12, 7, 0, 7] }
        ]]
      },
      layout: { fillColor: () => PRIMARY, hLineColor: () => PRIMARY, vLineColor: () => PRIMARY },
      margin: [0, 0, 0, 8],
    };
  }

  buildPrudhomeFicheFileName(title: string): string {
    const slug = (title || 'dossier')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40);
    const date = new Date().toISOString().slice(0, 10);
    return `fiche-prudhomale-${slug}-${date}.pdf`;
  }

  buildChecklistFileName(title: string): string {
    const slug = (title || 'dossier')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40);
    const date = new Date().toISOString().slice(0, 10);
    return `checklist-${slug}-${date}.pdf`;
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

  exportImmigrationChecklist(checklist: ImmigrationChecklist, caseFileTitle: string): void {
    import('pdfmake/build/pdfmake').then(pdfMakeModule => {
      import('pdfmake/build/vfs_fonts').then(vfsFontsModule => {
        const pdfMake = (pdfMakeModule.default || pdfMakeModule) as any;
        const vfsFonts = (vfsFontsModule.default || vfsFontsModule) as any;
        pdfMake.vfs = vfsFonts.pdfMake ? vfsFonts.pdfMake.vfs : vfsFonts.vfs;
        const docDefinition = this.buildImmigrationChecklistDocument(checklist, caseFileTitle) as TDocumentDefinitions;
        const fileName = this.buildImmigrationChecklistFileName(caseFileTitle);
        pdfMake.createPdf(docDefinition).download(fileName);
      });
    });
  }

  buildImmigrationChecklistDocument(checklist: ImmigrationChecklist, caseFileTitle: string): object {
    const exportDate = new Date().toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric'
    });
    const present = checklist.pieces.filter(p => p.statut === 'PRESENT').length;
    const absent  = checklist.pieces.filter(p => p.statut === 'ABSENT').length;
    const inconnu = checklist.pieces.filter(p => p.statut === 'INCONNU').length;

    const STATUT_CONFIG: Record<string, { label: string; color: string }> = {
      PRESENT: { label: '✔ Présente', color: '#27AE60' },
      ABSENT:  { label: '✖ Absente',  color: ERROR },
      INCONNU: { label: '? Inconnue', color: TEXT_SECONDARY },
    };

    const pieceRows: object[] = checklist.pieces.length === 0
      ? [{ text: 'Aucune pièce', italics: true, color: TEXT_SECONDARY, margin: [0, 4, 0, 4] }]
      : checklist.pieces.map(p => {
          const cfg = STATUT_CONFIG[p.statut] ?? STATUT_CONFIG['INCONNU'];
          return {
            columns: [
              { text: p.label, width: '*', fontSize: 10, color: TEXT, margin: [0, 3, 0, 3] },
              { text: cfg.label, width: 90, fontSize: 10, bold: true, color: cfg.color, alignment: 'right', margin: [0, 3, 0, 3] },
            ],
            margin: [0, 0, 0, 2],
          };
        });

    const titreLabelMap: Record<string, string> = {
      VISA_ETUDIANT: 'Visa étudiant',
      TITRE_SALARIE: 'Titre salarié',
      REGROUPEMENT_FAMILIAL: 'Regroupement familial',
      NATURALISATION: 'Naturalisation',
    };
    const countryLabelMap: Record<string, string> = {
      FRANCE: 'France',
      BELGIQUE: 'Belgique',
    };
    const titreLabel = titreLabelMap[checklist.titreType] ?? checklist.titreType;
    const countryLabel = countryLabelMap[checklist.country] ?? checklist.country;

    return {
      pageSize: 'A4',
      pageMargins: [48, 64, 48, 64],
      content: [
        { text: '', margin: [0, 40, 0, 0] },
        {
          image: LEGALCASE_LOGO_BASE64,
          width: 180,
          alignment: 'center',
          margin: [0, 0, 0, 24],
        },
        {
          canvas: [{ type: 'line', x1: 80, y1: 0, x2: 420, y2: 0, lineWidth: 2, lineColor: ACCENT }],
          margin: [0, 0, 0, 20],
        },
        {
          text: caseFileTitle || 'Dossier',
          style: 'coverTitle',
          alignment: 'center',
          margin: [0, 0, 0, 8],
        },
        {
          text: 'Checklist pièces immigration',
          fontSize: 13,
          color: TEXT_SECONDARY,
          alignment: 'center',
          margin: [0, 0, 0, 4],
        },
        {
          text: `${titreLabel} — ${countryLabel}`,
          fontSize: 11,
          color: PRIMARY,
          bold: true,
          alignment: 'center',
          margin: [0, 0, 0, 4],
        },
        {
          text: `Exportée le ${exportDate}`,
          fontSize: 10,
          color: TEXT_SECONDARY,
          alignment: 'center',
          margin: [0, 0, 0, 24],
        },
        {
          table: {
            widths: ['*', '*', '*'],
            body: [[
              { text: `${present} présente(s)`, alignment: 'center', bold: true, color: '#27AE60', fontSize: 11, margin: [0, 6, 0, 6] },
              { text: `${absent} absente(s)`,   alignment: 'center', bold: true, color: ERROR,     fontSize: 11, margin: [0, 6, 0, 6] },
              { text: `${inconnu} inconnue(s)`, alignment: 'center', bold: true, color: TEXT_SECONDARY, fontSize: 11, margin: [0, 6, 0, 6] },
            ]]
          },
          layout: {
            hLineWidth: () => 0.5,
            vLineWidth: () => 0.5,
            hLineColor: () => DIVIDER,
            vLineColor: () => DIVIDER,
          },
          margin: [0, 0, 0, 24],
        },
        {
          canvas: [{ type: 'line', x1: 0, y1: 0, x2: 500, y2: 0, lineWidth: 0.5, lineColor: DIVIDER }],
          margin: [0, 0, 0, 16],
        },
        ...pieceRows,
      ],
      footer: (currentPage: number, pageCount: number) => this.buildFooter(currentPage, pageCount),
      defaultStyle: { font: 'Roboto', fontSize: 10, color: TEXT, lineHeight: 1.4 },
      styles: this.buildStyles(),
    };
  }

  buildImmigrationChecklistFileName(title: string): string {
    const slug = (title || 'dossier')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40);
    const date = new Date().toISOString().slice(0, 10);
    return `checklist-pieces-${slug}-${date}.pdf`;
  }

  // ── Export requête tribunal du travail belge ──

  exportTribunalTravailFiche(fiche: TribunalTravailFiche, caseFileTitle: string): void {
    import('pdfmake/build/pdfmake').then(pdfMakeModule => {
      import('pdfmake/build/vfs_fonts').then(vfsFontsModule => {
        const pdfMake = (pdfMakeModule.default || pdfMakeModule) as any;
        const vfsFonts = (vfsFontsModule.default || vfsFontsModule) as any;
        pdfMake.vfs = vfsFonts.pdfMake ? vfsFonts.pdfMake.vfs : vfsFonts.vfs;

        const docDefinition = this.buildTribunalTravailDocument(fiche, caseFileTitle) as TDocumentDefinitions;
        const fileName = this.buildTribunalTravailFileName(caseFileTitle);
        pdfMake.createPdf(docDefinition).download(fileName);
      });
    });
  }

  buildTribunalTravailDocument(fiche: TribunalTravailFiche, caseFileTitle: string): object {
    const exportDate = new Date().toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric'
    });
    const fmt = (n: number | null) =>
      n != null
        ? new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(n)
        : '—';
    const langueLabel = (l: string | null) => {
      if (l === 'NL') return 'Néerlandais';
      if (l === 'DE') return 'Allemand';
      return 'Français';
    };

    const content: object[] = [
      { text: '', margin: [0, 40, 0, 0] },
      {
        image: LEGALCASE_LOGO_BASE64,
        width: 180,
        alignment: 'center',
        margin: [0, 0, 0, 24],
      },
      {
        canvas: [{ type: 'line', x1: 80, y1: 0, x2: 420, y2: 0, lineWidth: 2, lineColor: ACCENT }],
        margin: [0, 0, 0, 20],
      },
      {
        text: caseFileTitle || 'Dossier',
        style: 'coverTitle',
        alignment: 'center',
        margin: [0, 0, 0, 8],
      },
      {
        text: 'Requête contradictoire — Tribunal du travail',
        fontSize: 13,
        color: TEXT_SECONDARY,
        alignment: 'center',
        margin: [0, 0, 0, 4],
      },
      {
        text: `Art. 702/704 Code judiciaire belge`,
        fontSize: 10,
        color: TEXT_SECONDARY,
        alignment: 'center',
        margin: [0, 0, 0, 4],
      },
      {
        text: `Générée le ${exportDate}`,
        fontSize: 10,
        color: TEXT_SECONDARY,
        alignment: 'center',
        margin: [0, 0, 0, 4],
      },
      {
        text: 'Document pré-rempli à environ 60-70% — À vérifier avant tout usage',
        fontSize: 9,
        color: ACCENT,
        italics: true,
        alignment: 'center',
        margin: [0, 0, 0, 24],
      },
      {
        canvas: [{ type: 'line', x1: 0, y1: 0, x2: 500, y2: 0, lineWidth: 0.5, lineColor: DIVIDER }],
        margin: [0, 0, 0, 16],
      },

      // Requérant
      this.buildPrudhomeSectionHeader('Requérant'),
      {
        table: {
          widths: [140, '*'],
          body: [
            [{ text: 'Nom', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.requerant?.nom || '—', fontSize: 10 }],
            [{ text: 'Prénom', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.requerant?.prenom || '—', fontSize: 10 }],
            [{ text: 'Domicile', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.requerant?.domicile || '—', fontSize: 10 }],
            [{ text: 'Registre national', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.requerant?.registreNational || '—', fontSize: 10 }],
          ]
        },
        layout: { hLineWidth: () => 0.5, vLineWidth: () => 0, hLineColor: () => DIVIDER },
        margin: [0, 0, 0, 16],
      },

      // Défendeur
      this.buildPrudhomeSectionHeader('Défendeur'),
      {
        table: {
          widths: [140, '*'],
          body: [
            [{ text: 'Nom / Raison sociale', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.defendeur?.nom || '—', fontSize: 10 }],
            [{ text: 'Siège social', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.defendeur?.siegeSocial || '—', fontSize: 10 }],
            [{ text: 'N° BCE', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.defendeur?.numeroBce || '—', fontSize: 10 }],
            [{ text: 'Représentant légal', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.defendeur?.representant || '—', fontSize: 10 }],
          ]
        },
        layout: { hLineWidth: () => 0.5, vLineWidth: () => 0, hLineColor: () => DIVIDER },
        margin: [0, 0, 0, 16],
      },

      // Procédure
      this.buildPrudhomeSectionHeader('Procédure'),
      {
        table: {
          widths: [140, '*'],
          body: [
            [{ text: 'Tribunal du travail', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.procedureInfo?.tribunal || '—', fontSize: 10 }],
            [{ text: 'Division', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.procedureInfo?.division || '—', fontSize: 10 }],
            [{ text: 'Langue', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: langueLabel(fiche.procedureInfo?.langue), fontSize: 10 }],
            [{ text: 'Commission paritaire', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.procedureInfo?.commissionParitaire || '—', fontSize: 10 }],
          ]
        },
        layout: { hLineWidth: () => 0.5, vLineWidth: () => 0, hLineColor: () => DIVIDER },
        margin: [0, 0, 0, 16],
      },

      // Contrat
      this.buildPrudhomeSectionHeader('Contrat de travail'),
      {
        table: {
          widths: [140, '*'],
          body: [
            [{ text: 'Type de contrat', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.contratInfo?.typeContrat === 'OUVRIER' ? 'Ouvrier' : 'Employé', fontSize: 10 }],
            [{ text: 'Date de début', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.contratInfo?.dateDebut || '—', fontSize: 10 }],
            [{ text: 'Date de fin', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.contratInfo?.dateFin || '—', fontSize: 10 }],
            [{ text: 'Motif de rupture', bold: true, fontSize: 9, color: TEXT_SECONDARY }, { text: fiche.contratInfo?.motifRupture || '—', fontSize: 10 }],
          ]
        },
        layout: { hLineWidth: () => 0.5, vLineWidth: () => 0, hLineColor: () => DIVIDER },
        margin: [0, 0, 0, 16],
      },

      // Demandes
      this.buildPrudhomeSectionHeader('Demandes chiffrées'),
      (fiche.demandes?.length ?? 0) > 0
        ? {
            table: {
              widths: ['*', 100],
              headerRows: 1,
              body: [
                [
                  { text: 'Intitulé', bold: true, color: SURFACE, fillColor: PRIMARY, fontSize: 9, margin: [8, 5, 4, 5] },
                  { text: 'Montant', bold: true, color: SURFACE, fillColor: PRIMARY, fontSize: 9, margin: [4, 5, 8, 5], alignment: 'right' },
                ],
                ...(fiche.demandes ?? []).map((d, i) => [
                  { text: d.label, fontSize: 10, fillColor: i % 2 === 0 ? BG : SURFACE, margin: [8, 5, 4, 5] },
                  { text: fmt(d.montant), fontSize: 10, fillColor: i % 2 === 0 ? BG : SURFACE, margin: [4, 5, 8, 5], alignment: 'right' },
                ]),
              ]
            },
            layout: { hLineWidth: () => 0.5, vLineWidth: () => 0, hLineColor: () => DIVIDER },
            margin: [0, 0, 0, 16],
          }
        : { text: 'Aucune demande renseignée.', fontSize: 10, color: TEXT_SECONDARY, italics: true, margin: [0, 0, 0, 16] },

      // Exposé des moyens
      this.buildPrudhomeSectionHeader('Exposé sommaire des moyens'),
      {
        text: fiche.exposeDesMoyens || '—',
        fontSize: 10,
        margin: [0, 0, 0, 16],
      },
    ];

    // Inventaire des pièces
    if ((fiche.piecesList?.length ?? 0) > 0) {
      content.push(
        this.buildPrudhomeSectionHeader('Inventaire des pièces'),
        ...(fiche.piecesList ?? []).map(p => ({
          text: `${p.numero}. ${p.nom}`,
          fontSize: 10,
          margin: [8, 2, 0, 2],
        }))
      );
    }

    return {
      pageSize: 'A4',
      pageMargins: [48, 64, 48, 64],
      content,
      footer: (currentPage: number, pageCount: number) => this.buildFooter(currentPage, pageCount),
      defaultStyle: { font: 'Roboto', fontSize: 10, color: TEXT, lineHeight: 1.4 },
      styles: this.buildStyles(),
    };
  }

  buildTribunalTravailFileName(title: string): string {
    const slug = (title || 'dossier')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40);
    const date = new Date().toISOString().slice(0, 10);
    return `requete-tribunal-travail-${slug}-${date}.pdf`;
  }

  // ── Recours Immigration ──────────────────────────────────────────

  exportRecoursImmigration(recours: RecoursResponse, caseFileTitle: string): void {
    import('pdfmake/build/pdfmake').then(pdfMakeModule => {
      import('pdfmake/build/vfs_fonts').then(vfsFontsModule => {
        const pdfMake = (pdfMakeModule.default || pdfMakeModule) as any;
        const vfsFonts = (vfsFontsModule.default || vfsFontsModule) as any;
        pdfMake.vfs = vfsFonts.pdfMake ? vfsFonts.pdfMake.vfs : vfsFonts.vfs;
        const docDefinition = this.buildRecoursDocument(recours, caseFileTitle) as TDocumentDefinitions;
        const fileName = this.buildRecoursFileName(caseFileTitle);
        pdfMake.createPdf(docDefinition).download(fileName);
      });
    });
  }

  buildRecoursDocument(recours: RecoursResponse, caseFileTitle: string): object {
    const exportDate = new Date().toLocaleDateString('fr-FR', {
      day: '2-digit', month: 'long', year: 'numeric'
    });
    const doc = recours.document;

    const content: any[] = [
      { image: LEGALCASE_LOGO_BASE64, width: 120, margin: [0, 0, 0, 16] },
      { text: recours.recoursLabel, style: 'title' },
      { text: `Dossier : ${caseFileTitle}`, style: 'subtitle' },
      { text: `Exporté le ${exportDate}`, style: 'date' },
      { canvas: [{ type: 'line', x1: 0, y1: 0, x2: 515, y2: 0, lineWidth: 1, lineColor: ACCENT }], margin: [0, 8, 0, 16] },
    ];

    if (recours.dateLimiteDepassee && recours.avertissement) {
      content.push({
        text: recours.avertissement,
        style: 'warning',
        margin: [0, 0, 0, 12]
      });
    }

    content.push(
      { text: `Date limite de dépôt : ${recours.dateLimite}`, bold: true, margin: [0, 0, 0, 16], color: PRIMARY },
    );

    const sections = [
      { title: 'EN-TÊTE', body: doc.enTete },
      { title: 'OBJET', body: doc.objetDemande },
      { title: 'VISA DES TEXTES', body: doc.visaTextes },
      { title: 'EXPOSÉ DES FAITS', body: doc.exposeFaits },
      { title: 'MOYENS DE DROIT', body: doc.moyensDroit },
      { title: 'CONCLUSIONS', body: doc.conclusions },
    ];

    for (const section of sections) {
      content.push(
        { text: section.title, style: 'sectionTitle' },
        { text: section.body, style: 'body', margin: [0, 0, 0, 12] }
      );
    }

    content.push({ text: 'PIÈCES À JOINDRE', style: 'sectionTitle' });
    content.push({
      ul: doc.piecesJointes.map((p: string) => ({ text: p, style: 'body' })),
      margin: [0, 0, 0, 12]
    });

    return {
      content,
      styles: {
        title: { fontSize: 18, bold: true, color: PRIMARY, margin: [0, 0, 0, 4] },
        subtitle: { fontSize: 12, color: TEXT, margin: [0, 0, 0, 4] },
        date: { fontSize: 10, color: '#6B7A8D', margin: [0, 0, 0, 8] },
        sectionTitle: { fontSize: 11, bold: true, color: ACCENT, margin: [0, 8, 0, 4] },
        body: { fontSize: 10, color: TEXT, lineHeight: 1.5 },
        warning: { fontSize: 10, color: ERROR, bold: true, background: ERROR_BG },
      },
      defaultStyle: { font: 'Roboto' },
      pageMargins: [40, 40, 40, 40],
    };
  }

  buildRecoursFileName(caseFileTitle: string): string {
    const slug = caseFileTitle
      .toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40);
    const date = new Date().toISOString().slice(0, 10);
    return `recours-immigration-${slug}-${date}.pdf`;
  }
}
