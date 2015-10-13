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

export const selectDocument = (id) => (state) => state.documents[id]

export const selectDocumentLoadError = (id) => (state) => state.documentLoadErrors[id]

export const isDocumentReady = (id) => (state) => {
    const doc = selectDocument(id)

    // if (doc) { return true }

    if (doc && doc.properties && doc.properties['file:content'] != null) {
        return true
    }

    return false
}
