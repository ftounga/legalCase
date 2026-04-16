import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { ActionType } from '../models/source-explanation.model';

/**
 * Dispatch de navigation vers la source d'une incohérence (SF-IA-03-15a).
 * Les routes cibles supportent des query params pour le scroll + highlight
 * (pattern SF-IA-03-12 pour Q&A et F-96).
 */
@Injectable({ providedIn: 'root' })
export class CoherenceSourceNavigator {
  constructor(private router: Router) {}

  navigate(caseFileId: string, actionType: ActionType, actionTarget: string | null): void {
    switch (actionType) {
      case 'OPEN_DOCUMENT':
        if (actionTarget) {
          this.router.navigate(['/case-files', caseFileId, 'documents', actionTarget]);
        }
        break;
      case 'SCROLL_QA':
        if (actionTarget) {
          this.router.navigate(['/case-files', caseFileId, 'synthesis'], { queryParams: { qa: actionTarget } });
        }
        break;
      case 'SCROLL_F96':
        if (actionTarget) {
          this.router.navigate(['/case-files', caseFileId, 'synthesis'], { queryParams: { check: actionTarget } });
        }
        break;
      case 'OPEN_CHAT':
        if (actionTarget) {
          this.router.navigate(['/case-files', caseFileId, 'synthesis'], { queryParams: { chat: actionTarget } });
        }
        break;
      case 'MISSING_PIECE':
        this.router.navigate(['/case-files', caseFileId, 'synthesis'], { queryParams: { section: 'pieces' } });
        break;
      case 'NONE':
      default:
        break;
    }
  }
}
