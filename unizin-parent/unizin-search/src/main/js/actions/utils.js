
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

export function httpPOST(url, data, options = {}) {
    options = {
        ...options,
        body: JSON.stringify(data),
        headers: {
            Accept: 'application/json',
            'Content-Type': 'application/json',
            ...options.headers,
        },
        credentials: 'include',
    }

    return fetch(url, options).catch(e => {
        console.warn('ERROR', e) // eslint-disable-line no-console
        console.warn(e.stack) // eslint-disable-line no-console
        throw e
    })
}

function weave(a, b) {
    if (a.length !== b.length + 1) {
        // I just need this for handling tagged template litterals, where the
        // first list is always exactly 1 longer than the 2nd list
        throw new Error('undefined behavior')
    }
    return a.reduce(( out, next, index ) => {
        out.push(next)
        if (index < b.length) {
            out.push(b[index])
        }
        return out
    }, [])
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
    const encodeValue = (value) => {
        if (typeof value == 'object') {
            return encodeParameters(value)
        }
        return encodeURIComponent(value)
    }

    return weave(strings, values.map(encodeValue)).join('')
}

function escapeQuote(input) {
    if (!input) { return '' }

    // If it looks like an array
    if (typeof input.map === 'function') {
        return input.map(escapeQuote).join(', ')
    }

    // toString here handles numbers
    const encoded = input.toString().replace(/["'\\]/g, function(x) { return '\\' + x })
    return `'${encoded}'`
}

export function nxql(strings, ...values) {
    return weave(strings, values.map(escapeQuote)).join('')
}

export function checkValue(val) {
    return (val === null || val.length === 0) ? "Not available" : val
}
