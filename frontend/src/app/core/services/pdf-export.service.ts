import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
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
import { RetainedPisteAlignment } from '../models/retained-piste-alignment.model';
import { ProcedureCheckAlignment } from '../models/procedure-check-alignment.model';
import { PieceManquanteAlignment } from '../models/piece-manquante-alignment.model';
import { RisqueAlignment } from '../models/risque-alignment.model';
import { AiQuestionAlignment } from '../models/ai-question-alignment.model';

const PRIMARY = '#1A3A5C';
const ACCENT = '#C9973A';
const ERROR = '#C0392B';
const ERROR_BG = '#FFEBEE';
const TEXT = '#1C2B3A';
const TEXT_SECONDARY = '#6B7A8D';
const DIVIDER = '#E0E4EA';
const SURFACE = '#FFFFFF';
const BG = '#F5F6FA';
// F-194 SF-194-03 — fond or léger (teinte claire d'ACCENT) pour le bandeau
// titre proéminent de la section « 📎 Pièces à demander au client ».
const PIECES_BG = '#FBF4E2';
// F-195 SF-195-03 — keywords critiques qui amplifient visuellement un risque
// VALIDÉ (pictogramme 🔴 + liseré rouge `ERROR` au lieu du ⚠️ + or par défaut).
// Liste alignée sur le mapping `RisqueToolMatcher` SF-195-01 (harcèlement →
// F-DT-12, violence → F-FA-14, expulsion → F-IM-08, dilapidation → F-FA-15).
const RISQUE_CRITICAL_KEYWORDS = [
  'harcèlement',
  'harcelement',
  'violence',
  'expulsion',
  'dilapidation',
];

@Injectable({ providedIn: 'root' })
export class PdfExportService {

  constructor(private snackBar: MatSnackBar) {}

  export(
    caseFile: CaseFile,
    synthesis: CaseAnalysisResult,
    retainedPistes?: RetainedPisteAlignment[],
    toolLabelResolver?: (toolId: string) => string | null,
    procedureChecksAlignment?: ProcedureCheckAlignment[],
    piecesAlignment?: PieceManquanteAlignment[],
    risquesAlignment?: RisqueAlignment[],
    aiQuestionsAlignment?: AiQuestionAlignment[],
  ): void {
    import('pdfmake/build/pdfmake').then(pdfMakeModule => {
      import('pdfmake/build/vfs_fonts').then(vfsFontsModule => {
        const pdfMake = (pdfMakeModule.default || pdfMakeModule) as any;
        const vfsFonts = (vfsFontsModule.default || vfsFontsModule) as any;
        pdfMake.vfs = vfsFonts.pdfMake ? vfsFonts.pdfMake.vfs : vfsFonts.vfs;

        const docDefinition = this.buildDocument(
          caseFile,
          synthesis,
          retainedPistes,
          toolLabelResolver,
          procedureChecksAlignment,
          piecesAlignment,
          risquesAlignment,
          aiQuestionsAlignment,
        ) as TDocumentDefinitions;
        const fileName = this.buildFileName(caseFile.title, synthesis);
        pdfMake.createPdf(docDefinition).download(fileName);
      });
    });
  }

