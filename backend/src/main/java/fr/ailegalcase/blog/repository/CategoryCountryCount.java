package fr.ailegalcase.blog.repository;

/**
 * Projection Spring Data pour {@link BlogArticleRepository#countPublishedSince}.
 * Regroupe les articles publiés par catégorie (legal_domain) et pays.
 */
public interface CategoryCountryCount {
    String getCategory();
    String getCountry();
    Long getCount();
}
