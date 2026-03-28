import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { AuditLogScreenComponent } from './audit-log-screen.component';
import { AuditLogService } from '../../core/services/audit-log.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuditLogEntry } from '../../core/models/audit-log-entry.model';
import { provideRouter } from '@angular/router';

const mockLogs: AuditLogEntry[] = [
  { id: 'log-1', action: 'DOCUMENT_DELETED', userEmail: 'alice@test.com', caseFileId: 'cf-1', caseFileTitle: 'Licenciement Dupont', documentName: 'contrat.pdf', createdAt: '2026-03-24T10:00:00Z' },
  { id: 'log-2', action: 'DOCUMENT_DELETED', userEmail: 'bob@test.com', caseFileId: 'cf-2', caseFileTitle: 'Harcèlement Martin', documentName: 'mail.pdf', createdAt: '2026-03-25T09:00:00Z' }
];

describe('AuditLogScreenComponent', () => {
  let component: AuditLogScreenComponent;
  let fixture: ComponentFixture<AuditLogScreenComponent>;
  let auditLogService: jasmine.SpyObj<AuditLogService>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  async function setup(logsReturn: any, exportReturn?: any) {
    auditLogService = jasmine.createSpyObj('AuditLogService', ['getAuditLogs', 'exportCsv']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);
    auditLogService.getAuditLogs.and.returnValue(logsReturn);
    if (exportReturn) auditLogService.exportCsv.and.returnValue(exportReturn);

    await TestBed.configureTestingModule({
      imports: [AuditLogScreenComponent, NoopAnimationsModule],
      providers: [
        { provide: AuditLogService, useValue: auditLogService },
        { provide: MatSnackBar, useValue: snackBar },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditLogScreenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  // T-01 : chargement nominal — tous les logs affichés
  it('affiche tous les logs au chargement nominal', fakeAsync(async () => {
    await setup(of(mockLogs));

    expect(component.loading()).toBeFalse();
    expect(component.allLogs().length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('alice@test.com');
    expect(fixture.nativeElement.textContent).toContain('bob@test.com');
  }));

  // T-02 : filtre texte "dupont" → une seule ligne
  it('filtre par texte sur caseFileTitle', fakeAsync(async () => {
    await setup(of(mockLogs));

    component.searchText.set('dupont');
    fixture.detectChanges();

    expect(component.filteredLogs().length).toBe(1);
    expect(component.filteredLogs()[0].caseFileTitle).toBe('Licenciement Dupont');
  }));

  // T-03 : filtre texte sur email
  it('filtre par texte sur userEmail', fakeAsync(async () => {
    await setup(of(mockLogs));

    component.searchText.set('bob');
    fixture.detectChanges();

    expect(component.filteredLogs().length).toBe(1);
    expect(component.filteredLogs()[0].userEmail).toBe('bob@test.com');
  }));

  // T-04 : filtre texte sur documentName
  it('filtre par texte sur documentName', fakeAsync(async () => {
    await setup(of(mockLogs));

    component.searchText.set('contrat');
    fixture.detectChanges();

    expect(component.filteredLogs().length).toBe(1);
    expect(component.filteredLogs()[0].documentName).toBe('contrat.pdf');
  }));

  // T-05 : filtre action "DOCUMENT_DELETED" → tous visibles (seul type existant)
  it('filtre par action DOCUMENT_DELETED affiche toutes les lignes correspondantes', fakeAsync(async () => {
    await setup(of(mockLogs));

    component.actionFilter.set('DOCUMENT_DELETED');
    fixture.detectChanges();

    expect(component.filteredLogs().length).toBe(2);
  }));

  // T-06 : liste vide → message "Aucune action enregistrée"
  it('affiche le message si liste vide', fakeAsync(async () => {
    await setup(of([]));

    expect(component.filteredLogs().length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Aucune action enregistrée');
  }));

  // T-07 : 403 → accessDenied = true
  it('affiche le message accès refusé si 403', fakeAsync(async () => {
    await setup(throwError(() => ({ status: 403 })));

    expect(component.accessDenied()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Accès réservé');
  }));

  // T-08 : erreur 500 → snackbar erreur
  it('affiche un snackbar si erreur serveur', fakeAsync(async () => {
    await setup(throwError(() => ({ status: 500 })));

    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Erreur'), jasmine.any(String), jasmine.any(Object)
    );
    expect(component.accessDenied()).toBeFalse();
  }));

  // T-12 : sélectionner une date from recharge les logs
  it('T-12: changer dateFrom recharge les logs', fakeAsync(async () => {
    await setup(of(mockLogs));
    auditLogService.getAuditLogs.calls.reset();
    auditLogService.getAuditLogs.and.returnValue(of([]));

    component.dateFrom.set('2026-03-01');
    component.loadLogs();
    tick();

    expect(auditLogService.getAuditLogs).toHaveBeenCalledWith(
      jasmine.stringMatching(/^2026-03-01/), undefined
    );
  }));

  // T-13 : from > to → snackbar "Dates invalides"
  it('T-13: from > to retourne une snackbar Dates invalides', fakeAsync(async () => {
    await setup(of(mockLogs), undefined);
    auditLogService.getAuditLogs.and.returnValue(throwError(() => ({ status: 400 })));

    component.dateFrom.set('2026-03-31');
    component.dateTo.set('2026-03-01');
    component.loadLogs();
    tick();

    expect(snackBar.open).toHaveBeenCalledWith('Dates invalides.', 'Fermer', jasmine.any(Object));
  }));

  // T-14 : vider dateFrom recharge sans ce paramètre
  it('T-14: vider dateFrom recharge sans paramètre from', fakeAsync(async () => {
    await setup(of(mockLogs));
    component.dateFrom.set('2026-03-01');
    tick();
    auditLogService.getAuditLogs.calls.reset();
    auditLogService.getAuditLogs.and.returnValue(of(mockLogs));

    component.dateFrom.set('');
    component.loadLogs();
    tick();

    expect(auditLogService.getAuditLogs).toHaveBeenCalledWith(undefined, undefined);
  }));

  // T-09 : exportCsv — appelle auditLogService.exportCsv()
  it('T-09: exportCsv appelle le service et remet exporting à false', fakeAsync(async () => {
    const blob = new Blob(['csv'], { type: 'text/csv' });
    await setup(of(mockLogs), of(blob));

    // mock anchor click to avoid JSDOM error
    spyOn(document, 'createElement').and.callFake((tag: string) => {
      if (tag === 'a') {
        const a = jasmine.createSpyObj('a', ['click']);
        (a as any).href = '';
        (a as any).download = '';
        return a;
      }
      return document.createElement(tag);
    });

    component.exportCsv();
    tick();

    expect(auditLogService.exportCsv).toHaveBeenCalled();
    expect(component.exporting()).toBeFalse();
  }));

  // T-10 : exportCsv désactivé pendant l'export
  it('T-10: exporting passe à true pendant l\'export', fakeAsync(async () => {
    const blob = new Blob(['csv'], { type: 'text/csv' });
    await setup(of(mockLogs), of(blob));

    // before call: false
    expect(component.exporting()).toBeFalse();
    // after call (sync): resolves immediately with of(blob), so exporting goes back to false
    component.exportCsv();
    tick();
    expect(component.exporting()).toBeFalse();
  }));

  // T-11 : exportCsv erreur → snackbar
  it('T-11: exportCsv affiche un snackbar en cas d\'erreur', fakeAsync(async () => {
    await setup(of(mockLogs), throwError(() => new Error('network error')));

    component.exportCsv();
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining("Erreur lors de l'export"), jasmine.any(String), jasmine.any(Object)
    );
    expect(component.exporting()).toBeFalse();
  }));
});
