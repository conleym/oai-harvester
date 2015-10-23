import {
    SET_BATCH_ID,
    SUBMIT,
    DOCUMENT,
    UPLOAD,
    UPLOAD_PROGRESS,
    UPLOAD_ERROR,
    UPLOAD_THUMBNAIL
} from './actions/uploads'
import { RESET } from './actions/route.js'

export { routerStateReducer as router } from 'redux-router'

export function step(state = "upload", action) {
    switch (action.type) {
    case RESET:
        return 'upload'
    case UPLOAD:
        return 'form'
    case SUBMIT:
        return 'finish_upload'
    case DOCUMENT:
        return 'success'
    }

    return state
}

export function placeholder(state = {}, action) {
    return state
}

export function batchId(state = null, action) {
    switch (action.type) {
    case RESET:
        return null
    case SET_BATCH_ID:
        return action.payload
    }
    return state
}

export function formData(state = {}, action) {

    switch (action.type) {
    case RESET:
        return {}
    case SUBMIT:
        return action.payload
    }

    return state
}

export function files(state = {}, action) {
    switch (action.type) {
    case RESET:
        return {}
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
