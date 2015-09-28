import { FETCH_CATALOGS, CHANGE_CATALOG, SEARCH_FOR, SEARCH_RESULTS } from './actions/search.js'
import { LOCATION_CHANGED } from './actions/route.js'
import { DOCUMENT } from './actions/documents.js'

export function documents(state = {}, action) {
    if (action.type === DOCUMENT) {
        return {
            ...state,
            [action.payload.uid]: action.payload
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
        const { totalSize, entries } = action.payload.results

        return {
            totalSize,
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

export function catalogs(state = {}, action) {
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
    if (action.type === FETCH_CATALOGS) {
        const enabled = true
        return action.payload.reduce((catalogs, record) => {
            const { id, label } = record.properties
            catalogs[id] = { label, enabled }
            return catalogs
        }, {})
    }
    return state
}
