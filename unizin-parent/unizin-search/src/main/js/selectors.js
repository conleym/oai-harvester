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

export const selectRetrievalStatus = (id) => (state) => {
    const doc = selectDocument(id)(state)

    const status = (doc
        && doc.properties
        && doc.properties['hrv:retrievalStatus'])

    return (status != null) ? status : ''
}

export const isDocumentReady = (id) => (state) => {
    return selectRetrievalStatus(id)(state) === 'success'
}
