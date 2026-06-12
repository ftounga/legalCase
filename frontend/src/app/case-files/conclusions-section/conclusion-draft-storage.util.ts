/**
 * F-279 / SF-279-01 — Stockage **local** du brouillon de conclusions en cours
 * d'édition (autosave anti-perte).
 *
 * Module **pur** (aucune dépendance Angular) pour être testé isolément et
 * réutilisé sans cycle d'import. Toutes les opérations sont encapsulées en
 * `try/catch` : si `localStorage` est indisponible (mode privé, quota plein,
 * exception au `setItem`/`getItem`), l'autosave est **désactivé silencieusement**
 * et l'éditeur reste pleinement fonctionnel (le seul écrit fiable reste
 * l'« Enregistrer » explicite côté serveur).
 *
 * Invariant (cf. `SF-279-00-coherence.md`) : ce brouillon est **transitoire** et
 * **purement local**. Il n'écrit jamais côté serveur, ne crée pas de version, et
 * est purgé dès qu'un enregistrement serveur réussit, à l'annulation, ou à la
 * sortie du cycle de vie `DRAFT`.
 */

/** Préfixe commun (cohérent avec les autres usages localStorage du produit). */
const KEY_PREFIX = 'lc.conclusions.draft.';

/** Enregistrement persistant d'un brouillon local. */
export interface ConclusionDraftRecord {
  /** Identifiant de la version éditée (clé de désambiguïsation). */
  versionId: string;
  /** Markdown brut du brouillon (round-trip F-264 garanti). */
  content: string;
  /** Horodatage ISO de la dernière écriture locale. */
  savedAt: string;
}

/**
 * Clé de stockage déterministe pour un couple (dossier, version). Scoper par
 * `versionId` évite qu'un brouillon d'une version ne « fuite » sur une autre.
 */
export function draftStorageKey(caseFileId: string, versionId: string): string {
  return `${KEY_PREFIX}${caseFileId}.${versionId}`;
}

/**
 * Lit le brouillon local d'une version. Retourne `null` si absent, si
 * `localStorage` est indisponible, ou si l'entrée est corrompue (JSON invalide
 * ou forme inattendue) — dans ce dernier cas l'entrée est aussi **supprimée**
 * pour ne pas reproposer un brouillon illisible.
 */
export function readDraft(
  caseFileId: string,
  versionId: string,
): ConclusionDraftRecord | null {
  const key = draftStorageKey(caseFileId, versionId);
  try {
    const raw = localStorage.getItem(key);
    if (raw === null) {
      return null;
    }
    const parsed = JSON.parse(raw) as Partial<ConclusionDraftRecord>;
    if (
      parsed &&
      typeof parsed.content === 'string' &&
      typeof parsed.versionId === 'string'
    ) {
      return {
        versionId: parsed.versionId,
        content: parsed.content,
        savedAt: typeof parsed.savedAt === 'string' ? parsed.savedAt : '',
      };
    }
    // Forme inattendue → purge.
    clearDraft(caseFileId, versionId);
    return null;
  } catch {
    // JSON invalide ou localStorage inaccessible → traité comme absent.
    try {
      localStorage.removeItem(key);
    } catch {
      /* no-op : localStorage indisponible */
    }
    return null;
  }
}

/**
 * Écrit (ou remplace) le brouillon local d'une version. No-op silencieux si
 * `localStorage` est indisponible ou si le quota est atteint.
 */
export function writeDraft(
  caseFileId: string,
  versionId: string,
  content: string,
): void {
  const record: ConclusionDraftRecord = {
    versionId,
    content,
    savedAt: new Date().toISOString(),
  };
  try {
    localStorage.setItem(
      draftStorageKey(caseFileId, versionId),
      JSON.stringify(record),
    );
  } catch {
    /* no-op : autosave désactivé silencieusement */
  }
}

/** Supprime le brouillon local d'une version. No-op silencieux en cas d'échec. */
export function clearDraft(caseFileId: string, versionId: string): void {
  try {
    localStorage.removeItem(draftStorageKey(caseFileId, versionId));
  } catch {
    /* no-op : localStorage indisponible */
  }
}
