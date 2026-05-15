import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { ProcedureStageSectionComponent } from './procedure-stage-section.component';
import { ProcedureStageService } from '../../core/services/procedure-stage.service';
import {
  ProcedureStage,
  ProcedureStageOptions
} from '../../core/models/procedure-stage.model';

// ── Fixtures ────────────────────────────────────────────────────────────────

function makeOptions(): ProcedureStageOptions {
  return {
    domain: 'DROIT_DU_TRAVAIL',
    country: 'FRANCE',
    jurisdictions: [
      { code: 'CPH', label: "Conseil de prud'hommes" },
      { code: 'CA_SOC', label: "Cour d'appel — chambre sociale" }
    ],
    stages: [
      { code: 'FOND', label: 'Bureau de jugement (fond)', jurisdictionCode: 'CPH' },
      { code: 'REFERE', label: 'Référé prud\'homal', jurisdictionCode: 'CPH' },
      { code: 'APPEL', label: 'Appel', jurisdictionCode: 'CA_SOC' }
    ],
    positions: [
      { code: 'DEMANDEUR', label: 'Demandeur (salarié)', stageCodes: ['FOND', 'REFERE'] },
      { code: 'DEFENDEUR', label: 'Défendeur (employeur)', stageCodes: ['FOND', 'REFERE'] },
      { code: 'APPELANT', label: 'Appelant', stageCodes: ['APPEL'] }
    ]
  };
}

function makeStage(overrides: Partial<ProcedureStage> = {}): ProcedureStage {
  return {
    caseFileId: 'cf-1',
    jurisdiction: null,
    jurisdictionLabel: null,
    stage: null,
    stageLabel: null,
    position: null,
    positionLabel: null,
    ...overrides
  };
}

