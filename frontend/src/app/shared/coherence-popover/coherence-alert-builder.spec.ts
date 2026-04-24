import { CoherenceAlertBuilder } from './coherence-alert-builder';

type TestField = 'SALAIRE' | 'MOTIF' | 'DATE';

describe('CoherenceAlertBuilder (SF-155-05)', () => {
  it('build() sans contributor → null', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('SALAIRE').build();
    expect(alert).toBeNull();
  });

  it('une seule source IA → source="IA", contributors=["IA"]', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('SALAIRE')
      .addSource('IA', {
        expectedDisplay: '3 000 €',
        reason: 'Analyse du dossier : 3 000 €',
      })
      .build();
    expect(alert).not.toBeNull();
    expect(alert!.field).toBe('SALAIRE');
    expect(alert!.source).toBe('IA');
    expect(alert!.contributors).toEqual(['IA']);
    expect(alert!.expectedDisplay).toBe('3 000 €');
    expect(alert!.reason).toBe('Analyse du dossier : 3 000 €');
  });

  it('IA + F96 même expected → source="MULTI", 2 contributors', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('MOTIF')
      .addSource('IA', { expectedDisplay: 'TRAVAIL', reason: 'IA : TRAVAIL' })
      .addSource('F96', { expectedDisplay: 'TRAVAIL', reason: 'Checklist : TRAVAIL' })
      .build();
    expect(alert).not.toBeNull();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toEqual(['IA', 'F96']);
    expect(alert!.reason).toBe('IA : TRAVAIL ET Checklist : TRAVAIL');
    expect(alert!.expectedDisplay).toBe('TRAVAIL');
  });

  it('IA + QUESTION_IA + PIECE_MANQUANTE same expected → 3 contributors', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('MOTIF')
      .addSource('IA', { expectedDisplay: 'FAMILLE', reason: 'Analyse du dossier : FAMILLE' })
      .addSource('QUESTION_IA', {
        expectedDisplay: 'FAMILLE',
        reason: 'Question complémentaire : mariage récent → oui',
      })
      .addSource('PIECE_MANQUANTE', {
        expectedDisplay: 'FAMILLE',
        reason: 'Pièce manquante : acte de mariage',
        pieceTexte: 'Acte de mariage manquant',
      })
      .build();
    expect(alert).not.toBeNull();
    expect(alert!.source).toBe('MULTI');
    expect(alert!.contributors).toEqual(['IA', 'QUESTION_IA', 'PIECE_MANQUANTE']);
    expect(alert!.pieceTexte).toBe('Acte de mariage manquant');
  });

  it('deuxième source avec expected divergent → ignorée', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('MOTIF')
      .addSource('IA', { expectedDisplay: 'TRAVAIL', reason: 'IA : TRAVAIL' })
      .addSource('F96', { expectedDisplay: 'ETUDES', reason: 'F96 : ETUDES' })
      .build();
    expect(alert).not.toBeNull();
    expect(alert!.source).toBe('IA');
    expect(alert!.contributors).toEqual(['IA']);
    expect(alert!.expectedDisplay).toBe('TRAVAIL');
  });

  it('addPieceManquante() attache le pieceTexte à une alerte déjà initiée', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('MOTIF')
      .addSource('IA', { expectedDisplay: 'FAMILLE', reason: 'IA : FAMILLE' })
      .addPieceManquante('Livret de famille manquant')
      .build();
    expect(alert).not.toBeNull();
    expect(alert!.contributors).toEqual(['IA', 'PIECE_MANQUANTE']);
    expect(alert!.pieceTexte).toBe('Livret de famille manquant');
    expect(alert!.reason).toContain('Livret de famille manquant');
  });

  it('addPieceManquante() avant toute source → silencieusement ignorée', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('MOTIF')
      .addPieceManquante('Pièce X manquante')
      .build();
    expect(alert).toBeNull();
  });

  it('severity par défaut → WARNING', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('SALAIRE')
      .addSource('IA', { expectedDisplay: '3 000 €', reason: 'Analyse : 3 000 €' })
      .build();
    expect(alert!.severity).toBe('WARNING');
  });

  it('withSeverity("CRITICAL") override → severity CRITICAL', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('DATE')
      .withSeverity('CRITICAL')
      .addSource('IA', {
        expectedDisplay: 'Recours hors délai probable',
        reason: 'Notification > 48h sans recours',
      })
      .build();
    expect(alert!.severity).toBe('CRITICAL');
  });

  it('withSeverity("INFO") + seule source IA → severity INFO', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('SALAIRE')
      .withSeverity('INFO')
      .addSource('IA', {
        expectedDisplay: 'Salaire brut déduit',
        reason: 'Le salaire brut a été déduit d\'un net (×1,30)',
      })
      .build();
    expect(alert!.severity).toBe('INFO');
  });

  it('ordre d\'insertion des contributors préservé', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('MOTIF')
      .addSource('QUESTION_IA', { expectedDisplay: 'TRAVAIL', reason: 'Q : TRAVAIL' })
      .addSource('IA', { expectedDisplay: 'TRAVAIL', reason: 'IA : TRAVAIL' })
      .addSource('F96', { expectedDisplay: 'TRAVAIL', reason: 'F96 : TRAVAIL' })
      .build();
    expect(alert!.contributors).toEqual(['QUESTION_IA', 'IA', 'F96']);
    expect(alert!.reason).toBe('Q : TRAVAIL ET IA : TRAVAIL ET F96 : TRAVAIL');
  });

  it('addSource avec expectedDisplay vide → ignorée', () => {
    const alert = CoherenceAlertBuilder.forField<TestField>('SALAIRE')
      .addSource('IA', { expectedDisplay: '', reason: 'IA empty' })
      .addSource('F96', { expectedDisplay: '3 000 €', reason: 'F96 : 3 000 €' })
      .build();
    expect(alert).not.toBeNull();
    expect(alert!.contributors).toEqual(['F96']);
    expect(alert!.expectedDisplay).toBe('3 000 €');
  });

  it('field typé strict avec enum composant local', () => {
    type IM05Field = 'MOTIF' | 'NATIONALITE_UE';
    const alert = CoherenceAlertBuilder.forField<IM05Field>('NATIONALITE_UE')
      .addSource('IA', {
        expectedDisplay: 'Ressortissant UE',
        reason: 'Analyse du dossier : nationalité UE/EEE/Suisse',
      })
      .build();
    expect(alert!.field).toBe('NATIONALITE_UE');
  });

  it('addSource(PIECE_MANQUANTE) initial sans autre source → seule l\'alerte PIECE_MANQUANTE', () => {
    // On accepte que PIECE_MANQUANTE fonde seule une alerte via addSource
    // (sémantique différente d'addPieceManquante qui requiert un contributor préalable).
    const alert = CoherenceAlertBuilder.forField<TestField>('MOTIF')
      .addSource('PIECE_MANQUANTE', {
        expectedDisplay: 'Pièce requise',
        reason: 'F-145 : attestation de travail manquante',
        pieceTexte: 'Attestation de travail manquante',
      })
      .build();
    expect(alert).not.toBeNull();
    expect(alert!.source).toBe('PIECE_MANQUANTE');
    expect(alert!.pieceTexte).toBe('Attestation de travail manquante');
  });
});
