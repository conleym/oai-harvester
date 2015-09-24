import { routeSearchFor } from './route.js'

export const SEARCH_FOR = 'SEARCH_FOR'
export const SEARCH_RESULTS = 'SEARCH_RESULTS'
export const CHANGE_CATALOG = 'CHANGE_CATALOG'

export function json(response) {
    return response.json()
}

export function httpGET(url, options = {}) {
    options = {
        ...options,
        headers: {
            ...options.headers,
            Accept: 'application/json',
        },
        credentials: 'include',
    }

    return fetch(url, options).catch(e => {
        console.warn('ERROR', e) // eslint-disable-line no-console
        console.warn(e.stack) // eslint-disable-line no-console
        throw e
    })
}

function encodeParameters(params) {
    return Object.keys(params).map((key) => {
        let value = params[key]
        if (value == null) { value = '' }
        if (encodeURIComponent(key) != key) {
            // I don't know if keys should be encoded or not. Probaby you just
            // shouldn't use a key that needs encoding
            throw new Error("Invalid key")
        }

        return key + '=' + encodeURIComponent(value)
    }).join('&')
}

export function encodeURL(strings, ...values) {
    return strings.reduce((out, next, index) => {
        out = out + next
        // strings always has one more element than values
        if (index < values.length) {
            if (typeof values[index] === 'object') {
                return out + encodeParameters(values[index])
            }
            return out + encodeURIComponent(values[index])
        }
        return out
    }, '')
}

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
