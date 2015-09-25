
export function json(response) {
    return response.json()
}

export function httpGET(url, options = {}) {
    options = {
        ...options,
        headers: {
            ...options.headers,
            Accept: 'application/json',
        },
        credentials: 'include',
    }

    return fetch(url, options).catch(e => {
        console.warn('ERROR', e) // eslint-disable-line no-console
        console.warn(e.stack) // eslint-disable-line no-console
        throw e
    })
}

function encodeParameters(params) {
    return Object.keys(params).map((key) => {
        let value = params[key]
        if (value == null) { value = '' }
        if (encodeURIComponent(key) != key) {
            // I don't know if keys should be encoded or not. Probaby you just
            // shouldn't use a key that needs encoding
            throw new Error("Invalid key")
        }

        return key + '=' + encodeURIComponent(value)
    }).join('&')
}

export function encodeURL(strings, ...values) {
    return strings.reduce((out, next, index) => {
        out = out + next
        // strings always has one more element than values
        if (index < values.length) {
            if (typeof values[index] === 'object') {
                return out + encodeParameters(values[index])
            }
            return out + encodeURIComponent(values[index])
        }
        return out
    }, '')
}
