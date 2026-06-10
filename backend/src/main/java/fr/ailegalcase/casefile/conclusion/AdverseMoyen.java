package fr.ailegalcase.casefile.conclusion;

import java.util.List;

/**
 * F-261 / SF-261-02 — moyen de la partie adverse extrait de ses écritures
 * (document marqué {@code adverse_pleadings}, SF-261-01).
 *
 * <p>Un moyen = un argument soutenu par l'adversaire : la <strong>thèse</strong>
 * qu'il défend, les <strong>fondements</strong> juridiques qu'il invoque
 * (articles / bases légales) et les <strong>pièces</strong> sur lesquelles il
 * s'appuie. Intrant de génération injecté dans la section « MOYENS ADVERSES À
 * RÉFUTER » du prompt de conclusions (SF-261-03), pour que l'acte généré réfute
 * chaque moyen explicitement.</p>
 *
 * <p>Anti-invention : l'extraction ne fabrique jamais un moyen absent du texte ;
 * à défaut de moyen fiable, la liste reste vide (silence &gt; erreur, cohérent
 * avec les invariants F-179 / SF-98-56).</p>
 *
 * @param these          thèse soutenue par la partie adverse (jamais {@code null})
 * @param fondements      articles / bases juridiques invoqués (jamais {@code null}, peut être vide)
 * @param piecesInvoquees pièces sur lesquelles l'adversaire s'appuie (jamais {@code null}, peut être vide)
 */
public record AdverseMoyen(String these, List<String> fondements, List<String> piecesInvoquees) {

    public AdverseMoyen {
        fondements = fondements == null ? List.of() : List.copyOf(fondements);
        piecesInvoquees = piecesInvoquees == null ? List.of() : List.copyOf(piecesInvoquees);
    }
}
