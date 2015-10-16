import test from 'tape'
import {
    documentImport,
    CLEAR_DOCUMENT_LOAD_ERROR,
} from '../documents.js'
import * as selectors from '../../selectors'
import { createSpy, spyOn, restoreAllSpies } from '../../spies'


const after = test

test('documentImport does nothing if the document is ready', assert => {
    const dispatch = createSpy()
    const getState = function() {
        return { documents: { foo: { } } }
    }

    spyOn(selectors, 'isDocumentReady',
        ( id => state => true )
    )
    documentImport('foo')(dispatch, getState)

    assert.equal(dispatch.calls, 1, 'dispatch is called once')
    assert.equal(dispatch.args[0][0].type, CLEAR_DOCUMENT_LOAD_ERROR, 'with CLEAR_DOCUMENT_LOAD_ERROR')

    restoreAllSpies()
    assert.end()
})

test("documentImport tells Nuxeo to download the content if it isn't ready", assert => {
    const dispatch = createSpy()
    const getState = function() {
        return { documents: { foo: { } } }
    }

    spyOn(selectors, 'isDocumentReady',
        ( id => state => false )
    )
    spyOn(documentImport, 'nxDownloadContent')
    spyOn(documentImport, 'poll', () => Promise.resolve({}))
    documentImport('foo')(dispatch, getState)

    assert.equal(documentImport.nxDownloadContent.calls, 1, 'nxDownloadContent was called once')

    restoreAllSpies()
    assert.end()
})

test("documentImport polls every 10 seconds for 5 minutes", assert => {
    const dispatch = createSpy()
    const getState = function() {
        return { documents: { foo: { } } }
    }

    spyOn(selectors, 'isDocumentReady',
        ( id => state => false )
    )
    spyOn(documentImport, 'nxDownloadContent')
    spyOn(documentImport, 'poll', () => Promise.resolve({}))
    documentImport('foo')(dispatch, getState)

    assert.equal(documentImport.poll.calls, 1, 'poll was called once')
    const { timeout, interval } = documentImport.poll.args[0][0]
    assert.equal(timeout, 5 * 60 * 1000, 'timeout: 30 seconds')
    assert.equal(interval, 10000, 'interva: 10 seconds')

    restoreAllSpies()
    assert.end()
})


after('after', assert => {
    restoreAllSpies()
    assert.end()
})
