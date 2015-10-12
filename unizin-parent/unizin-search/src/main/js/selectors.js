import difference from 'lodash.difference'

export const selectResults = (text, catalogs, page) => (state) => {
    const { criteria, searchResults } = state

    if (criteria != null
        && text === criteria.text
        && difference(criteria.catalogs, catalogs).length == 0
        && difference(catalogs, criteria.catalogs).length == 0) {

        return searchResults
    }

    return {
        entities: []
    }
}

export const selectCatalogs = (state) => state.catalogs