  buildDocument(
    caseFile: CaseFile,
    synthesis: CaseAnalysisResult,
    retainedPistes?: RetainedPisteAlignment[],
    toolLabelResolver?: (toolId: string) => string | null,
    procedureChecksAlignment?: ProcedureCheckAlignment[],
    piecesAlignment?: PieceManquanteAlignment[],
    risquesAlignment?: RisqueAlignment[],
    aiQuestionsAlignment?: AiQuestionAlignment[],
  ): object {
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
      ...this.buildStrategiesRetenuesSection(retainedPistes, toolLabelResolver),
      ...this.buildProcedureChecksSection(procedureChecksAlignment, toolLabelResolver),
      ...this.buildPiecesADemanderSection(piecesAlignment, toolLabelResolver),
      ...this.buildRisquesACreuserSection(risquesAlignment, toolLabelResolver),
      ...this.buildRisquesValidesSection(risquesAlignment, synthesis, toolLabelResolver),
      ...this.buildAiQuestionsSection(aiQuestionsAlignment),
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

  /**
   * F-192 SF-192-03 — Section « 🎯 Stratégies retenues » insérée juste après
   * la page de garde (avant la chronologie / faits / etc.).
   *
   * Comportement :
   *   - retainedPistes vide ou non fourni → tableau vide (section omise, fail-open)
   *   - ≥ 1 piste RETAINED → titre de section + un bloc par piste (texte +
   *     baseJuridique + horizonTemporel + conditions + badge alignement),
   *     séparés par une ligne navy fine.
   *
   * Badge alignement (selon `matchStatus`) :
   *   - ALIGNED        → ✅ Stratégie alignée avec l'outil <label>      (or)
   *   - DIVERGENT      → ⚠️ Stratégie divergente avec l'outil <label>   (navy souligné)
   *   - NOT_ANALYZED   → ⏳ Outil <label> non encore analysé
   *   - NO_TARGET_TOOL → pas de badge (pistes Famille/Travail V1)
   *
   * Le `toolLabelResolver` (fourni par le SynthesisComponent) lit
   * `TOOL_LABEL` static via `TOOL_REGISTRY` du panel F-IA-04. Defensive :
   * si le resolver retourne `null` ou si non fourni, le toolId brut est
   * affiché.
   */
  private buildStrategiesRetenuesSection(
    retainedPistes?: RetainedPisteAlignment[],
    toolLabelResolver?: (toolId: string) => string | null,
  ): object[] {
    if (!retainedPistes || retainedPistes.length === 0) {
      return [];
    }

    const sections: object[] = [
      {
        text: '🎯 Stratégies retenues',
        fontSize: 16,
        bold: true,
        color: PRIMARY,
        margin: [0, 0, 0, 12],
      },
    ];

    retainedPistes.forEach((piste, index) => {
      sections.push(this.buildPisteBlock(piste, toolLabelResolver));
      if (index < retainedPistes.length - 1) {
        sections.push({
          canvas: [
            { type: 'line', x1: 0, y1: 0, x2: 500, y2: 0, lineWidth: 0.5, lineColor: PRIMARY },
          ],
          margin: [0, 8, 0, 8],
        });
      }
    });

    sections.push({ text: '', margin: [0, 0, 0, 16] });
    sections.push({ text: '', pageBreak: 'after' });

    return sections;
  }

  /** F-192 SF-192-03 — un bloc par piste retenue. */
  private buildPisteBlock(
    piste: RetainedPisteAlignment,
    toolLabelResolver?: (toolId: string) => string | null,
  ): object {
    const stack: object[] = [
      { text: piste.texte, fontSize: 11, color: TEXT, margin: [0, 0, 0, 4] },
    ];

    if (piste.baseJuridique) {
      stack.push({
        text: piste.baseJuridique,
        font: 'JetBrainsMono',
        fontSize: 9,
        italics: true,
        color: TEXT_SECONDARY,
        margin: [0, 0, 0, 4],
      });
    }

    if (piste.horizonTemporel) {
      stack.push({
        text: `Horizon : ${piste.horizonTemporel}`,
        fontSize: 9,
        italics: true,
        color: TEXT_SECONDARY,
        margin: [0, 0, 0, 4],
      });
    }

    if (piste.conditions && piste.conditions.length > 0) {
      stack.push({
        ul: piste.conditions.map(c => ({ text: c, fontSize: 9, color: TEXT })),
        margin: [0, 2, 0, 4],
      });
    }

    const badge = this.buildAlignmentBadge(piste, toolLabelResolver);
    if (badge) {
      stack.push(badge);
    }

    return { stack, margin: [0, 0, 0, 8] };
  }

  /**
   * F-192 SF-192-03 — badge d'alignement avec l'outil cible.
   * Retourne `null` pour `NO_TARGET_TOOL` (pistes Famille/Travail V1).
   */
  private buildAlignmentBadge(
    piste: RetainedPisteAlignment,
    toolLabelResolver?: (toolId: string) => string | null,
  ): object | null {
    const status = piste.matchStatus;
    if (status === 'NO_TARGET_TOOL') {
      return null;
    }
    const toolId = piste.toolIdCible ?? '';
    const resolvedLabel = toolLabelResolver ? toolLabelResolver(toolId) : null;
    const label = (resolvedLabel && resolvedLabel.trim().length > 0) ? resolvedLabel : toolId;

    switch (status) {
      case 'ALIGNED':
        return {
          text: `✅ Stratégie alignée avec l'outil ${label}`,
          fontSize: 10,
          bold: true,
          color: ACCENT,
          margin: [0, 4, 0, 0],
        };
      case 'DIVERGENT':
        return {
          text: `⚠️ Stratégie divergente avec l'outil ${label}`,
          fontSize: 10,
          bold: true,
          color: PRIMARY,
          decoration: 'underline',
          margin: [0, 4, 0, 0],
        };
      case 'NOT_ANALYZED':
        return {
          text: `⏳ Outil ${label} non encore analysé`,
          fontSize: 10,
          italics: true,
          color: TEXT_SECONDARY,
          margin: [0, 4, 0, 0],
        };
      default:
        return null;
    }
  }

  /**
   * F-193 SF-193-03 — Section « 🔍 Conformité procédurale validée par votre
   * avocat » insérée APRÈS la section « Stratégies retenues » de F-192 et
   * AVANT la chronologie / faits / etc.
   *
   * Comportement :
   *   - procedureChecksAlignment vide ou non fourni → tableau vide (section
   *     omise, fail-open).
   *   - ≥ 1 check matérialisé → titre de section + 3 sous-blocs distincts
   *     selon les statuts présents (ALIGNED ✅ / NON_COMPLIANT_FLAG ❌ /
   *     TO_VERIFY_FLAG ⏳), séparés par une ligne navy fine.
   *
   * Pour chaque check, le suffixe `→ <label outil>` est ajouté si
   * `toolIdCible` est non null. Lookup via `toolLabelResolver` (TOOL_REGISTRY).
   *
   * Fail-open indépendant : l'échec de la section pistes (F-192) n'empêche
   * pas la section checks (F-193) — chacune est conditionnée à son propre
   * tableau.
   */
  private buildProcedureChecksSection(
    procedureChecksAlignment?: ProcedureCheckAlignment[],
    toolLabelResolver?: (toolId: string) => string | null,
  ): object[] {
    if (!procedureChecksAlignment || procedureChecksAlignment.length === 0) {
      return [];
    }

    // Tri par `statut` (pas `matchStatus`) — un check NO_TARGET_TOOL doit
    // apparaître dans le sous-bloc correspondant à son statut F-96 (cf.
    // CA-04 cas d'erreur SF-193-03 : « Tous les checks en NO_TARGET_TOOL →
    // Section incluse, listant les checks sans suffixe → <label outil> »).
    // Le matchStatus (ALIGNED / NON_COMPLIANT_FLAG / TO_VERIFY_FLAG) est
    // redondant avec `statut` (VERIFIED / NON_COMPLIANT / TO_CHECK) sauf en
    // cas de DIVERGENT (l'outil cible diverge sur un check VERIFIED), pour
    // lequel V1 considère que la décision avocat prime → traité comme ALIGNED.
    const aligned = procedureChecksAlignment.filter(c => c.statut === 'VERIFIED');
    const nonCompliant = procedureChecksAlignment.filter(c => c.statut === 'NON_COMPLIANT');
    const toVerify = procedureChecksAlignment.filter(c => c.statut === 'TO_CHECK');

    if (aligned.length === 0 && nonCompliant.length === 0 && toVerify.length === 0) {
      return [];
    }

    const sections: object[] = [
      {
        text: '🔍 Conformité procédurale validée par votre avocat',
        fontSize: 16,
        bold: true,
        color: PRIMARY,
        margin: [0, 0, 0, 12],
      },
    ];

    const subBlocks: object[][] = [];

    if (aligned.length > 0) {
      subBlocks.push(this.buildProcedureChecksSubBlock(
        '✅ Vérifications confirmées',
        ACCENT,
        '✅',
        aligned,
        toolLabelResolver,
      ));
    }

    if (nonCompliant.length > 0) {
      subBlocks.push(this.buildProcedureChecksSubBlock(
        '❌ Points non conformes',
        ERROR,
        '❌',
        nonCompliant,
        toolLabelResolver,
      ));
    }

    if (toVerify.length > 0) {
      subBlocks.push(this.buildProcedureChecksSubBlock(
        '⏳ Points à vérifier',
        TEXT_SECONDARY,
        '⏳',
        toVerify,
        toolLabelResolver,
      ));
    }

    subBlocks.forEach((block, index) => {
      sections.push(...block);
      if (index < subBlocks.length - 1) {
        sections.push({
          canvas: [
            { type: 'line', x1: 0, y1: 0, x2: 500, y2: 0, lineWidth: 0.5, lineColor: PRIMARY },
          ],
          margin: [0, 8, 0, 8],
        });
      }
    });

    sections.push({ text: '', margin: [0, 0, 0, 16] });
    sections.push({ text: '', pageBreak: 'after' });

    return sections;
  }

  /**
   * F-193 SF-193-03 — un sous-bloc (titre coloré + liste de checks) pour une
   * famille de statut (ALIGNED / NON_COMPLIANT_FLAG / TO_VERIFY_FLAG).
   */
  private buildProcedureChecksSubBlock(
    title: string,
    titleColor: string,
    badge: string,
    checks: ProcedureCheckAlignment[],
    toolLabelResolver?: (toolId: string) => string | null,
  ): object[] {
    const block: object[] = [
      {
        text: title,
        fontSize: 12,
        bold: true,
        color: titleColor,
        margin: [0, 0, 0, 6],
      },
    ];

    checks.forEach(check => {
      block.push(this.buildProcedureCheckItem(check, badge, toolLabelResolver));
    });

    return block;
  }

  /**
   * F-193 SF-193-03 — un item check : badge + libellé Inter regular 11,
   * raison Inter italique 9 grise (si présente), suffixe outil JetBrains
   * Mono italique 9 (si toolIdCible non null).
   */
  private buildProcedureCheckItem(
    check: ProcedureCheckAlignment,
    badge: string,
    toolLabelResolver?: (toolId: string) => string | null,
  ): object {
    const stack: object[] = [
      {
        text: `${badge}  ${check.libelle}`,
        fontSize: 11,
        color: TEXT,
        margin: [0, 0, 0, 2],
      },
    ];

    if (check.raison) {
      stack.push({
        text: check.raison,
        fontSize: 9,
        italics: true,
        color: TEXT_SECONDARY,
        margin: [12, 0, 0, 2],
      });
    }

    if (check.toolIdCible) {
      const resolvedLabel = toolLabelResolver
        ? toolLabelResolver(check.toolIdCible)
        : null;
      const label = (resolvedLabel && resolvedLabel.trim().length > 0)
        ? resolvedLabel
        : check.toolIdCible;
      stack.push({
        text: `→ ${label}`,
        font: 'JetBrainsMono',
        fontSize: 9,
        italics: true,
        color: TEXT_SECONDARY,
        margin: [12, 0, 0, 2],
      });
    }

    return { stack, margin: [0, 0, 0, 6] };
  }

  /**
   * F-194 SF-194-03 — Section « 📎 Pièces à demander au client » insérée
   * APRÈS la section « Conformité procédurale » F-193 et AVANT la
   * chronologie / faits / etc. Cette section est le **livrable principal
   * côté client** : l'avocat envoie le PDF au client comme todo-list de
   * pièces à fournir.
   *
   * Comportement :
   *   - `piecesAlignment` vide ou non fourni → tableau vide (section
   *     omise, fail-open).
   *   - Aucune pièce statut À_DEMANDER → section omise (cas nominal).
   *   - ≥ 1 pièce À_DEMANDER → titre proéminent (encadré or, fond or
   *     léger) + sous-titre client + liste à cocher (Pièce / Destinataire
   *     / Date butoir = today + 14j). Compteurs OBTENUE / NON_APPLICABLE
   *     en sous-blocs si ≥ 1.
   *
   * Le suffixe `→ <label outil>` est ajouté à la pièce si
   * `toolIdsCibles[0]` est non null. Lookup via `toolLabelResolver`
   * (TOOL_REGISTRY). Fail-open : si le lookup échoue, on affiche le
   * `toolId` brut.
   *
   * Fail-open indépendant : l'échec des sections F-192 / F-193 n'empêche
   * pas la section F-194 — chacune est conditionnée à son propre tableau.
   */
  private buildPiecesADemanderSection(
    piecesAlignment?: PieceManquanteAlignment[],
    toolLabelResolver?: (toolId: string) => string | null,
  ): object[] {
    if (!piecesAlignment || piecesAlignment.length === 0) {
      return [];
    }

    const aDemander = piecesAlignment.filter(p => p.statut === 'A_DEMANDER');
    const obtenue = piecesAlignment.filter(p => p.statut === 'OBTENUE');
    const nonApplicable = piecesAlignment.filter(p => p.statut === 'NON_APPLICABLE');

    if (aDemander.length === 0) {
      // Cas nominal : rien à demander → section omise (compteurs sans
      // À_DEMANDER ne sont pas affichés seuls — l'info principale est
      // ce qui reste à fournir au client).
      return [];
    }

    // Date butoir = today + 14 jours, formatée JJ/MM/AAAA
    const dateButoir = this.formatDateButoir(this.computeDateButoirIn(14));

    const sections: object[] = [
      // Titre proéminent : encadré or avec fond or léger (CA-04)
      {
        table: {
          widths: ['*'],
          body: [[{
            text: '📎 Pièces à demander au client',
            fontSize: 18,
            bold: true,
            color: PRIMARY,
            margin: [12, 10, 12, 10],
            fillColor: PIECES_BG,
          }]],
        },
        layout: {
          hLineWidth: () => 1.2,
          vLineWidth: () => 1.2,
          hLineColor: () => ACCENT,
          vLineColor: () => ACCENT,
        },
        margin: [0, 0, 0, 10],
      },
      // Sous-titre client (Inter regular 11)
      {
        text: 'Pour avancer sur votre dossier, merci de transmettre les pièces suivantes :',
        fontSize: 11,
        color: TEXT,
        margin: [0, 0, 0, 12],
      },
    ];

    // Tableau : ☐ + Pièce + Destinataire + Date butoir
    sections.push(this.buildPiecesADemanderTable(aDemander, dateButoir, toolLabelResolver));

    // Sous-bloc compteur OBTENUE (CA-07)
    if (obtenue.length > 0) {
      sections.push({
        text: `✅ Pièces déjà reçues : ${obtenue.length}`,
        fontSize: 9,
        color: TEXT_SECONDARY,
        margin: [0, 8, 0, 0],
      });
    }

    // Sous-bloc compteur NON_APPLICABLE (CA-07)
    if (nonApplicable.length > 0) {
      sections.push({
        text: `🚫 Pièces non applicables au dossier : ${nonApplicable.length}`,
        fontSize: 9,
        color: TEXT_SECONDARY,
        margin: [0, 4, 0, 0],
      });
    }

    sections.push({ text: '', margin: [0, 0, 0, 16] });
    // Page break — la todo-list peut être imprimée seule (CA-12)
    sections.push({ text: '', pageBreak: 'after' });

    return sections;
  }

  /**
   * F-194 SF-194-03 — tableau case à cocher pour les pièces À_DEMANDER.
   *
   * Colonnes : ☐ (case) | Pièce (libellé + suffixe outil) | Destinataire
   * (italique 9 — défaut "Client") | Date butoir (JetBrainsMono 9).
   */
  private buildPiecesADemanderTable(
    pieces: PieceManquanteAlignment[],
    dateButoir: string,
    toolLabelResolver?: (toolId: string) => string | null,
  ): object {
    const headerRow = [
      { text: 'À fournir', bold: true, color: SURFACE, fillColor: ACCENT, fontSize: 9, margin: [8, 5, 4, 5] },
      { text: 'Pièce', bold: true, color: SURFACE, fillColor: ACCENT, fontSize: 9, margin: [4, 5, 4, 5] },
      { text: 'Destinataire', bold: true, color: SURFACE, fillColor: ACCENT, fontSize: 9, margin: [4, 5, 4, 5] },
      { text: 'Date butoir', bold: true, color: SURFACE, fillColor: ACCENT, fontSize: 9, margin: [4, 5, 8, 5] },
    ];

    const rows = pieces.map((piece, i) => {
      const bg = i % 2 === 0 ? BG : SURFACE;
      const pieceCell = this.buildPieceLibelleCell(piece, bg, toolLabelResolver);
      const destinataire = (piece.destinataire && piece.destinataire.trim().length > 0)
        ? piece.destinataire
        : 'Client';
      return [
        { text: '☐', fontSize: 14, color: PRIMARY, fillColor: bg, alignment: 'center', margin: [4, 4, 4, 4] },
        pieceCell,
        { text: destinataire, fontSize: 9, italics: true, color: TEXT_SECONDARY, fillColor: bg, margin: [4, 6, 4, 6] },
        { text: dateButoir, font: 'JetBrainsMono', fontSize: 9, color: TEXT, fillColor: bg, margin: [4, 6, 8, 6] },
      ];
    });

    return {
      table: {
        widths: [40, '*', 90, 70],
        headerRows: 1,
        body: [headerRow, ...rows],
      },
      layout: {
        hLineWidth: () => 0.5,
        vLineWidth: () => 0,
        hLineColor: () => DIVIDER,
      },
      margin: [0, 0, 0, 0],
    };
  }

  /**
   * F-194 SF-194-03 — cellule libellé d'une pièce, avec suffixe
   * `→ <label outil>` JetBrainsMono italique 9 si `toolIdsCibles[0]`
   * est renseigné. Pattern miroir du suffixe SF-192-03 / SF-193-03.
   */
  private buildPieceLibelleCell(
    piece: PieceManquanteAlignment,
    bg: string,
    toolLabelResolver?: (toolId: string) => string | null,
  ): object {
    const stack: object[] = [
      { text: piece.pieceLibelle, fontSize: 10, color: TEXT },
    ];

    const firstToolId = piece.toolIdsCibles && piece.toolIdsCibles.length > 0
      ? piece.toolIdsCibles[0]
      : null;
    if (firstToolId) {
      const resolvedLabel = toolLabelResolver ? toolLabelResolver(firstToolId) : null;
      const label = (resolvedLabel && resolvedLabel.trim().length > 0)
        ? resolvedLabel
        : firstToolId;
      stack.push({
        text: `→ ${label}`,
        font: 'JetBrainsMono',
        fontSize: 9,
        italics: true,
        color: TEXT_SECONDARY,
        margin: [0, 2, 0, 0],
      });
    }

    return { stack, fillColor: bg, margin: [4, 6, 4, 6] };
  }

  /**
   * F-194 SF-194-03 — calcule today + N jours (heure locale, sans modifier
   * `Date.now()`). Exposé en méthode privée pour permettre le mock dans
   * les tests Jest.
   */
  private computeDateButoirIn(days: number): Date {
    const d = new Date();
    d.setDate(d.getDate() + days);
    return d;
  }

  /**
   * F-194 SF-194-03 — formate une date au format JJ/MM/AAAA (locale fr).
   */
  private formatDateButoir(d: Date): string {
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    return `${dd}/${mm}/${yyyy}`;
  }

  /**
   * F-253 SF-253-03 — Section « 🔍 Risques à creuser » insérée APRÈS la
   * section « 📎 Pièces à demander » F-194 et AVANT la section « ⚠️ Risques
   * retenus par votre avocat » F-195. Ordre logique : indécision avant
   * décision.
   *
   * <p>Donne un consommateur PDF au statut À_CREUSER (par défaut sur tout
   * risque IA non encore arbitré). Aujourd'hui le PDF agrégeait V/É via
   * F-195 mais ne mentionnait pas les arbitrages restants — l'avocat
   * recevait un PDF qui taisait son travail de curation en cours.</p>
   *
   * <p>Comportement :</p>
   * <ul>
   *   <li>{@code risquesAlignment} vide / non fourni → tableau vide (section
   *       omise, fail-open).</li>
   *   <li>Aucun risque statut À_CREUSER → section omise (l'avocat a tout
   *       arbitré — la section F-195 « Risques retenus » couvre déjà
   *       l'état final).</li>
   *   <li>≥ 1 risque À_CREUSER → bloc par risque (libellé Inter 11 +
   *       suffixe {@code → <label outil>} JetBrainsMono italique 9 si
   *       {@code toolIdsCibles[0]} non null). Liseré navy à gauche
   *       (palette gris navy subtil, cohérent avec la pill `to_explore`
   *       du panel décisionnel — pas de rouge ici, c'est de l'indécision).</li>
   * </ul>
   *
   * <p>Pas de section critique : un risque À_CREUSER reste neutre tant
   * que l'avocat ne l'a pas validé. La détection critique reste
   * exclusive à F-195 SF-195-03 (`buildRisquesValidesSection`).</p>
   */
  private buildRisquesACreuserSection(
    risquesAlignment: RisqueAlignment[] | undefined | null,
    toolLabelResolver?: (toolId: string) => string | null,
  ): object[] {
    if (!risquesAlignment || risquesAlignment.length === 0) {
      return [];
    }

    const aCreuser = risquesAlignment.filter(r => r.statut === 'A_CREUSER');
    if (aCreuser.length === 0) {
      return [];
    }

    const sections: object[] = [];

    // Titre navy + liseré navy (palette gris navy subtil — pas de rouge
    // ni d'or, ce sont des risques EN ATTENTE d'arbitrage).
    sections.push({
      table: {
        widths: ['*'],
        body: [[{
          text: '🔍 Risques à creuser',
          fontSize: 16,
          bold: true,
          color: PRIMARY,
          margin: [12, 8, 12, 8],
          fillColor: SURFACE,
        }]],
      },
      layout: {
        hLineWidth: () => 0.8,
        vLineWidth: () => 0.8,
        hLineColor: () => PRIMARY,
        vLineColor: () => PRIMARY,
      },
      margin: [0, 0, 0, 8],
    });

    // Sous-titre : N risque(s) à arbitrer.
    const total = aCreuser.length;
    sections.push({
      text: `${total} risque${total > 1 ? 's' : ''} à arbitrer — arbitrage avocat en attente`,
      font: 'JetBrainsMono',
      fontSize: 11,
      color: TEXT_SECONDARY,
      margin: [0, 0, 0, 12],
    });

    // Un bloc par risque À_CREUSER : pictogramme 🔍 + libellé + suffixe outil.
    aCreuser.forEach((risque, idx) => {
      sections.push(this.buildRisqueACreuserBloc(risque, toolLabelResolver, idx));
    });

    sections.push({ text: '', margin: [0, 0, 0, 16] });

    return sections;
  }

  /**
   * F-253 SF-253-03 — bloc visuel pour un risque À_CREUSER. Liseré navy à
   * gauche (palette gris navy subtil, cohérent avec la pill `to_explore`
   * du panel et la pill secondaire « 🔍 N à creuser » SF-253-02).
   *
   * Pas de variante critique (le critique vient après VALIDATION par
   * l'avocat — un À_CREUSER reste neutre tant que pas arbitré).
   */
  private buildRisqueACreuserBloc(
    risque: RisqueAlignment,
    toolLabelResolver: ((toolId: string) => string | null) | undefined,
    idx: number,
  ): object {
    const fillColor = idx % 2 === 0 ? BG : SURFACE;

    const stack: object[] = [
      {
        columns: [
          {
            width: 22,
            text: '🔍',
            fontSize: 12,
            margin: [0, 1, 0, 0],
          },
          {
            width: '*',
            text: risque.risqueLibelle,
            fontSize: 11,
            color: TEXT,
          },
        ],
      },
    ];

    const firstToolId = (risque.toolIdsCibles && risque.toolIdsCibles.length > 0)
      ? risque.toolIdsCibles[0]
      : null;
    if (firstToolId) {
      const resolvedLabel = toolLabelResolver ? toolLabelResolver(firstToolId) : null;
      const label = (resolvedLabel && resolvedLabel.trim().length > 0)
        ? resolvedLabel
        : firstToolId;
      stack.push({
        text: `→ ${label}`,
        font: 'JetBrainsMono',
        fontSize: 9,
        italics: true,
        color: TEXT_SECONDARY,
        margin: [22, 2, 0, 0],
      });
    }

    return {
      table: {
        widths: [3, '*'],
        body: [[
          { text: '', fillColor: PRIMARY, border: [false, false, false, false] },
          { stack, fillColor, margin: [8, 6, 8, 6], border: [false, false, false, false] },
        ]],
      },
      layout: {
        hLineWidth: () => 0,
        vLineWidth: () => 0,
        paddingLeft: () => 0,
        paddingRight: () => 0,
        paddingTop: () => 0,
        paddingBottom: () => 0,
      },
      margin: [0, 0, 0, 4],
    };
  }

  /**
   * F-195 SF-195-03 — Section « ⚠️ Risques retenus par votre avocat »
   * insérée APRÈS la section « 📎 Pièces à demander » F-194 et AVANT la
   * chronologie / faits / etc.
   *
   * Comportement :
   *   - `risquesAlignment` vide ou non fourni → tableau vide (section
   *     omise, fail-open).
   *   - Aucun risque statut VALIDÉ → section omise (cas nominal — les
   *     risques À_CREUSER restent affichés dans le bloc « Risques »
   *     classique de la synthèse, les ÉCARTÉ sont consultables là aussi
   *     mais non mis en évidence ici).
   *   - ≥ 1 risque VALIDÉ → bloc par risque (libellé Inter 11 + suffixe
   *     `→ <label outil>` JetBrainsMono italique 9 si toolIdsCibles[0] est
   *     non null). Liseré or à gauche par défaut, liseré rouge `ERROR` si
   *     keyword critique (harcèlement / violence / expulsion /
   *     dilapidation) — pictogramme `🔴` au lieu de `⚠️` dans ce cas.
   *   - Sous-titre « Score validé avocat : Y / 100 (vs IA brut : X / 100) »
   *     affiché si `synthesis.riskScore` ou `synthesis.riskScoreAvocat`
   *     présents (cohérent CA-03 mini-spec — fail-open si l'un absent).
   *
   * Fail-open indépendant : l'échec des sections F-192 / F-193 / F-194
   * n'empêche pas la section F-195 — chacune est conditionnée à son propre
   * tableau.
   */
  private buildRisquesValidesSection(
    risquesAlignment: RisqueAlignment[] | undefined | null,
    synthesis: CaseAnalysisResult,
    toolLabelResolver?: (toolId: string) => string | null,
  ): object[] {
    if (!risquesAlignment || risquesAlignment.length === 0) {
      return [];
    }

    const valides = risquesAlignment.filter(r => r.statut === 'VALIDE');
    const ecartes = risquesAlignment.filter(r => r.statut === 'ECARTE');

    if (valides.length === 0) {
      // Cas nominal : aucun risque retenu → section omise (les écartés
      // n'ont pas de mise en évidence dédiée — restent visibles dans la
      // synthèse normale).
      return [];
    }

    const sections: object[] = [];

    // Titre proéminent (taille 16 bold navy, cohérent mini-spec) + liseré
    // or léger (rouge réservé aux cas keyword critique uniquement, par
    // ligne de risque — palette charte navy/or par défaut).
    sections.push({
      table: {
        widths: ['*'],
        body: [[{
          text: '⚠️ Risques retenus par votre avocat',
          fontSize: 16,
          bold: true,
          color: PRIMARY,
          margin: [12, 8, 12, 8],
          fillColor: SURFACE,
        }]],
      },
      layout: {
        hLineWidth: () => 0.8,
        vLineWidth: () => 0.8,
        hLineColor: () => ACCENT,
        vLineColor: () => ACCENT,
      },
      margin: [0, 0, 0, 8],
    });

    // Sous-titre score validé / IA brut si l'un des deux est dispo (CA-03)
    const scoreSubtitle = this.formatRisqueScoreSubtitle(synthesis);
    if (scoreSubtitle) {
      sections.push({
        text: scoreSubtitle,
        font: 'JetBrainsMono',
        fontSize: 11,
        color: TEXT_SECONDARY,
        margin: [0, 0, 0, 12],
      });
    }

    // Un bloc par risque VALIDÉ : pictogramme + libellé + suffixe outil.
    valides.forEach((risque, idx) => {
      const isCritical = this.isRisqueCritical(risque.risqueLibelle);
      sections.push(this.buildRisqueValideBloc(risque, isCritical, toolLabelResolver, idx));
    });

    // Sous-bloc compteur ÉCARTÉS (cohérent F-194 — pas de liste détaillée
    // pour ne pas allonger inutilement le PDF).
    if (ecartes.length > 0) {
      sections.push({
        text: `❌ Risques écartés : ${ecartes.length}`,
        fontSize: 9,
        italics: true,
        color: TEXT_SECONDARY,
        margin: [0, 8, 0, 0],
      });
    }

    sections.push({ text: '', margin: [0, 0, 0, 16] });

    return sections;
  }

  /**
   * F-195 SF-195-03 — bloc visuel pour un risque VALIDÉ.
   *
   * Liseré or à gauche par défaut (charte navy/or), liseré rouge `ERROR`
   * + pictogramme `🔴` quand le libellé contient un keyword critique
   * (harcèlement / violence / expulsion / dilapidation) — palette rouge
   * réservée aux alertes critiques (DESIGN_SYSTEM.md).
   */
  private buildRisqueValideBloc(
    risque: RisqueAlignment,
    isCritical: boolean,
    toolLabelResolver: ((toolId: string) => string | null) | undefined,
    idx: number,
  ): object {
    const borderColor = isCritical ? ERROR : ACCENT;
    const fillColor = idx % 2 === 0 ? BG : SURFACE;
    const picto = isCritical ? '🔴' : '⚠️';

    const stack: object[] = [
      {
        columns: [
          {
            width: 22,
            text: picto,
            fontSize: 12,
            margin: [0, 1, 0, 0],
          },
          {
            width: '*',
            text: risque.risqueLibelle,
            fontSize: 11,
            color: TEXT,
          },
        ],
      },
    ];

    const firstToolId = (risque.toolIdsCibles && risque.toolIdsCibles.length > 0)
      ? risque.toolIdsCibles[0]
      : null;
    if (firstToolId) {
      const resolvedLabel = toolLabelResolver ? toolLabelResolver(firstToolId) : null;
      const label = (resolvedLabel && resolvedLabel.trim().length > 0)
        ? resolvedLabel
        : firstToolId;
      stack.push({
        text: `→ ${label}`,
        font: 'JetBrainsMono',
        fontSize: 9,
        italics: true,
        color: TEXT_SECONDARY,
        margin: [22, 2, 0, 0],
      });
    }

    // Liseré or (ou rouge) à gauche du bloc (cellule étroite teintée).
    return {
      table: {
        widths: [3, '*'],
        body: [[
          { text: '', fillColor: borderColor, border: [false, false, false, false] },
          { stack, fillColor, margin: [8, 6, 8, 6], border: [false, false, false, false] },
        ]],
      },
      layout: {
        hLineWidth: () => 0,
        vLineWidth: () => 0,
        paddingLeft: () => 0,
        paddingRight: () => 0,
        paddingTop: () => 0,
        paddingBottom: () => 0,
      },
      margin: [0, 0, 0, 4],
    };
  }

  /**
   * F-195 SF-195-03 — détecte si un libellé de risque contient un keyword
   * critique (insensible à la casse + à la diacritique pour
   * « harcèlement » / « harcelement »). Cohérent avec
   * `RisqueToolMatcher` SF-195-01.
   */
  private isRisqueCritical(libelle: string): boolean {
    if (!libelle) return false;
    const lower = libelle.toLowerCase();
    return RISQUE_CRITICAL_KEYWORDS.some(k => lower.includes(k));
  }

  /**
   * F-195 SF-195-03 — produit le sous-titre score validé avocat / IA brut
   * si l'un des deux scores est disponible. Le champ optionnel
   * `riskScoreAvocat` est lu de façon défensive (le backend SF-195-01 peut
   * ne pas être encore mergé — fail-open : sous-titre omis).
   *
   * Format :
   *   - Les deux présents : « Score validé avocat : Y / 100 (vs IA brut : X / 100) »
   *   - Avocat seul       : « Score validé avocat : Y / 100 »
   *   - IA brut seul      : « Score IA brut : X / 100 »
   *   - Aucun             : null (sous-titre omis)
   */
  private formatRisqueScoreSubtitle(synthesis: CaseAnalysisResult): string | null {
    const ia = synthesis?.riskScore;
    // Le champ `riskScoreAvocat` peut ne pas exister sur le type V1 ; on
    // l'accède de façon défensive (fail-open en attendant SF-195-01).
    const avocat = (synthesis as unknown as { riskScoreAvocat?: number | null })?.riskScoreAvocat;
    const iaPresent = typeof ia === 'number' && Number.isFinite(ia);
    const avocatPresent = typeof avocat === 'number' && Number.isFinite(avocat);
    if (avocatPresent && iaPresent) {
      return `Score validé avocat : ${avocat} / 100 (vs IA brut : ${ia} / 100)`;
    }
    if (avocatPresent) {
      return `Score validé avocat : ${avocat} / 100`;
    }
    if (iaPresent) {
      return `Score IA brut : ${ia} / 100`;
    }
    return null;
  }

  /**
   * F-196 SF-196-03 — Section « ❓ Réponses aux questions complémentaires »
   * insérée APRÈS « Risques validés par votre avocat » (F-195) et AVANT la
   * chronologie / faits.
   *
   * Comportement :
   *   - aiQuestionsAlignment vide / non fourni → tableau vide (section omise,
   *     fail-open identique aux 4 sections F-192..F-195).
   *   - Filtre sur `answerText` non null/non vide → seules les questions
   *     RÉPONDUES par l'avocat sont incluses au PDF (les non répondues
   *     restent visibles dans la synthèse écran F-94).
   *   - ≥ 1 question répondue → titre navy 16/bold + sous-titre + liste Q&R
   *     avec pictogramme `❓`. Suffixe « → Pièce déduite : <libellé> » si
   *     `pieceLibelleDeduit` non null (signal positif PIECE_OBTENUE ou
   *     manquante PIECE_MANQUANTE).
   */
  private buildAiQuestionsSection(
    aiQuestionsAlignment?: AiQuestionAlignment[],
  ): object[] {
    if (!aiQuestionsAlignment || aiQuestionsAlignment.length === 0) {
      return [];
    }

    const repondues = aiQuestionsAlignment.filter(
      q => q.answerText !== null && q.answerText !== undefined && q.answerText.trim().length > 0,
    );
    if (repondues.length === 0) {
      return [];
    }

    const sections: object[] = [
      {
        text: '❓ Réponses aux questions complémentaires',
        fontSize: 16,
        bold: true,
        color: PRIMARY,
        margin: [0, 0, 0, 6],
      },
      {
        text: 'Réponses fournies par votre avocat aux questions complémentaires posées par l\'IA pour préciser le diagnostic.',
        fontSize: 10,
        color: TEXT_SECONDARY,
        italics: true,
        margin: [0, 0, 0, 12],
      },
    ];

    for (const q of repondues) {
      sections.push(this.buildAiQuestionBloc(q));
    }

    sections.push({ text: '', margin: [0, 0, 0, 16] });
    sections.push({ text: '', pageBreak: 'after' });

    return sections;
  }

  private buildAiQuestionBloc(q: AiQuestionAlignment): object {
    const stack: object[] = [];

    const questionText = q.questionText && q.questionText.trim().length > 0
      ? q.questionText
      : '(Question non disponible)';

    stack.push({
      text: [
        { text: '❓ ', color: ACCENT, fontSize: 11 },
        { text: questionText, color: PRIMARY, fontSize: 11, bold: true },
      ],
      margin: [0, 0, 0, 4],
    });

    stack.push({
      text: q.answerText ?? '',
      font: 'JetBrainsMono',
      italics: true,
      fontSize: 9,
      color: TEXT,
      margin: [12, 0, 0, 4],
    });

    if (q.pieceLibelleDeduit && q.pieceLibelleDeduit.trim().length > 0) {
      const prefix = q.statutDeduction === 'PIECE_OBTENUE'
        ? '✅ Pièce confirmée'
        : q.statutDeduction === 'PIECE_MANQUANTE'
          ? '📩 Pièce à demander'
          : '→ Pièce déduite';
      stack.push({
        text: `${prefix} : ${q.pieceLibelleDeduit}`,
        fontSize: 9,
        color: TEXT_SECONDARY,
        margin: [12, 0, 0, 8],
      });
    } else {
      stack.push({ text: '', margin: [0, 0, 0, 8] });
    }

    return { stack };
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

  /**
   * F-98 / SF-98-51 — Exporte une version de conclusions au format `.pdf`.
   *
   * Construit un document `pdfmake` à partir du `content` texte de la version :
   * les lignes d'en-tête de section (entièrement en MAJUSCULES, courtes — type
   * `POUR`, `CONTRE`, `FAITS ET PROCÉDURE`, `DISCUSSION`, `PAR CES MOTIFS`)
   * sont rendues en titres ; les autres lignes en paragraphes. Les lignes vides
   * sont préservées pour conserver l'aération du texte.
   *
   * Réutilise le pattern `pdfmake` du service (import dynamique de
   * `pdfmake/build/pdfmake` + `vfs_fonts`, `createPdf(...).download(...)`) et
   * la même heuristique d'en-tête de section que `DocxExportService`
   * (SF-98-50). Un échec (import ou génération) affiche une `MatSnackBar`
   * d'erreur, sans téléchargement.
   *
   * @param content        texte de la version de conclusions.
   * @param caseTitle      titre du dossier — sert au nommage du fichier.
   * @param versionNumber  numéro de la version exportée.
   */
  exportConclusion(content: string, caseTitle: string, versionNumber: number): void {
    import('pdfmake/build/pdfmake').then(pdfMakeModule => {
      import('pdfmake/build/vfs_fonts').then(vfsFontsModule => {
        const pdfMake = (pdfMakeModule.default || pdfMakeModule) as any;
        const vfsFonts = (vfsFontsModule.default || vfsFontsModule) as any;
        pdfMake.vfs = vfsFonts.pdfMake ? vfsFonts.pdfMake.vfs : vfsFonts.vfs;

        const docDefinition = this.buildConclusionDocument(content) as TDocumentDefinitions;
        const fileName = this.buildConclusionFileName(caseTitle, versionNumber);
        pdfMake.createPdf(docDefinition).download(fileName);
      }).catch(() => this.notifyConclusionError());
    }).catch(() => this.notifyConclusionError());
  }

  /**
   * Document `pdfmake` d'une version de conclusions : chaque ligne du `content`
   * devient un paragraphe ; les en-têtes de section sont stylés en titre.
   */
  buildConclusionDocument(content: string): object {
    const lines = (content ?? '').split('\n');
    const body = lines.map((line) =>
      this.isSectionHeading(line)
        ? { text: line.trim(), style: 'sectionTitle' }
        : { text: line, style: 'paragraph' },
    );

    return {
      pageSize: 'A4',
      pageMargins: [48, 64, 48, 64],
      content: body,
      defaultStyle: {
        font: 'Roboto',
        fontSize: 10,
        color: TEXT,
        lineHeight: 1.4,
      },
      styles: {
        sectionTitle: { fontSize: 12, bold: true, color: PRIMARY, margin: [0, 12, 0, 6] },
        paragraph: { margin: [0, 0, 0, 4] },
      },
    };
  }

  /**
   * Nom de fichier d'une version de conclusions :
   * `{slug-du-dossier}-conclusions-v{N}.pdf` (fallback `conclusions-vN.pdf`
   * si le titre est vide). Même heuristique de slug que `buildFileName`.
   */
  buildConclusionFileName(caseTitle: string, versionNumber: number): string {
    const title = caseTitle?.trim() || '';
    if (!title) {
      return `conclusions-v${versionNumber}.pdf`;
    }
    return `${this.slugifyTitle(title)}-conclusions-v${versionNumber}.pdf`;
  }

  /** Affiche la `MatSnackBar` d'erreur de génération du PDF de conclusions. */
  private notifyConclusionError(): void {
    this.snackBar.open('Erreur lors de la génération du document PDF.', 'Fermer', {
      duration: 4000, panelClass: ['snack-error'],
    });
  }

  /**
   * Vrai si la ligne est un en-tête de section : une fois la ponctuation et
   * les espaces retirés, elle ne contient que des lettres en MAJUSCULES, au
   * moins une lettre, et reste courte (≤ 60 caractères). Même heuristique que
   * `DocxExportService.isSectionHeading` (SF-98-50) — robuste pour la
   * structure produite par `CaseConclusionPromptBuilder` (`POUR`, `CONTRE`,
   * `FAITS ET PROCÉDURE`, `DISCUSSION`, `PAR CES MOTIFS`…).
   */
  private isSectionHeading(line: string): boolean {
    const trimmed = line.trim();
    if (trimmed.length === 0 || trimmed.length > 60) {
      return false;
    }
    const letters = trimmed.replace(/[^\p{L}]/gu, '');
    if (letters.length === 0) {
      return false;
    }
    return letters === letters.toUpperCase() && letters !== letters.toLowerCase();
  }

  /** Slug ASCII minuscule, accents retirés, tronqué à 40 caractères. */
  private slugifyTitle(value: string): string {
    return value
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 40);
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
