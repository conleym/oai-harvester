import { CHANGE_CATALOG, SEARCH_FOR, SEARCH_RESULTS } from './actions/search.js'
import { LOCATION_CHANGED } from './actions/route.js'
import { DOCUMENT, DOCUMENT_LOAD_ERROR } from './actions/documents.js'

export function documents(state = {}, action) {
    if (action.type === DOCUMENT) {
        return {
            ...state,
            [action.payload.uid]: action.payload
        }
    }
    if (action.type === DOCUMENT_LOAD_ERROR) {
        const { id } = action.payload
        const doc = {
            ...state[id],
            loadError: action.payload.message
        }
        return {
            ...state,
            [id]: doc
        }
    }

    return state
}

const defaultResults = {
    entries: [],
}
export function searchResults(state = defaultResults, action) {
    if (action.type === SEARCH_FOR) {
        // Reset on search
        return defaultResults
    }

    if (action.type === SEARCH_RESULTS) {
        const { totalSize, pageSize, entries } = action.payload.results

        return {
            totalSize,
            pageSize,
            entries
        }
    }

    return state
}

export function criteria(state = {}, action) {
    if (action.type === SEARCH_FOR) {
        return {
            text: action.payload.text
        }
    }

    return state
}

export function location(state = {}, action) {
    const { type, payload } = action
    if (type === LOCATION_CHANGED) {
        return payload
    }
    return state
}

const defaultCatalogs = {
    contentRelay: {
        label: 'Content Relay',
        enabled: true,
    },
    hathi: {
        label: 'Hathi Trust',
        enabled: true,
    },
    jstor: {
        label: 'JSTOR',
        enabled: true,
    },
    spiders: {
        label: 'Web of Science',
        enabled: true,
    }
}

export function catalogs(state = defaultCatalogs, action) {
    if (action.type === CHANGE_CATALOG) {
        const { key, enabled } = action.payload
        return {
            ...state,
            [key]: {
                ...state[key],
                enabled
            }
        }
    }
    return state
}
