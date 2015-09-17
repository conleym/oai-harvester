import { compose, createStore, applyMiddleware } from 'redux'
import * as reducers from './reducers'
import { combineReducers } from 'redux'
import routing from './middleware/routing.js'
import thunk from 'redux-thunk'

const rootReducer = combineReducers(reducers)

const middleware = [
    thunk,
    routing,
]
let createStoreWithMiddleware

if (process.env.NODE_ENV !== 'production') {
    const { devTools, persistState } = require('redux-devtools')

    // `window` would refer to the page inside the iframe where `top` will refer
    // to the location outside the iframe
    const location = top.location

    createStoreWithMiddleware = compose(
        applyMiddleware(...middleware),
        devTools(),
        persistState(location.href.match(/[?&]debug_session=([^&]+)\b/))
    )(createStore)
} else {
    createStoreWithMiddleware = applyMiddleware(...middleware)(createStore)
}


/**
 * Creates a preconfigured store for this example.
 */
export default function configureStore(initialState) {
    return createStoreWithMiddleware(rootReducer, initialState)
}
