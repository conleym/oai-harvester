import test from 'tape'
import { selectResults, isDocumentReady } from '../selectors.js'

const generateState = (text, catalogs, searchResults) => ({
    criteria: { text, catalogs },
    searchResults
})

test('selectResults requires the current criteria', assert => {
    const expected = {
        totalSize: 1,
        pageSize: 20,
        entries: [ { } ]
    }
    const state = generateState('jpeg', [ 'a' ], expected)

    const actual = selectResults('jpeg', [ 'a' ])(state)

    assert.deepEqual(actual, expected)

    assert.end()
})

test('selectResults requires the current criteria', assert => {
    const searchResults = {
        totalSize: 1,
        pageSize: 20,
        entries: [ { } ]
    }
    const state = generateState('jpeg', [ 'a' ], searchResults)

    const actualCatalog = selectResults('jpeg', [ ])(state)
    const actualText = selectResults('jpg', [ 'a' ])(state)
    const expected = { entities: [] }

    assert.deepEqual(actualCatalog, expected)
    assert.deepEqual(actualText, expected)

    assert.end()
})

import { selectDocument } from '../selectors.js'

test('documentImport checks a "success" in hrv:retrievalStatus', assert => {
    const isReady = isDocumentReady('b1cc5bdf')
    const store = {
        documents: {
            'b1cc5bdf': {
                properties: { }
            }
        }
    }

    assert.equal(selectDocument('b1cc5bdf')(store), store.documents['b1cc5bdf'])

    assert.equal(isReady(store), false)

    store.documents['b1cc5bdf'].properties['hrv:retrievalStatus'] = 'success'

    assert.equal(isReady(store), true)

    assert.end()

})
