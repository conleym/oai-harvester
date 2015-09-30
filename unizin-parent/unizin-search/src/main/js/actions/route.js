import { ROUTE } from '../middleware/routing.js'
import { encodeURL } from './search.js'

export const LOCATION_CHANGED = 'LOCATION_CHANGED'
export let history

export function locationChanged(location) {
    // http://rackt.github.io/history/stable/Location.html
    return {
        type: LOCATION_CHANGED,
        payload: {
            // copying the keys does 2 things:
            // 1. it documents what is available
            // 2. it ensures this action is serializable even if location isn't
            pathname: location.pathname,
            search: location.search,
            state: location.state,
            action: location.action,
            key: location.key,
        }
    }
}

// This should only be called from main.js
export function setupHistory(h, store) {
    history = h

    history.listen((location) => {
        store.dispatch(locationChanged(location))
    })
}


export function route(route, query) {
    return {
        // I like the symbol as a key to catch in the middleware
        [ROUTE]: { route, query },
        // Views can call an action and pull the route key without dispatching
        // the action.
        route,
        query,
        url: history.createHref(route, query)
    }
}

export function routeSearchFor(search, page = 1) {
    return route('/search', { search, page })
}

export function routeResult(item) {
    return route(encodeURL`/result/${item.uid}`)
}

export function routeInsert(item) {
    return route(encodeURL`/insert/${item.uid}`)
}

export function routeReturnUrl(item) {
    const { path, title } = item
    const url = location.protocol + '//' + location.hostname + path

    const query = encodeURL`?return_type=url&url=${url}&text=${title}&target=_blank`

    return {
        url: window.lti_data.ext_content_return_url + query
    }
}

export function routePreviewUrl(item) {
    const { protocol, hostname } = window.location
    const prefix = `${protocol}//${hostname}`

    const params = {
        file: prefix + encodeURL`/nuxeo/api/v1/id/${item.uid}/@blob/blobholder:0`
    }

    return {
        url: prefix + encodeURL`/nuxeo/viewer/web/viewer.html?${params}`
    }
}
