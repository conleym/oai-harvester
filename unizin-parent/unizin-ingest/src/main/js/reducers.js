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

import {
    UPLOAD,
    UPLOAD_PROGRESS,
    UPLOAD_ERROR,
    UPLOAD_THUMBNAIL
} from './actions/uploads'
export function files(state = {}, action) {


    switch (action.type) {
    case UPLOAD: {
        const { key, ...values } = action.payload
        return {
            ...state,
            [key]: values
        }
    }
    case UPLOAD_PROGRESS:
    case UPLOAD_ERROR:
    case UPLOAD_THUMBNAIL:
        const { key, ...values } = action.payload
        return {
            ...state,
            [key]: {
                ...state[key],
                ...values
            }
        }
    }

    return state
}
