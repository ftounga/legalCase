import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TimeReportComponent } from './time-report.component';
import { TimeReportService } from '../../core/services/time-report.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { TimeReportRow } from '../../core/models/time-tracking.models';

const mockRow: TimeReportRow = {
  caseFileId: 'cf-1',
  caseFileTitle: 'Dossier Dupont',
  userId: 'u-1',
  userEmail: "alice@test.com",
  totalSeconds: 3661,
  ratePerHour: 200,
  amount: 203.39
};

describe('TimeReportComponent', () => {
  let component: TimeReportComponent;
  let fixture: ComponentFixture<TimeReportComponent>;
  let reportService: jest.Mocked<TimeReportService>;
  let snackBar: jest.Mocked<MatSnackBar>;

  beforeEach(async () => {
    const reportSpy = {
      getReport: jest.fn().mockReturnValue(of([])),
      exportCsv: jest.fn().mockReturnValue(of(new Blob([''], { type: 'text/csv' })))
    } as unknown as jest.Mocked<TimeReportService>;

    const snackSpy = { open: jest.fn() } as unknown as jest.Mocked<MatSnackBar>;

    await TestBed.configureTestingModule({
      imports: [TimeReportComponent, NoopAnimationsModule],
      providers: [
        { provide: TimeReportService, useValue: reportSpy },
        { provide: MatSnackBar, useValue: snackSpy }
      ]
    }).compileComponents();

    reportService = TestBed.inject(TimeReportService) as jest.Mocked<TimeReportService>;
    snackBar = TestBed.inject(MatSnackBar) as jest.Mocked<MatSnackBar>;
    fixture = TestBed.createComponent(TimeReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('devrait créer le composant', () => {
    expect(component).toBeTruthy();
  });

  it('charge le rapport au ngOnInit', () => {
    expect(reportService.getReport).toHaveBeenCalledWith(component.months[0].value);
  });

  it('affiche les lignes du rapport', () => {
    reportService.getReport.mockReturnValue(of([mockRow]));
    component.load();
    fixture.detectChanges();
    expect(component.rows()).toHaveLength(1);
    expect(component.rows()[0].caseFileTitle).toBe('Dossier Dupont');
  });

  it('affiche le message vide si aucune entrée', () => {
    expect(component.rows()).toHaveLength(0);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.empty-state')).toBeTruthy();
  });

  it('affiche une erreur si getReport() échoue', () => {
    reportService.getReport.mockReturnValue(throwError(() => new Error('500')));
    component.load();
    fixture.detectChanges();
    expect(component.loadError()).toBe(true);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.error-state')).toBeTruthy();
  });

  it('onMonthChange() recharge le rapport', () => {
    reportService.getReport.mockReturnValue(of([mockRow]));
    component.selectedMonth.set('2026-03');
    component.onMonthChange();
    expect(reportService.getReport).toHaveBeenCalledWith('2026-03');
  });

  it('exportCsv() appelle le service et affiche le snackbar succès', () => {
    reportService.getReport.mockReturnValue(of([mockRow]));
    component.load();
    component.exportCsv();
    expect(reportService.exportCsv).toHaveBeenCalledWith(component.selectedMonth());
    expect(snackBar.open).toHaveBeenCalledWith(
      'Export terminé', 'Fermer', expect.objectContaining({ panelClass: ['snack-success'] })
    );
  });

  it('exportCsv() affiche une erreur en cas d\'échec', () => {
    reportService.exportCsv.mockReturnValue(throwError(() => new Error('500')));
    component.exportCsv();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Erreur lors de l\'export.', 'Fermer', expect.objectContaining({ panelClass: ['snack-error'] })
    );
  });

  describe('formatDuration()', () => {
    it('3661s → "1h 01min"', () => {
      expect(component.formatDuration(3661)).toBe('1h 01min');
    });

    it('90s → "1min"', () => {
      expect(component.formatDuration(90)).toBe('1min');
    });

    it('45s → "45s"', () => {
      expect(component.formatDuration(45)).toBe('45s');
    });

    it('7200s → "2h 00min"', () => {
      expect(component.formatDuration(7200)).toBe('2h 00min');
    });
  });

  it('totalSeconds() somme correctement', () => {
    reportService.getReport.mockReturnValue(of([mockRow, { ...mockRow, totalSeconds: 1800, amount: 100 }]));
    component.load();
    expect(component.totalSeconds()).toBe(3661 + 1800);
  });

  it('totalAmount() somme correctement', () => {
    reportService.getReport.mockReturnValue(of([mockRow, { ...mockRow, totalSeconds: 1800, amount: 100 }]));
    component.load();
    expect(component.totalAmount()).toBeCloseTo(303.39, 1);
  });

  it('buildMonths() génère 13 entrées (mois courant + 12 en arrière)', () => {
    expect(component.months).toHaveLength(13);
  });
});
