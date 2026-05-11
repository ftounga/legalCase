import { Component, Input, computed, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  DivorceConsentementScoring,
  DivorceConsentementValidityDetection,
  DivorceConsentementVerdict,
} from '../../core/models/case-analysis.model';

const CRITERE_LABELS_FR: Record<string, string> = {
  DC_MAJORITE: 'Les deux époux majeurs',
  DC_CONSENTEMENT_LIBRE: 'Consentement libre (absence de vice)',
  DC_CONVENTION_EQUITABLE: 'Convention équitable entre époux',
  DC_ENFANT_MINEUR_ENTENDU: 'Enfant mineur informé du droit d\'être entendu',
  DC_DELAI_REFLEXION_15J: 'Délai de réflexion 15 jours respecté (art. 229-4 Cciv)',
  DC_NOTAIRE_DEPOT: 'Convention déposée chez notaire (art. 229-1 Cciv)',
  DC_INDEPENDANCE_AVOCATS: 'Un avocat distinct par époux (art. 229-2 Cciv)',
};

/**
 * F-242 : libellés BE des 7 critères validité divorce par consentement mutuel.
 *
 * Les codes restent FR (le scoring backend est calculé selon le droit FR — art. 229+
 * Cciv), mais l'affichage adapte les libellés au droit belge (Code judiciaire
 * art. 1287-1304 + Loi 27/04/2007) pour ne pas induire l'avocat BE en erreur.
 *
 * <p><strong>Limites assumées</strong> (cf. bannière info en BE) :
 * <ul>
 *   <li>Le critère `DC_DELAI_REFLEXION_15J` calcule un délai de 15 jours (FR)
 *       — en BE le délai est de 3 mois (art. 1289 CJ) avec dispense si séparation
 *       préalable ≥ 6 mois. La réponse OUI/NON peut donc être trompeuse ; le
 *       libellé est adapté mais une vérification manuelle reste nécessaire.</li>
 *   <li>Le critère `DC_NOTAIRE_DEPOT` répond OUI/NON sur le dépôt notarié FR
 *       — en BE le dépôt se fait au greffe du tribunal de la famille (art. 1288 CJ).</li>
 * </ul>
 */
const CRITERE_LABELS_BE: Record<string, string> = {
  DC_MAJORITE: 'Les deux époux majeurs',
  DC_CONSENTEMENT_LIBRE: 'Consentement libre (absence de vice — art. 1287 CJ)',
  DC_CONVENTION_EQUITABLE: 'Convention préalable couvrant tous les effets (art. 1287-1294 CJ)',
  DC_ENFANT_MINEUR_ENTENDU: 'Enfant mineur ≥ 12 ans entendu si la convention l\'affecte (art. 1004/1 CJ)',
  DC_DELAI_REFLEXION_15J: 'Délai de réflexion 3 mois respecté (art. 1289 CJ — dispense si séparation ≥ 6 mois) ⚠ calculé selon droit FR (15j)',
  DC_NOTAIRE_DEPOT: 'Convention déposée au greffe du tribunal de la famille (art. 1288 CJ) ⚠ calculé selon droit FR (notaire)',
  DC_INDEPENDANCE_AVOCATS: 'Représentation séparée par avocats (non obligatoire en BE)',
};

const CRITERE_ORDER = [
  'DC_MAJORITE',
  'DC_CONSENTEMENT_LIBRE',
  'DC_CONVENTION_EQUITABLE',
  'DC_ENFANT_MINEUR_ENTENDU',
  'DC_DELAI_REFLEXION_15J',
  'DC_NOTAIRE_DEPOT',
  'DC_INDEPENDANCE_AVOCATS',
];

/**
 * F-152 SF-152-01 : bloc synthèse famille — validité du divorce par
 * consentement mutuel avec jauge de score + checklist des 7 critères.
 */
@Component({
  selector: 'app-divorce-consentement-scoring-section',
  standalone: true,
  imports: [MatIconModule, MatExpansionModule, MatTooltipModule],
  templateUrl: './divorce-consentement-scoring-section.component.html',
  styleUrl: './divorce-consentement-scoring-section.component.scss',
})
export class DivorceConsentementScoringSectionComponent {
  readonly detectionSignal = signal<DivorceConsentementValidityDetection | null>(null);
  readonly scoringSignal = signal<DivorceConsentementScoring | null>(null);
  /** F-242 : pays applicable du workspace — adapte libellés FR/BE. */
  readonly workspaceCountrySignal = signal<string | null>(null);

  @Input() set detection(v: DivorceConsentementValidityDetection | null | undefined) {
    this.detectionSignal.set(v ?? null);
  }
  @Input() set scoring(v: DivorceConsentementScoring | null | undefined) {
    this.scoringSignal.set(v ?? null);
  }
  @Input() set workspaceCountry(v: string | null | undefined) {
    this.workspaceCountrySignal.set(v ?? null);
  }

  readonly isBe = computed(() => this.workspaceCountrySignal() === 'BE');

  readonly critereEntries = computed(() => {
    const det = this.detectionSignal();
    if (!det) return [];
    const labels = this.isBe() ? CRITERE_LABELS_BE : CRITERE_LABELS_FR;
    return CRITERE_ORDER.map(code => ({
      code,
      label: labels[code],
      answer: det.detections[code] ?? null,
    }));
  });

  verdictLabel(verdict: DivorceConsentementVerdict): string {
    switch (verdict) {
      case 'VALIDE': return 'Validité confirmée';
      case 'RISQUE_MOYEN': return 'Risque modéré';
      case 'RISQUE_ELEVE_NULLITE': return 'Risque élevé de nullité';
    }
  }

  verdictClass(verdict: DivorceConsentementVerdict): string {
    switch (verdict) {
      case 'VALIDE': return 'verdict-badge verdict-badge--ok';
      case 'RISQUE_MOYEN': return 'verdict-badge verdict-badge--warn';
      case 'RISQUE_ELEVE_NULLITE': return 'verdict-badge verdict-badge--error';
    }
  }

  iconFor(reponse: string | null | undefined): string {
    if (reponse === 'OUI') return 'check_circle';
    if (reponse === 'NON') return 'cancel';
    return 'help_outline';
  }

  iconClass(reponse: string | null | undefined): string {
    if (reponse === 'OUI') return 'critere-icon critere-icon--ok';
    if (reponse === 'NON') return 'critere-icon critere-icon--ko';
    return 'critere-icon critere-icon--inconnu';
  }

  /** 314.16 = 2π·50 (circonférence du cercle SVG de rayon 50). */
  readonly gaugeCircumference = 2 * Math.PI * 50;

  gaugeOffset(score: number): number {
    return this.gaugeCircumference * (1 - score / 100);
  }
}
