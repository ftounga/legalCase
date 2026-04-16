import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { PartageImmobilierSectionComponent } from './partage-immobilier-section.component';

describe('PartageImmobilierSectionComponent', () => {
  let component: PartageImmobilierSectionComponent;
  let fixture: ComponentFixture<PartageImmobilierSectionComponent>;
  let httpMock: HttpTestingController;

  const ID = '77777777-7777-7777-7777-777777777777';
  const URL = `/api/v1/case-files/${ID}/partage-immobilier`;
  const MOCK = {
    caseFileId: ID, country: 'FRANCE', valeurVenale: 300000, capitalRestantDu: 100000,
    valeurNette: 200000, quotePartAttributaire: 0.5, quotePartCedant: 0.5,
    partAttributaire: 100000, partCedant: 100000, soulte: 100000,
    droitPartage: 2200, tauxDroitPartage: 1.1, fraisNotaireEstimes: 4500,
    coutTotal: 106700, baseJuridique: 'Art. 746 CGI', commentaire: 'Test'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PartageImmobilierSectionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideAnimationsAsync()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PartageImmobilierSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = ID;
  });

  afterEach(() => { httpMock.verify(); });

  function flushSE(): void {
    httpMock.match(r => r.url.endsWith('/source-explanations')).forEach(r => r.flush([]));
  }
  function initNo(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(null, { status: 404, statusText: 'NF' }); flushSE(); }
  function initWith(): void { fixture.detectChanges(); httpMock.expectOne(URL).flush(MOCK); flushSE(); }

  it('should create', () => { initNo(); expect(component).toBeTruthy(); });
  it('should show form when no existing', () => { initNo(); expect(component.showForm()).toBe(true); });
  it('should call POST when calculate()', () => {
    initNo();
    component.valeurVenale.set(300000); component.calculate();
    const r = httpMock.expectOne(req => req.url === URL && req.method === 'POST');
    r.flush(MOCK);
    expect(component.result()).toBeTruthy(); expect(component.showForm()).toBe(false);
  });
  it('should display existing from GET', () => { initWith(); expect(component.result()).toBeTruthy(); expect(component.showForm()).toBe(false); });

  // ---- Import IA (SF-FA-05-04) ----

  function setLiquidation(actif: { libelle: string; valeur: number | null }[], passif: { libelle: string; valeur: number | null }[] = []) {
    component.liquidationCommunaute = {
      regimeMatrimonial: 'COMMUNAUTE_LEGALE',
      actifCommun: actif as any,
      biensPropresEpouxA: [],
      biensPropresEpouxB: [],
      passifCommun: passif as any,
    };
  }

  it('should filter actifCommun to only immobilier items', () => {
    setLiquidation([
      { libelle: 'Maison principale', valeur: 400000 },
      { libelle: 'Compte épargne', valeur: 30000 },
      { libelle: 'Véhicule Tesla', valeur: 50000 },
    ]);
    initNo();
    expect(component.biensImmobiliersFiltres().map(b => b.libelle)).toEqual(['Maison principale']);
  });

  it('should accept multiple immobilier items case-insensitive', () => {
    setLiquidation([
      { libelle: 'appartement Paris', valeur: 500000 },
      { libelle: 'APPARTEMENT Lyon', valeur: 300000 },
    ]);
    initNo();
    expect(component.biensImmobiliersFiltres().length).toBe(2);
  });

  it('should filter passifCommun to only prêt items', () => {
    setLiquidation(
      [{ libelle: 'Maison', valeur: 400000 }],
      [
        { libelle: 'Prêt BNP', valeur: 150000 },
        { libelle: 'Carte bleue', valeur: 2000 },
      ]
    );
    initNo();
    expect(component.pretsFiltres().map(p => p.libelle)).toEqual(['Prêt BNP']);
  });

  it('should consider canImport false when no valid item', () => {
    setLiquidation([{ libelle: 'Maison', valeur: null }]);
    initNo();
    expect(component.canImport()).toBe(false);
  });

  it('should apply import : set valeurVenale from selected bien', () => {
    setLiquidation([{ libelle: 'Maison', valeur: 400000 }], [{ libelle: 'Prêt BNP', valeur: 150000 }]);
    initNo();
    component.selectedBienLibelle.set('Maison');
    component.selectedPretLibelle.set('Prêt BNP');
    component.applyImport();
    expect(component.valeurVenale()).toBe(400000);
    expect(component.capitalRestantDu()).toBe(150000);
    expect(component.provenanceValeur()).toBe('IA');
    expect(component.provenancePret()).toBe('IA');
  });

  it('should apply import without prêt if user selects "Aucun prêt"', () => {
    setLiquidation([{ libelle: 'Maison', valeur: 400000 }]);
    initNo();
    component.capitalRestantDu.set(50000);
    component.selectedBienLibelle.set('Maison');
    component.selectedPretLibelle.set(null);
    component.applyImport();
    expect(component.valeurVenale()).toBe(400000);
    expect(component.capitalRestantDu()).toBe(50000); // unchanged
    expect(component.provenancePret()).toBeNull();
  });

  it('should clear provenance note when user edits valeurVenale manually', () => {
    setLiquidation([{ libelle: 'Maison', valeur: 400000 }]);
    initNo();
    component.selectedBienLibelle.set('Maison');
    component.applyImport();
    expect(component.provenanceValeur()).toBe('IA');
    component.valeurVenale.set(420000);
    component.onValeurVenaleChange();
    expect(component.provenanceValeur()).toBeNull();
  });

  it('should NOT show import when liquidationCommunaute is null', () => {
    initNo();
    expect(component.biensImmobiliersFiltres()).toEqual([]);
    expect(component.canImport()).toBe(false);
  });

  it('should not apply import when no bien selected', () => {
    setLiquidation([{ libelle: 'Maison', valeur: 400000 }]);
    initNo();
    component.valeurVenale.set(100000);
    component.applyImport();
    expect(component.valeurVenale()).toBe(100000);
  });

  // ---- Coherence alerts (SF-IA-03-08) ----

  it('should NOT alert valeur when within 10% of IA best match', () => {
    setLiquidation([{ libelle: 'Maison', valeur: 400000 }]);
    initNo();
    component.valeurVenale.set(420000); // +5%
    expect(component.coherenceAlerts().VALEUR_VENALE).toBeUndefined();
  });

  it('should alert warning valeur when 10% diverges', () => {
    setLiquidation([{ libelle: 'Maison', valeur: 400000 }]);
    initNo();
    component.valeurVenale.set(450000); // +12.5%
    const alert = component.coherenceAlerts().VALEUR_VENALE;
    expect(alert).toBeDefined();
    expect(alert!.iaValue).toBe(400000);
    expect(alert!.iaLibelle).toBe('Maison');
  });

  it('should pick best-match among multiple biens', () => {
    setLiquidation([
      { libelle: 'Maison', valeur: 400000 },
      { libelle: 'Appartement Paris', valeur: 300000 },
    ]);
    initNo();
    component.valeurVenale.set(310000); // +3.3% vs appt, -22.5% vs maison
    // Best match = appartement
    expect(component.coherenceAlerts().VALEUR_VENALE).toBeUndefined();
  });

  it('should alert when user diverges even from best match', () => {
    setLiquidation([
      { libelle: 'Maison', valeur: 400000 },
      { libelle: 'Appartement', valeur: 300000 },
    ]);
    initNo();
    component.valeurVenale.set(360000); // best match = maison (-10%)
    // écart relatif = 40/400 = 0.10 exactement
    const alert = component.coherenceAlerts().VALEUR_VENALE;
    expect(alert).toBeDefined();
    expect(alert!.iaValue).toBe(400000);
  });

  it('should use imported reference over best-match if import active', () => {
    setLiquidation([
      { libelle: 'Maison', valeur: 400000 },
      { libelle: 'Appartement', valeur: 300000 },
    ]);
    initNo();
    // User imports Maison
    component.selectedBienLibelle.set('Maison');
    component.applyImport();
    // User modifies to 350000 (−12.5% vs Maison, but −16.7% vs Appartement)
    component.valeurVenale.set(350000);
    component.onValeurVenaleChange();
    const alert = component.coherenceAlerts().VALEUR_VENALE;
    expect(alert?.iaValue).toBe(400000); // reference = imported Maison, not best match
  });

  it('should NOT alert when valeur user = 0', () => {
    setLiquidation([{ libelle: 'Maison', valeur: 400000 }]);
    initNo();
    component.valeurVenale.set(0);
    expect(component.coherenceAlerts().VALEUR_VENALE).toBeUndefined();
  });

  it('should NOT alert when liquidationCommunaute null', () => {
    initNo();
    component.valeurVenale.set(999999);
    expect(component.alertsSummary().total).toBe(0);
  });

  it('should NOT alert when no immobilier item', () => {
    setLiquidation([{ libelle: 'Véhicule', valeur: 50000 }]);
    initNo();
    component.valeurVenale.set(500000);
    expect(component.coherenceAlerts().VALEUR_VENALE).toBeUndefined();
  });

  // Capital
  it('should alert warning capital on 10%+ divergence', () => {
    setLiquidation(
      [{ libelle: 'Maison', valeur: 400000 }],
      [{ libelle: 'Prêt BNP', valeur: 150000 }]
    );
    initNo();
    component.capitalRestantDu.set(170000); // +13%
    expect(component.coherenceAlerts().CAPITAL_RESTANT).toBeDefined();
  });

  it('should NOT alert capital when 0', () => {
    setLiquidation(
      [{ libelle: 'Maison', valeur: 400000 }],
      [{ libelle: 'Prêt', valeur: 150000 }]
    );
    initNo();
    component.capitalRestantDu.set(0);
    expect(component.coherenceAlerts().CAPITAL_RESTANT).toBeUndefined();
  });

  it('should count multiple alerts', () => {
    setLiquidation(
      [{ libelle: 'Maison', valeur: 400000 }],
      [{ libelle: 'Prêt', valeur: 150000 }]
    );
    initNo();
    component.valeurVenale.set(500000);
    component.capitalRestantDu.set(200000);
    expect(component.alertsSummary()).toEqual({ total: 2, blockers: 0 });
  });

  it('should freeze alerts when result is loaded', () => {
    setLiquidation([{ libelle: 'Maison', valeur: 400000 }]);
    initWith(); // loads existing result, hides form
    component.valeurVenale.set(999999);
    expect(component.coherenceAlerts().VALEUR_VENALE).toBeUndefined();
  });

  it('SF-118-04: GET 200 pré-remplit les signals du formulaire depuis la réponse sauvegardée', () => {
    initWith();
    expect(component.country()).toBe('FRANCE');
    expect(component.valeurVenale()).toBe(300000);
    expect(component.capitalRestantDu()).toBe(100000);
    // Backend stocke la décimale 0.5 → signal en pourcentage 50
    expect(component.quotePartAttributaire()).toBe(50);
  });

  it('SF-118-04: editForm restaure les signals depuis le résultat', () => {
    initWith();
    expect(component.showForm()).toBe(false);
    // Simuler une modification locale perdue puis un Modifier (reload-like)
    component.valeurVenale.set(0);
    component.capitalRestantDu.set(0);
    component.editForm();
    expect(component.showForm()).toBe(true);
    expect(component.valeurVenale()).toBe(300000);
    expect(component.capitalRestantDu()).toBe(100000);
    expect(component.quotePartAttributaire()).toBe(50);
  });

  // ---- Pattern F-IA-03 complet (F96 + Question IA + Pièce manquante) ----

  it('should alert F96 when procedureCheck VERIFIED contredit la valeur saisie', () => {
    component.procedureChecks = [{
      id: 'pc1', ordre: 1, description: 'Valeur vénale',
      statut: 'VERIFIED', critereCode: 'FA05_VALEUR_VENALE', expectedValue: '400000',
      raison: 'Rapport expert joint',
    }];
    initNo();
    component.valeurVenale.set(300000); // écart > 10%
    const alert = component.coherenceAlerts().VALEUR_VENALE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('F96');
    expect(alert!.iaValue).toBe(400000);
    expect(alert!.contributors).toEqual(['F96']);
  });

  it('should alert QUESTION_IA when avocat répond oui avec expectedValue divergent', () => {
    component.aiQuestions = [{
      id: 'q1', orderIndex: 1,
      questionText: 'Le capital restant dû est-il de 120000 € ?',
      answerText: 'oui',
      critereCode: 'FA05_CAPITAL_RESTANT', expectedValue: '120000',
    }];
    initNo();
    component.capitalRestantDu.set(150000); // écart 25%
    const alert = component.coherenceAlerts().CAPITAL_RESTANT;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('QUESTION_IA');
    expect(alert!.iaValue).toBe(120000);
  });

  it('should add PIECE_MANQUANTE contributor when F96 actif + pièce manquante même code', () => {
    component.procedureChecks = [{
      id: 'pc1', ordre: 1, description: 'Valeur vénale',
      statut: 'VERIFIED', critereCode: 'FA05_VALEUR_VENALE', expectedValue: '400000',
    }];
    component.piecesManquantes = [{
      texte: 'Rapport d\'expertise immobilière',
      critereCode: 'FA05_VALEUR_VENALE',
    }];
    initNo();
    component.valeurVenale.set(300000);
    const alert = component.coherenceAlerts().VALEUR_VENALE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toContain('F96');
    expect(alert!.contributors).toContain('PIECE_MANQUANTE');
    expect(alert!.pieceTexte).toBe('Rapport d\'expertise immobilière');
  });

  it('should produce MULTI source when F96 + IA best-match convergent', () => {
    component.procedureChecks = [{
      id: 'pc1', ordre: 1, description: 'Valeur vénale',
      statut: 'VERIFIED', critereCode: 'FA05_VALEUR_VENALE', expectedValue: '400000',
    }];
    setLiquidation([{ libelle: 'Maison', valeur: 395000 }]); // within 10% of 400000
    initNo();
    component.valeurVenale.set(300000); // écart > 10% vs les deux
    const alert = component.coherenceAlerts().VALEUR_VENALE;
    expect(alert).toBeDefined();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toEqual(['F96', 'IA']);
    expect(alert!.iaValue).toBe(400000);
  });
});
