import { httpPOST, json } from './utils'

export const SET_BATCH_ID = 'SET_BATCH_ID'
export const UPLOAD = 'UPLOAD'
export const UPLOAD_PROGRESS = 'UPLOAD_PROGRESS'
export const UPLOAD_ERROR = 'UPLOAD_ERROR'
export const UPLOAD_THUMBNAIL = 'UPLOAD_THUMBNAIL'

export function uploadFile(key, file) {
    const { name, size } = file

    return {
        type: UPLOAD,
        payload: {
            key,
            name,
            size,
            progress: 0,
            bytesSent: 0,
            thumbnail: null,
        }
    }
}

export function uploadProgress(key, progress, bytesSent) {
    return {
        type: UPLOAD_PROGRESS,
        payload: { key, progress, bytesSent }
    }
}

export function uploadThumbnail(key, thumbnail) {
    return {
        type: UPLOAD_THUMBNAIL,
        payload: { key, thumbnail }
    }
}

export function uploadError(key, error) {
    return {
        type: UPLOAD_ERROR,
        payload: {key, error}
    }
}

export function getBatchId() {
    return (dispatch) => {
        httpPOST('/nuxeo/api/v1/upload/').then(json).then(({batchId}) => {
            dispatch({
                type: SET_BATCH_ID,
                payload: batchId
            })
        })
    }
}
