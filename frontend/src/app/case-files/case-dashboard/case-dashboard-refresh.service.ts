import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

/**
 * Bus d'événements local à une fiche dossier pour notifier le dashboard F-IA-02
 * qu'un outil métier vient de valider une action (Résoudre / Calculer /
 * Enregistrer) avec succès et que ses cards doivent être rechargées.
 *
 * Doit être fourni par `CaseFileDetailComponent` via `providers: []` — une
 * instance par fiche ouverte, pas au niveau root.
 */
@Injectable()
export class CaseDashboardRefreshService {
  private readonly subject = new Subject<void>();
  readonly refresh$: Observable<void> = this.subject.asObservable();

  triggerRefresh(): void {
    this.subject.next();
  }
}
