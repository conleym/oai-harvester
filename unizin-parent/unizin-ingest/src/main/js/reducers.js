import { SET_BATCH_ID } from './actions/uploads'

export { routerStateReducer as router } from 'redux-router'

export function placeholder(state = {}, action) {
    return state
}

export function batchId(state = null, action) {

    if (action.type === SET_BATCH_ID) {
        return action.payload
    }
    return state
}
