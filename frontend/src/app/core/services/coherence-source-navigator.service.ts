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
      case 'OPEN_DOCUMENTS_LIST':
        this.router.navigate(['/case-files', caseFileId], { queryParams: { section: 'documents' } });
        break;
      case 'SCROLL_QA':
        if (actionTarget) {
          this.router.navigate(['/case-files', caseFileId, 'synthesis'], { queryParams: { qa: actionTarget } });
        }
        break;
      case 'OPEN_QUESTIONS':
        this.router.navigate(['/case-files', caseFileId, 'synthesis'], { queryParams: { section: 'questions' } });
        break;
      case 'SCROLL_F96':
        if (actionTarget) {
          this.router.navigate(['/case-files', caseFileId, 'synthesis'], { queryParams: { check: actionTarget } });
        }
        break;
      case 'OPEN_F96_LIST':
        this.router.navigate(['/case-files', caseFileId, 'synthesis'], { queryParams: { section: 'checklist' } });
        break;
      case 'OPEN_CHAT':
        this.router.navigate(['/case-files', caseFileId, 'synthesis'], {
          queryParams: actionTarget ? { chat: actionTarget } : { section: 'chat' },
        });
        break;
      case 'OPEN_MISSING_PIECES':
        this.router.navigate(['/case-files', caseFileId, 'synthesis'], {
          queryParams: actionTarget != null
            ? { section: 'pieces', piece: actionTarget }
            : { section: 'pieces' },
        });
        break;
      case 'NONE':
      default:
        break;
    }
  }
}
