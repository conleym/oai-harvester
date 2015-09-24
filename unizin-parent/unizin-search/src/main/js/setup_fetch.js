// Fetch requires es6 promises
if (window.Promise == null) {
    window.Promise = require('es6-promise')
}

// dataloader requires es6 maps
if (window.Map == null) {
    window.Map = require('es6-map')
}
// Fetch is a pollyfill for https://fetch.spec.whatwg.org that will attach
// itself to window.
//
// https://github.com/github/fetch
require('whatwg-fetch')
