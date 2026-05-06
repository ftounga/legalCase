import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';

import { TypeLitigeOverrideDialogComponent, TypeLitigeOverrideDialogData } from './type-litige-override-dialog.component';
import { TypeLitigeOverrideService } from '../../../core/services/type-litige-override.service';
import { TypeLitigeOverrideResponse } from '../../../core/models/type-litige-override.model';

describe('TypeLitigeOverrideDialogComponent — F-197 SF-197-02', () => {
  let fixture: ComponentFixture<TypeLitigeOverrideDialogComponent>;
  let component: TypeLitigeOverrideDialogComponent;
  let dialogRefMock: { close: jest.Mock };
  let serviceMock: { update: jest.Mock; getForCaseFile: jest.Mock };
  let snackBarMock: { open: jest.Mock };

  const setup = async (data: TypeLitigeOverrideDialogData) => {
    dialogRefMock = { close: jest.fn() };
    serviceMock = {
      update: jest.fn().mockReturnValue(of({ typeLitigeAvocat: data.domain === 'TRAVAIL_FR' ? 'LICENCIEMENT_ECONOMIQUE' : null, typeProcedureAvocat: null, raison: null } as TypeLitigeOverrideResponse)),
      getForCaseFile: jest.fn().mockReturnValue(of({})),
    };
    snackBarMock = { open: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [TypeLitigeOverrideDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: TypeLitigeOverrideService, useValue: serviceMock },
        { provide: MatSnackBar, useValue: snackBarMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TypeLitigeOverrideDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  it('Travail FR — expose les 7 valeurs enum', async () => {
    await setup({ caseFileId: 'cf-1', domain: 'TRAVAIL_FR' });
    expect(component.options.map(o => o.value)).toEqual([
      'LICENCIEMENT_SANS_CAUSE_REELLE',
      'LICENCIEMENT_ECONOMIQUE',
      'PRISE_ACTE_RUPTURE',
      'HARCELEMENT_MORAL',
      'DISCRIMINATION',
      'HEURES_SUPPLEMENTAIRES',
      'RAPPEL_SALAIRE',
    ]);
  });

  it('Immigration — expose les 10 valeurs enum', async () => {
    await setup({ caseFileId: 'cf-1', domain: 'IMMIGRATION' });
    expect(component.options.length).toBe(10);
    expect(component.options.map(o => o.value)).toContain('OQTF_SANS_DELAI');
    expect(component.options.map(o => o.value)).toContain('NATURALISATION_DECRET');
  });

  it('canSave() — false par défaut (rien sélectionné)', async () => {
    await setup({ caseFileId: 'cf-1', domain: 'TRAVAIL_FR' });
    expect(component.canSave()).toBe(false);
  });

  it('canSave() — true après sélection d\'un code', async () => {
    await setup({ caseFileId: 'cf-1', domain: 'TRAVAIL_FR' });
    component.onCodeChange('LICENCIEMENT_ECONOMIQUE');
    expect(component.canSave()).toBe(true);
  });

  it('cancel() — ferme le dialog avec undefined (pas de PUT)', async () => {
    await setup({ caseFileId: 'cf-1', domain: 'TRAVAIL_FR' });
    component.cancel();
    expect(dialogRefMock.close).toHaveBeenCalledWith(undefined);
    expect(serviceMock.update).not.toHaveBeenCalled();
  });

  it('save() — Travail FR : appel PUT avec code + raison trimmée, ferme avec response', async () => {
    await setup({ caseFileId: 'cf-1', domain: 'TRAVAIL_FR' });
    component.onCodeChange('LICENCIEMENT_ECONOMIQUE');
    component.onRaisonInput('  Motif évident   ');
    component.save();
    expect(serviceMock.update).toHaveBeenCalledWith('cf-1', {
      type: 'LICENCIEMENT_ECONOMIQUE',
      raison: 'Motif évident',
    });
    expect(snackBarMock.open).toHaveBeenCalledWith(
      'Type de litige enregistré', 'Fermer',
      expect.objectContaining({ panelClass: ['snack-success'] }),
    );
    expect(dialogRefMock.close).toHaveBeenCalledTimes(1);
  });

  it('save() — raison vide → null envoyé au backend', async () => {
    await setup({ caseFileId: 'cf-1', domain: 'TRAVAIL_FR' });
    component.onCodeChange('HARCELEMENT_MORAL');
    component.onRaisonInput('   ');
    component.save();
    expect(serviceMock.update).toHaveBeenCalledWith('cf-1', {
      type: 'HARCELEMENT_MORAL',
      raison: null,
    });
  });

  it('save() — erreur backend : snackbar erreur, dialog reste ouvert', async () => {
    await setup({ caseFileId: 'cf-1', domain: 'TRAVAIL_FR' });
    serviceMock.update.mockReturnValue(throwError(() => ({ status: 500 })));
    component.onCodeChange('DISCRIMINATION');
    component.save();
    expect(snackBarMock.open).toHaveBeenCalledWith(
      expect.stringContaining('Impossible'),
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] }),
    );
    // Dialog NON fermé (CA-erreur SF-197-02)
    expect(dialogRefMock.close).not.toHaveBeenCalled();
    // saving remis à false pour permettre une nouvelle tentative
    expect(component.saving()).toBe(false);
  });

  it('pré-sélection — override Travail FR existant : code + raison pré-remplis', async () => {
    await setup({
      caseFileId: 'cf-1',
      domain: 'TRAVAIL_FR',
      current: {
        typeLitigeAvocat: 'PRISE_ACTE_RUPTURE',
        typeProcedureAvocat: null,
        raison: 'Test stratégique',
      },
    });
    expect(component.selectedCode()).toBe('PRISE_ACTE_RUPTURE');
    expect(component.raison()).toBe('Test stratégique');
  });

  it('pré-sélection — override Immigration existant', async () => {
    await setup({
      caseFileId: 'cf-1',
      domain: 'IMMIGRATION',
      current: {
        typeLitigeAvocat: null,
        typeProcedureAvocat: 'OQTF_SANS_DELAI',
        raison: null,
      },
    });
    expect(component.selectedCode()).toBe('OQTF_SANS_DELAI');
    expect(component.raison()).toBe('');
  });

  it('iaDetectedLabel — affiche le libellé FR du code IA détecté', async () => {
    await setup({
      caseFileId: 'cf-1',
      domain: 'TRAVAIL_FR',
      iaDetectedCode: 'LICENCIEMENT_SANS_CAUSE_REELLE',
    });
    expect(component.iaDetectedLabel).toBe('Licenciement sans cause réelle et sérieuse');
  });

  it('iaDetectedLabel — null si code IA inconnu (rétro-compat)', async () => {
    await setup({
      caseFileId: 'cf-1',
      domain: 'TRAVAIL_FR',
      iaDetectedCode: 'CODE_INCONNU' as any,
    });
    expect(component.iaDetectedLabel).toBeNull();
  });

  it('save() — désactive le bouton pendant le PUT (canSave=false tant que saving=true)', async () => {
    await setup({ caseFileId: 'cf-1', domain: 'TRAVAIL_FR' });
    component.onCodeChange('LICENCIEMENT_ECONOMIQUE');
    let observer: any;
    serviceMock.update.mockReturnValue({
      subscribe: (obs: any) => { observer = obs; return { unsubscribe: () => {} }; },
    });
    component.save();
    expect(component.saving()).toBe(true);
    expect(component.canSave()).toBe(false);
    // simule la fin du PUT
    observer.next({ typeLitigeAvocat: 'LICENCIEMENT_ECONOMIQUE', typeProcedureAvocat: null, raison: null });
    expect(component.saving()).toBe(false);
  });
});
