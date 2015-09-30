import DataLoader from 'dataloader'
import { encodeURL, httpGET, json } from './search.js'
import { routeReturnUrl } from './route.js'
export const DOCUMENT_LOAD_ERROR = 'DOCUMENT_LOAD_ERROR'
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

function documentReady(doc) {
    if (doc && doc.properties && doc.properties['file:content'] != null) {
        return true
    }

    return false
}

const selectDocument = (id) => (state) => state.documents[id]

function nxDownloadContent(id) {
    // TODO: Make an API request for Nuxeo to download the content
}


const POLL_TIMEOUT = 30000
const POLL_INTERVAL = 10000

export function waitForDocument(id) {
    const selector = selectDocument(id)

    return (dispatch, getState) => {
        if (documentReady(selector(getState()))) {
            const document = selector(getState())
            window.location = routeReturnUrl(document).url
            return
        }

        let hasTimedOut = false
        setTimeout(() => { hasTimedOut = true }, POLL_TIMEOUT)

        nxDownloadContent(id)

        function poll() {
            if (hasTimedOut) {
                dispatch({
                    type: DOCUMENT_LOAD_ERROR,
                    payload: {
                        message: 'Timeout',
                        id ,
                    },
                })
                return
            }
            documentLoader.clear(id)
            documentLoader.load(id).then((doc) => {
                dispatch({
                    type: DOCUMENT,
                    payload: doc
                })
                return selector(getState())
            }).then((document) => {
                if (documentReady(document)) {
                    window.location = routeReturnUrl(document).url
                    return
                }
                setTimeout(poll, POLL_INTERVAL)
            }).catch((error) => {
                dispatch({
                    type: DOCUMENT_LOAD_ERROR,
                    payload: {
                        message: error.message,
                        id,
                    },
                })

            })
        }

        poll()
    }
}
