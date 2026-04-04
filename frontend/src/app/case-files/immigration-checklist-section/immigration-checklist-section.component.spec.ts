import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { ImmigrationChecklistSectionComponent } from './immigration-checklist-section.component';
import { ImmigrationChecklistService } from '../../core/services/immigration-checklist.service';
import { ImmigrationChecklist } from '../../core/models/immigration-checklist.model';
import { PdfExportService } from '../../core/services/pdf-export.service';

function makeChecklist(pieces: { label: string; statut: string }[] = []): ImmigrationChecklist {
  return {
    caseFileId: 'cf-1',
    titreType: 'VISA_ETUDIANT',
    country: 'FRANCE',
    pieces: pieces as ImmigrationChecklist['pieces'],
  };
}

describe('ImmigrationChecklistSectionComponent', () => {
  let fixture: ComponentFixture<ImmigrationChecklistSectionComponent>;
  let component: ImmigrationChecklistSectionComponent;
  let serviceSpy: jest.Mocked<ImmigrationChecklistService>;
  let snackBarSpy: { open: jest.Mock };
  let pdfExportSpy: { exportImmigrationChecklist: jest.Mock };

  beforeEach(async () => {
    serviceSpy = {
      get: jest.fn().mockReturnValue(of(makeChecklist([
        { label: 'Passeport', statut: 'INCONNU' },
        { label: 'Justificatif hébergement', statut: 'PRESENT' },
      ]))),
      upsert: jest.fn().mockReturnValue(of(makeChecklist())),
    } as unknown as jest.Mocked<ImmigrationChecklistService>;

    snackBarSpy = { open: jest.fn() };
    pdfExportSpy = { exportImmigrationChecklist: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [ImmigrationChecklistSectionComponent, NoopAnimationsModule],
      providers: [
        { provide: ImmigrationChecklistService, useValue: serviceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: PdfExportService, useValue: pdfExportSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ImmigrationChecklistSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'cf-1';
    fixture.detectChanges();
  });

  // TC-IM02-01 : init → GET appelé avec valeurs par défaut
  it('ngOnInit calls GET with default titreType and country', () => {
    expect(serviceSpy.get).toHaveBeenCalledWith('cf-1', 'VISA_ETUDIANT', 'FRANCE');
    expect(component.checklist()).not.toBeNull();
  });

  // TC-IM02-02 : badge = nb pièces PRESENT
  it('presentCount returns number of PRESENT pieces', () => {
    expect(component.presentCount()).toBe(1);
  });

  // TC-IM02-03 : cycleStatut INCONNU → PRESENT → ABSENT → INCONNU
  it('cycleStatut cycles through statuts correctly', () => {
    const piece = component.checklist()!.pieces[0]; // INCONNU
    component.cycleStatut(piece);
    expect(piece.statut).toBe('PRESENT');
    component.cycleStatut(piece);
    expect(piece.statut).toBe('ABSENT');
    component.cycleStatut(piece);
    expect(piece.statut).toBe('INCONNU');
  });

  // TC-IM02-04 : save() → PUT appelé → snackbar succès
  it('save calls upsert and shows success snackbar', () => {
    component.save();
    expect(serviceSpy.upsert).toHaveBeenCalledWith('cf-1', expect.objectContaining({
      titreType: 'VISA_ETUDIANT',
      country: 'FRANCE',
    }));
    expect(snackBarSpy.open).toHaveBeenCalledWith('Checklist enregistrée', undefined, expect.any(Object));
  });

  // TC-IM02-05 : erreur GET → snackbar erreur
  it('load error shows error snackbar', () => {
    serviceSpy.get.mockReturnValue(throwError(() => new Error('network')));
    component.load();
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      expect.stringContaining('Erreur'), 'Fermer', expect.any(Object)
    );
  });

  // TC-IM02-06 : erreur PUT → snackbar erreur, saving reset
  it('save error shows error snackbar and resets saving', () => {
    serviceSpy.upsert.mockReturnValue(throwError(() => new Error('network')));
    component.save();
    expect(component.saving()).toBe(false);
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      expect.stringContaining('Erreur'), 'Fermer', expect.any(Object)
    );
  });

  // TC-IM02-07 : changement de sélecteur → GET rappelé
  it('onSelectorChange triggers load', () => {
    serviceSpy.get.mockClear();
    component.titreType.set('NATURALISATION');
    component.onSelectorChange();
    expect(serviceSpy.get).toHaveBeenCalledWith('cf-1', 'NATURALISATION', 'FRANCE');
  });

  // TC-IM03-01 : bouton export désactivé si checklist null
  it('exportPdf button disabled when checklist is null', () => {
    component.collapsed.set(false);
    component.checklist.set(null);
    fixture.detectChanges();
    const buttons = fixture.nativeElement.querySelectorAll('button');
    const exportBtn = Array.from(buttons).find((b: any) => b.textContent?.includes('Exporter PDF')) as HTMLButtonElement | undefined;
    expect(exportBtn).toBeDefined();
    expect(exportBtn!.disabled).toBe(true);
  });

  // TC-IM03-02 : clic bouton export → appel exportImmigrationChecklist
  it('exportPdf button click calls exportImmigrationChecklist', () => {
    component.caseFileTitle = 'Mon Dossier';
    fixture.detectChanges();
    component.exportPdf();
    expect(pdfExportSpy.exportImmigrationChecklist).toHaveBeenCalledWith(
      expect.objectContaining({ caseFileId: 'cf-1' }),
      'Mon Dossier'
    );
  });
});
