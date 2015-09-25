import { httpGET, json, encodeURL } from './utils.js'
import { routeSearchFor } from './route.js'

export const SEARCH_FOR = 'SEARCH_FOR'
export const SEARCH_RESULTS = 'SEARCH_RESULTS'
export const CHANGE_CATALOG = 'CHANGE_CATALOG'

const PATH = 'default-domain'
export function searchFor(text, page) {

    const params = {
        fullText: text,
        pageSize: 20,
    }
    if (page != null) {
        params.currentPageIndex = parseInt(page, 10)
    }

    const url =  encodeURL`/nuxeo/site/api/v1/path/${PATH}/@search?${params}`
    const options = {
        headers: {
            'X-NXDocumentProperties': '*'
        }
    }

    return (dispatch) => {
        dispatch({
            type: SEARCH_FOR,
            payload: { text },
        })
        dispatch(routeSearchFor(text, page))

        httpGET(url, options).then(json).then((results) => {
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
