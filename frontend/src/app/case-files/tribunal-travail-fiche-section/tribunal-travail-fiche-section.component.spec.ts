import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Overlay } from '@angular/cdk/overlay';
import { of, throwError } from 'rxjs';
import { SimpleChange } from '@angular/core';
import { TribunalTravailFicheSectionComponent } from './tribunal-travail-fiche-section.component';
import { TribunalTravailFicheService } from '../../core/services/tribunal-travail-fiche.service';
import { TribunalTravailFiche } from '../../core/models/tribunal-travail-fiche.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

function makeFiche(overrides: Partial<TribunalTravailFiche> = {}): TribunalTravailFiche {
  return {
    id: 'f-1',
    requerant: { nom: 'Janssens', prenom: 'Marie', domicile: null, registreNational: null },
    defendeur: { nom: 'Carrefour BE', siegeSocial: null, numeroBce: null, representant: null },
    procedureInfo: { tribunal: 'Bruxelles', division: null, langue: 'FR', commissionParitaire: 'CP200' },
    contratInfo: { typeContrat: 'EMPLOYE', dateDebut: '2020-01-15', dateFin: '2026-01-30', motifRupture: 'Motif grave' },
    demandes: [{ label: 'Indemnité', montant: 12000 }],
    exposeDesMoyens: 'Art. 35 LCT',
    piecesList: [{ numero: 1, nom: 'contrat.pdf' }],
    updatedAt: '2026-04-04T10:00:00Z',
    ...overrides
  };
}

