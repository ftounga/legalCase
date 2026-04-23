import { Component, Input, computed, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  DivorceConsentementScoring,
  DivorceConsentementValidityDetection,
  DivorceConsentementVerdict,
} from '../../core/models/case-analysis.model';

const CRITERE_LABELS: Record<string, string> = {
  DC_MAJORITE: 'Les deux époux majeurs',
  DC_CONSENTEMENT_LIBRE: 'Consentement libre (absence de vice)',
  DC_CONVENTION_EQUITABLE: 'Convention équitable entre époux',
  DC_ENFANT_MINEUR_ENTENDU: 'Enfant mineur informé du droit d\'être entendu',
  DC_DELAI_REFLEXION_15J: 'Délai de réflexion 15 jours respecté',
  DC_NOTAIRE_DEPOT: 'Convention déposée chez notaire',
  DC_INDEPENDANCE_AVOCATS: 'Un avocat distinct par époux',
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

  @Input() set detection(v: DivorceConsentementValidityDetection | null | undefined) {
    this.detectionSignal.set(v ?? null);
  }
  @Input() set scoring(v: DivorceConsentementScoring | null | undefined) {
    this.scoringSignal.set(v ?? null);
  }

  readonly critereEntries = computed(() => {
    const det = this.detectionSignal();
    if (!det) return [];
    return CRITERE_ORDER.map(code => ({
      code,
      label: CRITERE_LABELS[code],
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
