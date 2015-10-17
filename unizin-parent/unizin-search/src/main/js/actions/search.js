import { httpGET, json, encodeURL, nxql } from './utils.js'
import { routeSearchFor } from './route.js'
import difference from 'lodash.difference'

export const SEARCH_FOR = 'SEARCH_FOR'
export const SEARCH_RESULTS = 'SEARCH_RESULTS'
export const CHANGE_CATALOG = 'CHANGE_CATALOG'

export function fetchSearchResults(text, catalogs, page) {

    if (catalogs.length === 0) {
        return Promise.resolve({
            totalSize: 0,
            entries: []
        })
    }

    const params = {
        pageSize: 20,
        query: nxql`SELECT * FROM Document
            WHERE ecm:fulltext = ${text}
            AND hrv:sourceRepository in ( ${catalogs} )
        `
    }

    if (page != null) {
        // the API needs an index, but for the UI we show page numbers
        params.currentPageIndex = parseInt(page, 10) - 1
    }

    const url =  encodeURL`/nuxeo/site/api/v1/query?${params}`
    const options = {
        headers: {
            'X-NXDocumentProperties': '*'
        }
    }
    return httpGET(url, options).then(json)
}

const PATH = 'default-domain'
export function searchFor(text, catalogs, page) {
    return (dispatch, getState) => {
        dispatch({
            type: SEARCH_FOR,
            payload: { text, catalogs, page },
        })
        dispatch(routeSearchFor(text, catalogs, page))

        fetchSearchResults(text, catalogs, page).then((results) => {
            const { criteria } = getState()

            // Don't keep these results if the criteria changed
            if (criteria.text != text
                || difference(criteria.catalogs, catalogs).length >0 ) {
                return
            }

            dispatch({
                type: SEARCH_RESULTS,
                payload: { results }
            })
        })
    }
}

export function changeCatalog(key, enabled) {
    return {
        type: CHANGE_CATALOG,
        payload: { key, enabled }
    }
}

export const FETCH_CATALOGS = 'FETCH_CATALOGS'

const catalogUrl = '/nuxeo/api/v1/directory/sourceRepositories'
export function fetchCatalogs() {
    return (dispatch, getState) => {
        if (Object.keys(getState().catalogs).length === 0) {
            return httpGET(catalogUrl).then(json).then((results) => dispatch({
                type: FETCH_CATALOGS,
                payload: results.entries
            }))
        }
    }
}