describe('TribunalTravailFicheSectionComponent', () => {
  let component: TribunalTravailFicheSectionComponent;
  let fixture: ComponentFixture<TribunalTravailFicheSectionComponent>;
  let ficheServiceSpy: jest.Mocked<TribunalTravailFicheService>;
  let snackBarSpy: { open: jest.Mock };

  beforeEach(async () => {
    ficheServiceSpy = {
      get: jest.fn(),
      save: jest.fn(),
    } as unknown as jest.Mocked<TribunalTravailFicheService>;
    // Par défaut : 404 (pas de fiche persistée) → permet de tester le prefill IA.
    ficheServiceSpy.get.mockReturnValue(throwError(() => new Error('404')));

    snackBarSpy = { open: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [TribunalTravailFicheSectionComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: TribunalTravailFicheService, useValue: ficheServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        { provide: Overlay, useValue: { create: jest.fn(), position: jest.fn(), scrollStrategies: { reposition: jest.fn() } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TribunalTravailFicheSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'test-id';
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  it('affiche le header "Requête tribunal du travail"', () => {
    const header = fixture.nativeElement.querySelector('.section-title');
    expect(header?.textContent).toContain('Requête tribunal du travail');
  });

  it('collapsed par défaut', () => {
    expect(component.collapsed()).toBe(true);
    expect(fixture.nativeElement.querySelector('.fiche-form')).toBeNull();
  });

  it('toggleCollapsed ouvre le formulaire', () => {
    component.toggleCollapsed();
    fixture.detectChanges();
    expect(component.collapsed()).toBe(false);
    expect(fixture.nativeElement.querySelector('.fiche-form')).toBeTruthy();
  });

  it('addDemande ajoute une demande au FormArray', () => {
    expect(component.demandesArray.length).toBe(0);
    component.addDemande();
    expect(component.demandesArray.length).toBe(1);
  });

  it('removeDemande supprime une demande', () => {
    component.addDemande();
    component.addDemande();
    expect(component.demandesArray.length).toBe(2);
    component.removeDemande(0);
    expect(component.demandesArray.length).toBe(1);
  });

  // SF-173-02 — Pré-fill IA tests

  function makeAi(overrides: Partial<TravailExtractedData> = {}): TravailExtractedData {
    return {
      conventionCollective: 'CP200',
      typeContrat: 'EMPLOYE',
      dateEntree: '2020-01-15',
      dateLicenciement: '2026-01-30',
      motifLicenciement: 'Motif grave',
      ...overrides
    };
  }

  function buildFreshComponent(getResponse: ReturnType<typeof of> | ReturnType<typeof throwError>) {
    ficheServiceSpy.get.mockReturnValue(getResponse as never);
    const fixture2 = TestBed.createComponent(TribunalTravailFicheSectionComponent);
    const c = fixture2.componentInstance;
    c.caseFileId = 'cf-2';
    return { fixture: fixture2, component: c };
  }

  // SF-173-02-T1 : prefill sans aiData → tous champs IA vides, pas de badges.
  it('SF-173-02-T1 prefill sans aiData → formulaire vide (pas de provenance IA)', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = null;
    ctx.fixture.detectChanges();

    expect(ctx.component.form.get('procedureInfo.commissionParitaire')?.value).toBeFalsy();
    expect(ctx.component.form.get('contratInfo.typeContrat')?.value).toBeFalsy();
    expect(ctx.component.form.get('contratInfo.dateDebut')?.value).toBeFalsy();
    expect(ctx.component.provenanceCommissionParitaire()).toBeNull();
    expect(ctx.component.provenanceTypeContrat()).toBeNull();
    expect(ctx.component.provenanceDateDebut()).toBeNull();
  });

  // SF-173-02-T2 : prefill avec aiData complet → tous champs remplis + badges présents.
  it('SF-173-02-T2 prefill avec aiData complet → tous les champs mappés sont remplis', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi();
    ctx.fixture.detectChanges();
    ctx.component.collapsed.set(false);
    ctx.fixture.detectChanges();

    expect(ctx.component.form.get('procedureInfo.commissionParitaire')?.value).toBe('CP200');
    expect(ctx.component.form.get('contratInfo.typeContrat')?.value).toBe('EMPLOYE');
    expect(ctx.component.form.get('contratInfo.dateDebut')?.value).toBe('2020-01-15');
    expect(ctx.component.form.get('contratInfo.dateFin')?.value).toBe('2026-01-30');
    expect(ctx.component.form.get('contratInfo.motifRupture')?.value).toBe('Motif grave');

    expect(ctx.component.provenanceCommissionParitaire()).toBe('IA');
    expect(ctx.component.provenanceTypeContrat()).toBe('IA');
    expect(ctx.component.provenanceDateDebut()).toBe('IA');
    expect(ctx.component.provenanceDateFin()).toBe('IA');
    expect(ctx.component.provenanceMotifRupture()).toBe('IA');

    const html = ctx.fixture.nativeElement.innerHTML;
    expect(html).toContain('auto_awesome');
  });

  // SF-173-02-T3 : prefill partiel → certains champs vides, badges absents pour ceux-là.
  it('SF-173-02-T3 prefill partiel : seul commissionParitaire IA → autres champs vides', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = {
      conventionCollective: 'CP218',
      typeContrat: null,
      dateEntree: null,
      dateLicenciement: null,
      motifLicenciement: null,
    } as TravailExtractedData;
    ctx.fixture.detectChanges();

    expect(ctx.component.form.get('procedureInfo.commissionParitaire')?.value).toBe('CP218');
    expect(ctx.component.provenanceCommissionParitaire()).toBe('IA');

    expect(ctx.component.form.get('contratInfo.dateDebut')?.value).toBeFalsy();
    expect(ctx.component.provenanceDateDebut()).toBeNull();
    expect(ctx.component.provenanceMotifRupture()).toBeNull();
  });

  // SF-173-02-T4 : changement manuel → badge disparaît.
  it('SF-173-02-T4 saisie manuelle après prefill → provenance IA effacée pour le champ modifié', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi();
    ctx.fixture.detectChanges();

    expect(ctx.component.provenanceDateDebut()).toBe('IA');

    ctx.component.form.get('contratInfo.dateDebut')?.setValue('2019-12-01');
    expect(ctx.component.provenanceDateDebut()).toBeNull();
    // Les autres champs gardent leur badge IA.
    expect(ctx.component.provenanceCommissionParitaire()).toBe('IA');
  });

  // SF-173-02-T5 : re-analyse ne ré-écrase pas une saisie manuelle.
  it('SF-173-02-T5 re-analyse : champs avec saisie manuelle préservés', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi({ motifLicenciement: 'Motif grave' });
    ctx.fixture.detectChanges();

    ctx.component.form.get('contratInfo.motifRupture')?.setValue('Faute lourde');
    expect(ctx.component.provenanceMotifRupture()).toBeNull();

    const newAi = makeAi({ motifLicenciement: 'Insuffisance professionnelle' });
    ctx.component.aiData = newAi;
    ctx.component.ngOnChanges({
      aiData: new SimpleChange(makeAi({ motifLicenciement: 'Motif grave' }), newAi, false),
    });

    expect(ctx.component.form.get('contratInfo.motifRupture')?.value).toBe('Faute lourde');
  });

  // SF-173-02-T6 : F-IA-03 alertes sur divergences.
  it('SF-173-02-T6 coherenceAlerts émet des alertes sur divergences (DATE_DEBUT, MOTIF_RUPTURE)', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi();
    ctx.fixture.detectChanges();

    // L'avocat modifie dateDebut
    ctx.component.form.get('contratInfo.dateDebut')?.setValue('2018-06-01');
    ctx.component.form.get('contratInfo.motifRupture')?.setValue('Faute lourde');
    ctx.fixture.detectChanges();

    const alerts = ctx.component.coherenceAlerts();
    expect(alerts.DATE_DEBUT).toBeDefined();
    expect(alerts.DATE_DEBUT?.expectedDisplay).toBe('2020-01-15');
    expect(alerts.DATE_DEBUT?.source).toBe('IA');

    expect(alerts.MOTIF_RUPTURE).toBeDefined();
    expect(alerts.MOTIF_RUPTURE?.expectedDisplay).toBe('Motif grave');
  });

  // SF-173-02-T7 : sauvegarde nominale → pieces mises à jour + toast succès.
  it('SF-173-02-T7 save success → toast de succès', () => {
    ficheServiceSpy.save.mockReturnValue(of(makeFiche()));
    component.collapsed.set(false);
    component.form.get('requerant.nom')?.setValue('Janssens');

    component.save();

    expect(ficheServiceSpy.save).toHaveBeenCalledWith('test-id', expect.any(Object));
    expect(snackBarSpy.open).toHaveBeenCalledWith('Requête enregistrée', 'Fermer', expect.any(Object));
  });

  // SF-173-02-T8 : fiche persistée chargée → pas d'alerte F-IA-03.
  it('SF-173-02-T8 fiche persistée chargée → coherenceAlerts vide', () => {
    const ctx = buildFreshComponent(of(makeFiche()));
    ctx.component.aiData = makeAi({ motifLicenciement: 'Autre motif' });
    ctx.fixture.detectChanges();

    expect(ctx.component.coherenceAlerts()).toEqual({});
  });

  // SF-173-02-T9 : normalizeTypeContrat — mapping IA libre → enum BE.
  it('SF-173-02-T9 typeContrat IA "CDI" → mappé vers EMPLOYE', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi({ typeContrat: 'CDI' });
    ctx.fixture.detectChanges();
    expect(ctx.component.form.get('contratInfo.typeContrat')?.value).toBe('EMPLOYE');
    expect(ctx.component.provenanceTypeContrat()).toBe('IA');
  });

  it('SF-173-02-T9b typeContrat IA "ouvrier" → mappé vers OUVRIER', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi({ typeContrat: 'ouvrier' });
    ctx.fixture.detectChanges();
    expect(ctx.component.form.get('contratInfo.typeContrat')?.value).toBe('OUVRIER');
  });
});
