import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { AuditLogScreenComponent } from './audit-log-screen.component';
import { AuditLogService, AuditLogPage } from '../../core/services/audit-log.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuditLogEntry } from '../../core/models/audit-log-entry.model';
import { provideRouter } from '@angular/router';
import { PageEvent } from '@angular/material/paginator';

const mockEntries: AuditLogEntry[] = [
  { id: 'log-1', action: 'DOCUMENT_DELETED', userEmail: 'alice@test.com', caseFileId: 'cf-1', caseFileTitle: 'Licenciement Dupont', documentName: 'contrat.pdf', createdAt: '2026-03-24T10:00:00Z' },
  { id: 'log-2', action: 'DOCUMENT_DELETED', userEmail: 'bob@test.com', caseFileId: 'cf-2', caseFileTitle: 'Harcèlement Martin', documentName: 'mail.pdf', createdAt: '2026-03-25T09:00:00Z' }
];

function mockPage(entries: AuditLogEntry[], total = entries.length): AuditLogPage {
  return { content: entries, totalElements: total, totalPages: Math.ceil(total / 20), size: 20, number: 0 };
}

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

  // T-01 : chargement nominal — logs affichés, totalElements mis à jour
  it('T-01: affiche les logs et met à jour totalElements au chargement nominal', fakeAsync(async () => {
    await setup(of(mockPage(mockEntries, 42)));

    expect(component.loading()).toBeFalse();
    expect(component.logs().length).toBe(2);
    expect(component.totalElements()).toBe(42);
    expect(fixture.nativeElement.textContent).toContain('alice@test.com');
    expect(fixture.nativeElement.textContent).toContain('bob@test.com');
  }));

  // T-02 : liste vide → message "Aucune action enregistrée"
  it('T-02: affiche le message si liste vide', fakeAsync(async () => {
    await setup(of(mockPage([])));

    expect(component.logs().length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('Aucune action enregistrée');
  }));

  // T-03 : 403 → accessDenied = true
  it('T-03: affiche le message accès refusé si 403', fakeAsync(async () => {
    await setup(throwError(() => ({ status: 403 })));

    expect(component.accessDenied()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Accès réservé');
  }));

  // T-04 : erreur 500 → snackbar erreur
  it('T-04: affiche un snackbar si erreur serveur', fakeAsync(async () => {
    await setup(throwError(() => ({ status: 500 })));

    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining('Erreur'), jasmine.any(String), jasmine.any(Object)
    );
    expect(component.accessDenied()).toBeFalse();
  }));

  // T-05 : from > to → snackbar "Dates invalides"
  it('T-05: from > to retourne une snackbar Dates invalides', fakeAsync(async () => {
    await setup(of(mockPage(mockEntries)));
    auditLogService.getAuditLogs.and.returnValue(throwError(() => ({ status: 400 })));

    component.dateFrom.set('2026-03-31');
    component.dateTo.set('2026-03-01');
    component.loadLogs();
    tick();

    expect(snackBar.open).toHaveBeenCalledWith('Dates invalides.', 'Fermer', jasmine.any(Object));
  }));

  // T-06 : changer dateFrom recharge la page 0
  it('T-06: changer dateFrom recharge les logs sur la page 0', fakeAsync(async () => {
    await setup(of(mockPage(mockEntries)));
    component.pageIndex.set(2);
    auditLogService.getAuditLogs.calls.reset();
    auditLogService.getAuditLogs.and.returnValue(of(mockPage([])));

    component.dateFrom.set('2026-03-01');
    component.onDateChange();
    tick();

    expect(component.pageIndex()).toBe(0);
    expect(auditLogService.getAuditLogs).toHaveBeenCalledWith(
      jasmine.stringMatching(/^2026-03-01/), undefined, 0, 20
    );
  }));

  // T-07 : PageEvent → appel avec page et size corrects
  it('T-07: PageEvent déclenche loadLogs avec les bons paramètres', fakeAsync(async () => {
    await setup(of(mockPage(mockEntries)));
    auditLogService.getAuditLogs.calls.reset();
    auditLogService.getAuditLogs.and.returnValue(of(mockPage(mockEntries)));

    const event: PageEvent = { pageIndex: 1, pageSize: 10, length: 42 };
    component.onPageChange(event);
    tick();

    expect(auditLogService.getAuditLogs).toHaveBeenCalledWith(undefined, undefined, 1, 10);
  }));

  // T-08 : exportCsv — appelle auditLogService.exportCsv()
  it('T-08: exportCsv appelle le service et remet exporting à false', fakeAsync(async () => {
    const blob = new Blob(['csv'], { type: 'text/csv' });
    await setup(of(mockPage(mockEntries)), of(blob));

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

  // T-09 : exportCsv erreur → snackbar
  it('T-09: exportCsv affiche un snackbar en cas d\'erreur', fakeAsync(async () => {
    await setup(of(mockPage(mockEntries)), throwError(() => new Error('network error')));

    component.exportCsv();
    tick();

    expect(snackBar.open).toHaveBeenCalledWith(
      jasmine.stringContaining("Erreur lors de l'export"), jasmine.any(String), jasmine.any(Object)
    );
    expect(component.exporting()).toBeFalse();
  }));
});
