import { httpGET, json } from './utils'
import { encodeURL } from './utils.js'

export const DOCUMENT = "DOCUMENT"

export function loadDocument(uid) {
    const options = {
        headers: {
            'X-NXDocumentProperties': '*'
        }
    }

    const url = encodeURL`/nuxeo/api/v1/id/${uid}`

    return (dispatch) => {
        return httpGET(url, options).then(json).then((result) => {
            dispatch({
                type: DOCUMENT,
                payload: result
            })

        })

    }
}
