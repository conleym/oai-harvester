import { httpGET, httpPOST, json, encodeURL } from './utils.js'
import DataLoader from 'dataloader'
import { selectDocument, isDocumentReady } from '../selectors.js'

export const DOCUMENT_LOAD_ERROR = 'DOCUMENT_LOAD_ERROR'
export const CLEAR_DOCUMENT_LOAD_ERROR = 'CLEAR_DOCUMENT_LOAD_ERROR'
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

const DOCUMENT_IMPORT_TIMEOUT = 30000
const DOCUMENT_IMPORT_INTERVAL = 10000

export function documentImport(id) {
    const selector = selectDocument(id)
    const isReady = isDocumentReady(id)

    return (dispatch, getState) => {
        dispatch({
            type: CLEAR_DOCUMENT_LOAD_ERROR,
            payload: { id }
        })

        let isDone = false
        const done = (error) => {
            if (!isDone && error != null) {
                return dispatch({
                    type: DOCUMENT_LOAD_ERROR,
                    payload: {
                        id,
                        message: error
                    }
                })
            }
            isDone = true
        }

        if (isReady(getState())) {
            return done()
        }

        documentImport.nxDownloadContent(id)

        setTimeout(() => {
            done('Timeout')
        }, DOCUMENT_IMPORT_TIMEOUT)

        function poll() {
            if (isDone) { return }

            documentImport.refreshDocument(id).then((doc) => {
                dispatch({
                    type: DOCUMENT,
                    payload: doc
                })
                return selector(getState())
            }).then((document) => {
                if (isReady(getState())) {
                    return done()
                }
                setTimeout(poll, DOCUMENT_IMPORT_INTERVAL)
            }).catch((error) => {
                return done(error.message)
            })
        }

        poll()
    }
}
// These are just being attached so they can be mocked in tests.
Object.assign(documentImport, {
    nxDownloadContent(id) {
        const url = encodeURL`/nuxeo/site/api/v1/id/${id}/@op/UnizinCMP.RetrieveCopyFromSourceRepository`
        return httpPOST(url, { params: {} })
    },
    refreshDocument(id) {
        documentLoader.clear(id)
        return documentLoader.load(id)
    }
})
