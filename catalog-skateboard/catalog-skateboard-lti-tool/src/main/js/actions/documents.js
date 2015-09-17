import DataLoader from 'dataloader'
import { encodeURL, httpGET, json } from './search.js'

export const DOCUMENT = 'DOCUMENT'

const documentLoader = new DataLoader(docIds => {
    const options = {
        headers: {
            'X-NXDocumentProperties': '*'
        }
    }

    const ids = docIds.map(id => `'${id}'`).join(', ')
    const query = `select * from Document where ecm:uuid in (${ids})`
    const url = encodeURL`/nuxeo/site/api/v1/query?query=${query}`

    return httpGET(url, options).then(json).then((results) => {
        const map = results.entries.reduce((memo, next) => {
            memo[next.uid] = next
            return memo
        }, {})

        return docIds.map((id) => map[id])
    })
})

window.documentLoader = documentLoader

export function ensureDocument(id) {
    return (dispatch, getState) => {
        const { documents } = getState()

        if (!documents[id]) {
            documentLoader.load(id).then((doc) => {
                dispatch({
                    type: DOCUMENT,
                    payload: doc
                })
            })
        }
    }
}