describe('ProcedureStageSectionComponent', () => {
  let fixture: ComponentFixture<ProcedureStageSectionComponent>;
  let component: ProcedureStageSectionComponent;
  let serviceSpy: jest.Mocked<ProcedureStageService>;
  let snackBarSpy: { open: jest.Mock };

  function setup(): void {
    fixture = TestBed.createComponent(ProcedureStageSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'cf-1';
    component.legalDomain = 'DROIT_DU_TRAVAIL';
    component.workspaceCountry = 'FRANCE';
    fixture.detectChanges();
  }

  beforeEach(async () => {
    serviceSpy = jasmine.createSpyObj('ProcedureStageService',
      ['getOptions', 'get', 'update']);
    serviceSpy.getOptions.mockReturnValue(of(makeOptions()));
    serviceSpy.get.mockReturnValue(of(makeStage()));

    snackBarSpy = { open: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [ProcedureStageSectionComponent, NoopAnimationsModule],
      providers: [
        { provide: ProcedureStageService, useValue: serviceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();
  });

  // ── Chargement initial ────────────────────────────────────────────────────

  it('U-01: charge options et stade courant à l\'init', () => {
    setup();
    expect(serviceSpy.getOptions).toHaveBeenCalledWith('DROIT_DU_TRAVAIL', 'FRANCE');
    expect(serviceSpy.get).toHaveBeenCalledWith('cf-1');
  });

  it('U-02: stade renseigné — affiche les libellés humains', () => {
    serviceSpy.get.mockReturnValue(of(makeStage({
      jurisdiction: 'CPH', jurisdictionLabel: "Conseil de prud'hommes",
      stage: 'FOND', stageLabel: 'Bureau de jugement (fond)',
      position: 'DEMANDEUR', positionLabel: 'Demandeur (salarié)'
    })));
    setup();
    expect(component.hasValue()).toBe(true);
    const display = fixture.nativeElement.querySelector('.ps-display');
    expect(display).toBeTruthy();
    expect(display.textContent).toContain("Conseil de prud'hommes");
    expect(display.textContent).toContain('Bureau de jugement (fond)');
    expect(display.textContent).toContain('Demandeur (salarié)');
  });

  it('U-03: stade non renseigné — affiche l\'état vide', () => {
    setup();
    expect(component.hasValue()).toBe(false);
    expect(fixture.nativeElement.querySelector('.ps-empty')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.ps-display')).toBeNull();
  });

  it('U-04: échec chargement options — encart indisponible + snackbar erreur', () => {
    serviceSpy.getOptions.mockReturnValue(throwError(() => new Error('500')));
    setup();
    expect(component.unavailable()).toBe(true);
    expect(fixture.nativeElement.querySelector('.ps-unavailable')).toBeTruthy();
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      expect.any(String), 'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] })
    );
  });

  it('U-05: échec chargement stade — encart indisponible', () => {
    serviceSpy.get.mockReturnValue(throwError(() => new Error('500')));
    setup();
    expect(component.unavailable()).toBe(true);
  });

  // ── Édition + cascade ─────────────────────────────────────────────────────

  it('U-06: clic Modifier ouvre le formulaire avec 3 sélecteurs', () => {
    setup();
    component.startEdit();
    fixture.detectChanges();
    expect(component.editing()).toBe(true);
    const selects = fixture.nativeElement.querySelectorAll('.ps-select');
    expect(selects.length).toBe(3);
  });

  it('U-07: cascade — sélectionner une juridiction filtre les stades', () => {
    setup();
    component.startEdit();
    component.onJurisdictionChange('CPH');
    const stageCodes = component.availableStages().map(s => s.code);
    expect(stageCodes).toEqual(['FOND', 'REFERE']);
    expect(stageCodes).not.toContain('APPEL');
  });

  it('U-08: cascade — sélectionner un stade filtre les positions', () => {
    setup();
    component.startEdit();
    component.onJurisdictionChange('CPH');
    component.onStageChange('FOND');
    const positionCodes = component.availablePositions().map(p => p.code);
    expect(positionCodes).toEqual(['DEMANDEUR', 'DEFENDEUR']);
    expect(positionCodes).not.toContain('APPELANT');
  });

  it('U-09: changer la juridiction réinitialise stade + position', () => {
    setup();
    component.startEdit();
    component.onJurisdictionChange('CPH');
    component.onStageChange('FOND');
    component.onPositionChange('DEMANDEUR');
    expect(component.formStage()).toBe('FOND');
    expect(component.formPosition()).toBe('DEMANDEUR');

    component.onJurisdictionChange('CA_SOC');
    expect(component.formStage()).toBeNull();
    expect(component.formPosition()).toBeNull();
  });

  it('U-10: changer le stade réinitialise la position', () => {
    setup();
    component.startEdit();
    component.onJurisdictionChange('CPH');
    component.onStageChange('FOND');
    component.onPositionChange('DEMANDEUR');
    expect(component.formPosition()).toBe('DEMANDEUR');

    component.onStageChange('REFERE');
    expect(component.formPosition()).toBeNull();
    expect(component.formStage()).toBe('REFERE');
  });

  // ── Enregistrement ────────────────────────────────────────────────────────

  it('U-11: Enregistrer appelle update() avec la combinaison choisie', () => {
    serviceSpy.update.mockReturnValue(of(makeStage({
      jurisdiction: 'CPH', stage: 'FOND', position: 'DEMANDEUR'
    })));
    setup();
    component.startEdit();
    component.onJurisdictionChange('CPH');
    component.onStageChange('FOND');
    component.onPositionChange('DEMANDEUR');
    component.save();
    expect(serviceSpy.update).toHaveBeenCalledWith('cf-1', {
      jurisdiction: 'CPH', stage: 'FOND', position: 'DEMANDEUR'
    });
  });

  it('U-12: après enregistrement réussi — repasse en affichage', () => {
    serviceSpy.update.mockReturnValue(of(makeStage({
      jurisdiction: 'CPH', jurisdictionLabel: "Conseil de prud'hommes",
      stage: 'FOND', stageLabel: 'Bureau de jugement (fond)'
    })));
    setup();
    component.startEdit();
    component.onJurisdictionChange('CPH');
    component.onStageChange('FOND');
    component.save();
    expect(component.editing()).toBe(false);
    expect(component.current()?.jurisdiction).toBe('CPH');
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Stade procédural enregistré.', 'Fermer',
      expect.objectContaining({ panelClass: ['snack-success'] })
    );
  });

  it('U-13: effacement — Enregistrer avec champs vides envoie null', () => {
    serviceSpy.get.mockReturnValue(of(makeStage({
      jurisdiction: 'CPH', stage: 'FOND', position: 'DEMANDEUR'
    })));
    serviceSpy.update.mockReturnValue(of(makeStage()));
    setup();
    component.startEdit();
    component.onJurisdictionChange('');
    component.save();
    expect(serviceSpy.update).toHaveBeenCalledWith('cf-1', {
      jurisdiction: null, stage: null, position: null
    });
  });

  it('U-14: erreur 422 — snackbar avec message backend, formulaire reste ouvert', () => {
    serviceSpy.update.mockReturnValue(throwError(() => ({
      status: 422,
      error: { message: 'Le stade FOND n\'appartient pas à la juridiction CA_SOC.' }
    })));
    setup();
    component.startEdit();
    component.onJurisdictionChange('CPH');
    component.onStageChange('FOND');
    component.save();
    expect(component.editing()).toBe(true);
    expect(component.saving()).toBe(false);
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Le stade FOND n\'appartient pas à la juridiction CA_SOC.',
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] })
    );
  });

  it('U-15: erreur sans message backend — message générique', () => {
    serviceSpy.update.mockReturnValue(throwError(() => ({ status: 500 })));
    setup();
    component.startEdit();
    component.save();
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Erreur lors de l\'enregistrement du stade procédural.',
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] })
    );
  });

  it('U-16: Annuler ferme le formulaire sans appeler update()', () => {
    setup();
    component.startEdit();
    component.cancelEdit();
    expect(component.editing()).toBe(false);
    expect(serviceSpy.update).not.toHaveBeenCalled();
  });

  it('U-17: startEdit pré-remplit le formulaire avec le stade courant', () => {
    serviceSpy.get.mockReturnValue(of(makeStage({
      jurisdiction: 'CPH', stage: 'FOND', position: 'DEMANDEUR'
    })));
    setup();
    component.startEdit();
    expect(component.formJurisdiction()).toBe('CPH');
    expect(component.formStage()).toBe('FOND');
    expect(component.formPosition()).toBe('DEMANDEUR');
  });
});
