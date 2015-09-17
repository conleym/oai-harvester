import { SEARCH_FOR, SEARCH_RESULTS } from './actions/search.js'
import { LOCATION_CHANGED } from './actions/route.js'

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
