import { ChangeDetectionStrategy, Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

import { ProcedureCheckAlignment } from '../../../core/models/procedure-check-alignment.model';

/**
 * F-193 SF-193-02 — Bloc partagé "Vérifications procédurales" affiché dans la
 * sortie de chaque outil décisionnel concerné. Rend 3 sous-sections selon les
 * checks F-96 alignés sur cet outil :
 *
 * <ul>
 *   <li>✅ <b>Vérifications confirmées par votre avocat</b> (ALIGNED) —
 *   border-left or, icône `check_circle`.</li>
 *   <li>⚠️ <b>Points non conformes signalés</b> (NON_COMPLIANT_FLAG) —
 *   border-left rouge subtil, icône `warning`, libellé + raison.</li>
 *   <li>⏳ <b>Points à vérifier</b> (TO_VERIFY_FLAG) — border-left gris,
 *   icône `help_outline`, libellé.</li>
 * </ul>
 *
 * <p>Si la liste reçue est vide ou ne contient aucun statut affichable
 * (DIVERGENT / NOT_ANALYZED / NO_TARGET_TOOL ignorés ici), le composant ne
 * rend rien.</p>
 *
 * <p>Pattern : composant partagé (vs duplication HTML dans chaque outil)
 * pour garantir la cohérence visuelle DESIGN_SYSTEM.md sur les 10 outils
 * impactés et faciliter les évolutions ultérieures.</p>
 */
@Component({
  selector: 'app-procedure-checks-output',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './procedure-checks-output.component.html',
  styleUrls: ['./procedure-checks-output.component.scss'],
})
export class ProcedureChecksOutputComponent {
  private readonly alignmentSignal = signal<ProcedureCheckAlignment[]>([]);

  @Input()
  set alignment(value: ProcedureCheckAlignment[] | null | undefined) {
    this.alignmentSignal.set(Array.isArray(value) ? value : []);
  }

  protected readonly aligned = computed(() =>
    this.alignmentSignal().filter(c => c.matchStatus === 'ALIGNED'),
  );
  protected readonly nonCompliant = computed(() =>
    this.alignmentSignal().filter(c => c.matchStatus === 'NON_COMPLIANT_FLAG'),
  );
  protected readonly toVerify = computed(() =>
    this.alignmentSignal().filter(c => c.matchStatus === 'TO_VERIFY_FLAG'),
  );

  protected readonly hasAny = computed(() =>
    this.aligned().length + this.nonCompliant().length + this.toVerify().length > 0,
  );
}
