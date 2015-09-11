
export const SEARCH_FOR = 'SEARCH_FOR'
export const SEARCH_RESULTS = 'SEARCH_RESULTS'

function json(response) {
    return response.json()
}
function httpGET(url, options) {
    options = Object.assign({
        credentials: 'include',
    }, options)

    return fetch(url, options)
}

export function encodeURL(strings, ...values) {
    return strings.reduce((out, next, index) => {
        out = out + next
        if (index < values.length) {
            return out + encodeURIComponent(values[index])
        }
        return out
    }, '')
}


const PATH = 'default-domain'
export function searchFor(text) {
    const url = encodeURL`/nuxeo/site/api/v1/path/${PATH}/@search?fullText=${text}`

    return (dispatch) => {
        dispatch({
            type: SEARCH_FOR,
            payload: { text },
        })

        const options = {
            headers: {
                'X-NXDocumentProperties': '*'
            }
        }

        httpGET(url, options).then(json).then((results) => {
            dispatch({
                type: SEARCH_RESULTS,
                payload: { results }
            })
        })

    }

}
