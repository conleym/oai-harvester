import test from 'tape'
import {
    documentImport,
    CLEAR_DOCUMENT_LOAD_ERROR,
} from '../documents.js'
import lolex from 'lolex'
import * as selectors from '../../selectors'
import { createSpy, spyOn, restoreAllSpies } from '../../spies'


const after = test

test('documentImport does nothing if the document is ready', assert => {
    const clock = lolex.install(global)
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

    clock.uninstall()
    restoreAllSpies()
    assert.end()
})

test("documentImport tells Nuxeo to download the content if it isn't ready", assert => {
    const clock = lolex.install(global)
    const dispatch = createSpy()
    const getState = function() {
        return { documents: { foo: { } } }
    }

    spyOn(selectors, 'isDocumentReady',
        ( id => state => false )
    )
    spyOn(documentImport, 'nxDownloadContent')
    documentImport('foo')(dispatch, getState)

    assert.equal(documentImport.nxDownloadContent.calls, 1, 'nxDownloadContent was called once')

    clock.uninstall()
    restoreAllSpies()
    assert.end()
})


after('after', assert => {
    restoreAllSpies()
    assert.end()
})
