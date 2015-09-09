// Fetch requires es6 promises
if (window.Promise == null) {
    window.Promise = require('native-promise-only')
}
// Fetch is a pollyfill for https://fetch.spec.whatwg.org that will attach
// itself to window.
//
// https://github.com/github/fetch
require('whatwg-fetch')
