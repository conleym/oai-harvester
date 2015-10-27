import { httpPOST, json } from './utils'
import { routeSuccess } from './route'

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

export const SUBMIT = 'SUBMIT'

export function submit(data) {
    return {
        type: SUBMIT,
        payload: data
    }
}

function slugify(title) {
    return title.replace(/[^\w]/g, '-').replace(/--+/, '-')
}

let generatingDocument = false
export function generateDocument() {
    const url = '/nuxeo/api/v1/path/default-domain/workspaces/uploads'

    return (dispatch, getState) => {
        if (generatingDocument) { return }
        generatingDocument = true
        const { batchId, formData } = getState()

        const body = {
            "entity-type": "document",
            "name": slugify(formData.title),
            "type": "File",
            "properties" : {
                "dc:title": formData.title,
                "dc:description": formData.description,
                "file:content": {
                    "upload-batch": batchId,
                    "upload-fileId":"0"
                }
            }
        }

        httpPOST(url, body).then(json).then((result) => {
            const { uid } = result
            dispatch(routeSuccess(uid))
        })
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
