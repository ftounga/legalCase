import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import { RetainedPisteAlignmentService } from '../../core/services/retained-piste-alignment.service';
import { PieceManquanteAlignmentService } from '../../core/services/piece-manquante-alignment.service';
import { RisqueAlignmentService } from '../../core/services/risque-alignment.service';
import { AiQuestionAlignmentService } from '../../core/services/ai-question-alignment.service';

import { RetainedPisteAlignment } from '../../core/models/retained-piste-alignment.model';
import { PieceManquanteAlignment } from '../../core/models/piece-manquante-alignment.model';
import { RisqueAlignment } from '../../core/models/risque-alignment.model';
import { AiQuestionAlignment } from '../../core/models/ai-question-alignment.model';

/**
 * F-228 SF-228-01 — Bundle des 4 alignements IA ↔ outils décisionnels
 * consommé par le ctx de `entry.inputs(ctx)` côté `<app-case-dashboard>` et
 * `<app-decisional-tools-panel>` (TOOL_REGISTRY).
 *
 * <p>Chaque champ est un tableau "déjà fail-open" — vide par défaut si le
 * service backend correspondant est en erreur, en timeout ou absent. Les
 * consommateurs filtrent ensuite par `toolId` au moment de l'instanciation
 * du composant outil (pattern miroir
 * `decisional-tools-panel.component.ts:670-683`).</p>
 */
export interface DecisionToolAlignments {
  /** F-176 / F-192 — pistes 🟢 RETAINED associées aux outils. */
  retainedPistes: RetainedPisteAlignment[];
  /** F-194 — alignement matérialisé pièces ↔ outils (statuts décidés par l'avocat). */
  piecesAlignment: PieceManquanteAlignment[];
  /** F-195 — alignement matérialisé risques ↔ outils. */
  risquesAlignment: RisqueAlignment[];
  /** F-196 — alignement questions complémentaires ↔ outils. */
  aiQuestionsAlignment: AiQuestionAlignment[];
}

/**
 * F-228 SF-228-01 — Loader partagé qui charge les 4 alignements en parallèle
 * via `forkJoin`. Un échec sur l'un des streams ne bloque pas les 3 autres
 * (fail-open par stream → tableau vide).
 *
 * <p>Pattern miroir des 4 services (F-176 / F-194 / F-195 / F-196) :
 * chaque service implémente déjà son propre fail-open (timeout 5 s + 4xx/5xx
 * → `[]`). Cette double protection (service + loader) est volontaire — le
 * loader est utilisé par 2 consommateurs distincts (panel F-IA-04 et
 * case-dashboard F-167) et doit garantir l'invariant
 * "1 stream HS = 3 streams OK" indépendamment du contrat de chaque service.</p>
 *
 * <p><strong>Décision V1</strong> : pas de cache croisé entre case-dashboard
 * et panel — chaque composant charge ses données indépendamment (~50 ms
 * supplémentaires acceptables ; cache croisé = V2 si signal terrain).
 * Cf. mini-spec F-228 SF-228-01 §Périmètre / Hors scope V1 (b).</p>
 */
@Injectable({ providedIn: 'root' })
export class DecisionToolAlignmentsLoader {
  private readonly retainedPistesService = inject(RetainedPisteAlignmentService);
  private readonly piecesService = inject(PieceManquanteAlignmentService);
  private readonly risquesService = inject(RisqueAlignmentService);
  private readonly questionsService = inject(AiQuestionAlignmentService);

  /**
   * Charge les 4 alignements en parallèle. Aucun timeout supplémentaire ici :
   * chaque service applique déjà `timeout(5000)` + `catchError(of([]))`.
   *
   * @param caseFileId identifiant du dossier
   * @returns Observable qui émet une seule fois le bundle complet
   */
  loadAll(caseFileId: string): Observable<DecisionToolAlignments> {
    return forkJoin({
      retainedPistes: this.retainedPistesService.getForCaseFile(caseFileId).pipe(
        catchError(() => of([] as RetainedPisteAlignment[])),
      ),
      piecesAlignment: this.piecesService.getForCaseFile(caseFileId).pipe(
        catchError(() => of([] as PieceManquanteAlignment[])),
      ),
      risquesAlignment: this.risquesService.getForCaseFile(caseFileId).pipe(
        catchError(() => of([] as RisqueAlignment[])),
      ),
      aiQuestionsAlignment: this.questionsService.getForCaseFile(caseFileId).pipe(
        catchError(() => of([] as AiQuestionAlignment[])),
      ),
    });
  }
}
