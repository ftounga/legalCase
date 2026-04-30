import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Overlay } from '@angular/cdk/overlay';
import { of, throwError } from 'rxjs';
import { SimpleChange } from '@angular/core';
import { PrudhomeFicheSectionComponent } from './prudhome-fiche-section.component';
import { PrudhomeFicheService } from '../../core/services/prudhome-fiche.service';
import { PrudhomeFiche } from '../../core/models/prudhome-fiche.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

function makeFiche(overrides: Partial<PrudhomeFiche> = {}): PrudhomeFiche {
  return {
    id: 'f-1',
    demandeur: { nom: 'Dupont', prenom: 'Jean', adresse: null, telephone: null, email: null, profession: null },
    defendeur: { nom: 'Renault SAS', adresse: null, siret: null, representant: null },
    demandes: [{ label: 'Indemnité', montant: 5000 }],
    faitsTexte: 'Licenciement abusif',
    moyensDroitTexte: 'Art. L1235-3',
    piecesList: [{ numero: 1, nom: 'contrat.pdf' }],
    updatedAt: '2026-04-04T10:00:00Z',
    ...overrides
  };
}

describe('PrudhomeFicheSectionComponent', () => {
  let fixture: ComponentFixture<PrudhomeFicheSectionComponent>;
  let component: PrudhomeFicheSectionComponent;
  let ficheServiceSpy: jest.Mocked<PrudhomeFicheService>;
  let snackBarSpy: { open: jest.Mock };

  beforeEach(async () => {
    ficheServiceSpy = {
      get: jest.fn(),
      save: jest.fn()
    } as unknown as jest.Mocked<PrudhomeFicheService>;
    ficheServiceSpy.get.mockReturnValue(of(makeFiche()));

    snackBarSpy = { open: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [PrudhomeFicheSectionComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PrudhomeFicheService, useValue: ficheServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy },
        // Stub Overlay (CoherencePopoverTriggerDirective le requiert via DI).
        { provide: Overlay, useValue: { create: jest.fn(), position: jest.fn(), scrollStrategies: { reposition: jest.fn() } } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PrudhomeFicheSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'cf-1';
    fixture.detectChanges();
    // Absorbe la requête source-explanations émise par ngOnInit (fail-open).
    const httpMock = TestBed.inject(HttpTestingController);
    httpMock.match(/\/source-explanations$/).forEach(r => r.flush({}));
  });

  // SFDT02-01 : chargement fiche existante → formulaire pré-rempli
  it('SFDT02-01 should prefill form from existing fiche', () => {
    expect(component.form.get('demandeur.nom')?.value).toBe('Dupont');
    expect(component.form.get('defendeur.nom')?.value).toBe('Renault SAS');
    expect(component.demandesArray.length).toBe(1);
    expect(component.pieces()).toHaveLength(1);
    expect(component.pieces()[0].nom).toBe('contrat.pdf');
  });

  // SFDT02-02 : GET en erreur → formulaire conserve ses valeurs par défaut (vides)
  it('SFDT02-02 should keep default empty form values on GET error', () => {
    component.form.reset();
    component.demandesArray.clear();
    ficheServiceSpy.get.mockReturnValue(throwError(() => new Error('Network error')));

    component.ngOnInit();

    expect(component.form.get('demandeur.nom')?.value).toBeFalsy();
    expect(component.demandesArray.length).toBe(0);
  });

  // SFDT02-03 : sauvegarde nominale → toast succès
  it('SFDT02-03 should show success toast on save', () => {
    ficheServiceSpy.save.mockReturnValue(of(makeFiche()));
    component.collapsed.set(false);

    component.save();

    expect(ficheServiceSpy.save).toHaveBeenCalledWith('cf-1', expect.any(Object));
    expect(snackBarSpy.open).toHaveBeenCalledWith('Fiche enregistrée', 'Fermer', expect.any(Object));
  });

  // SFDT02-04 : sauvegarde en erreur → toast erreur, données conservées
  it('SFDT02-04 should show error toast on save failure', () => {
    ficheServiceSpy.save.mockReturnValue(throwError(() => new Error('500')));
    const nomBefore = component.form.get('demandeur.nom')?.value;

    component.save();

    expect(snackBarSpy.open).toHaveBeenCalledWith('Erreur lors de la sauvegarde', 'Fermer', expect.any(Object));
    expect(component.form.get('demandeur.nom')?.value).toBe(nomBefore);
  });

  // SFDT02-05 : ajout et suppression d'une demande
  it('SFDT02-05 should add and remove demandes', () => {
    const before = component.demandesArray.length;
    component.addDemande();
    expect(component.demandesArray.length).toBe(before + 1);

    component.removeDemande(0);
    expect(component.demandesArray.length).toBe(before);
  });

  // SF-173-01 — Pré-fill IA tests

  function makeAi(overrides: Partial<TravailExtractedData> = {}): TravailExtractedData {
    return {
      poste: 'Ingénieur logiciel',
      ...overrides
    };
  }

  function buildFreshComponent(getResponse: ReturnType<typeof of> | ReturnType<typeof throwError>) {
    ficheServiceSpy.get.mockReturnValue(getResponse as never);
    const fixture2 = TestBed.createComponent(PrudhomeFicheSectionComponent);
    const c = fixture2.componentInstance;
    c.caseFileId = 'cf-2';
    const httpMock = TestBed.inject(HttpTestingController);
    return {
      fixture: fixture2,
      component: c,
      flushSourceExplanations: () => httpMock.match(/\/source-explanations$/).forEach(r => r.flush({})),
    };
  }

  // SF-173-01-T1 : prefill sans aiData → pas de valeurs IA, pas de badge.
  it('SF-173-01-T1 prefill sans aiData → formulaire vide (pas de provenance IA)', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = null;
    ctx.fixture.detectChanges();

    expect(ctx.component.form.get('demandeur.profession')?.value).toBeFalsy();
    expect(ctx.component.provenanceProfession()).toBeNull();
  });

  // SF-173-01-T2 : prefill avec aiData → champ rempli + badge IA présent.
  it('SF-173-01-T2 prefill avec aiData complet → profession remplie + badge présent', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi({ poste: 'Cariste' });
    ctx.fixture.detectChanges();
    ctx.component.collapsed.set(false); // déplie pour que les badges soient rendus
    ctx.fixture.detectChanges();

    expect(ctx.component.form.get('demandeur.profession')?.value).toBe('Cariste');
    expect(ctx.component.provenanceProfession()).toBe('IA');

    const html = ctx.fixture.nativeElement.innerHTML;
    expect(html).toContain('auto_awesome');
  });

  // SF-173-01-T3 : prefill partiel (poste null) → champ vide, badge absent.
  it('SF-173-01-T3 prefill partiel : poste null → profession vide, badge absent', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi({ poste: null });
    ctx.fixture.detectChanges();

    expect(ctx.component.form.get('demandeur.profession')?.value).toBeFalsy();
    expect(ctx.component.provenanceProfession()).toBeNull();
  });

  // SF-173-01-T4 : changement manuel → badge disparaît.
  it('SF-173-01-T4 saisie manuelle après prefill → provenance IA effacée', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi({ poste: 'Ingénieur' });
    ctx.fixture.detectChanges();

    expect(ctx.component.provenanceProfession()).toBe('IA');

    ctx.component.form.get('demandeur.profession')?.setValue('Technicien supérieur');
    expect(ctx.component.provenanceProfession()).toBeNull();
  });

  // SF-173-01-T5 : re-analyse (ngOnChanges aiData) ne renverse pas une saisie manuelle existante.
  it('SF-173-01-T5 re-analyse : saisies manuelles préservées', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi({ poste: 'Ingénieur' });
    ctx.fixture.detectChanges();

    // Avocat saisit
    ctx.component.form.get('demandeur.profession')?.setValue('Technicien');
    expect(ctx.component.provenanceProfession()).toBeNull();

    // Re-analyse arrive avec une autre valeur IA
    const newAi = makeAi({ poste: 'Cadre' });
    ctx.component.aiData = newAi;
    ctx.component.ngOnChanges({
      aiData: new SimpleChange(makeAi({ poste: 'Ingénieur' }), newAi, false),
    });

    // La valeur saisie par l'avocat reste — la garde "champ vide" empêche l'écrasement.
    expect(ctx.component.form.get('demandeur.profession')?.value).toBe('Technicien');
  });

  // SF-173-01-T6 : F-IA-03 alerte sur divergence profession.
  it('SF-173-01-T6 coherenceAlerts émet une alerte sur divergence profession', () => {
    const ctx = buildFreshComponent(throwError(() => new Error('404')));
    ctx.component.aiData = makeAi({ poste: 'Ingénieur' });
    ctx.fixture.detectChanges();

    // L'avocat change la profession → alerte F-IA-03 attendue (divergence avec IA).
    ctx.component.form.get('demandeur.profession')?.setValue('Cariste');
    ctx.fixture.detectChanges();

    const alerts = ctx.component.coherenceAlerts();
    expect(alerts.PROFESSION).toBeDefined();
    expect(alerts.PROFESSION?.expectedDisplay).toBe('Ingénieur');
    expect(alerts.PROFESSION?.source).toBe('IA');
  });

  // SF-173-01-T7 : fiche persistée chargée → pas d'alerte F-IA-03 (les valeurs ont déjà été validées).
  it('SF-173-01-T7 fiche persistée chargée → pas d\'alerte F-IA-03', () => {
    // ficheServiceSpy.get retourne déjà une fiche (beforeEach) → hasPersistedFiche = true.
    component.aiData = makeAi({ poste: 'Cadre' });
    component.ngOnChanges({
      aiData: new SimpleChange(undefined, component.aiData, false),
    });
    fixture.detectChanges();

    expect(component.coherenceAlerts().PROFESSION).toBeUndefined();
  });
});
