import { compose, createStore, applyMiddleware } from 'redux'
import { reduxReactRouter } from 'redux-router'
import { routes } from './containers/root.jsx'
import { useQueries } from 'history'
import createHistory from 'history/lib/createHashHistory'

import * as reducers from './reducers'
import { combineReducers } from 'redux'
import thunk from 'redux-thunk'

const rootReducer = combineReducers(reducers)

const middleware = [
    thunk,
]
let createStoreWithMiddleware

if (process.env.NODE_ENV !== 'production') {
    const { devTools, persistState } = require('redux-devtools')

    let key
    try {
        // `window` would refer to the page inside the iframe where `top` will refer
        // to the location outside the iframe
        key = top.location.href.match(/[?&]debug_session=([^#&]+)\b/)[1]
    } catch (e) {
        key = undefined
    }

    createStoreWithMiddleware = compose(
        applyMiddleware(...middleware),
        reduxReactRouter({
            routes,
            createHistory: useQueries(createHistory)
        }),
        devTools(),
        persistState(key)
    )(createStore)
} else {
    createStoreWithMiddleware = compose(
        applyMiddleware(...middleware),
        reduxReactRouter({
            routes,
            createHistory: useQueries(createHistory)
        })
    )(createStore)
}


/**
 * Creates a preconfigured store for this example.
 */
export default function configureStore(initialState) {
    return createStoreWithMiddleware(rootReducer, initialState)
}
