import { createStore, applyMiddleware } from 'redux'
import * as reducers from './reducers'
import { combineReducers } from 'redux'
import thunk from 'redux-thunk'

const rootReducer = combineReducers(reducers)

const middleware = [
    thunk
]
if (process.env.NODE_ENV !== 'production') {
    const loggerMiddleware = require('redux-logger')
    middleware.push(
        loggerMiddleware()
    )
}

const createStoreWithMiddleware = applyMiddleware(...middleware)(createStore)

/**
 * Creates a preconfigured store for this example.
 */
export default function configureStore(initialState) {
    return createStoreWithMiddleware(rootReducer, initialState)
}
