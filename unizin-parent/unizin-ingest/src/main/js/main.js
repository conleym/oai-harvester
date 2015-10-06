// http://webpack.github.io/docs/configuration.html#output-publicpath
const scripts = document.getElementsByTagName("script")
const src = scripts[scripts.length - 1].getAttribute("src")

/*eslint-disable no-undef*/
__webpack_public_path__ = src.substr(0, src.lastIndexOf("/") + 1)
/*eslint-enable no-undef*/


import '../css/main.scss'
import './setup_fetch.js'
import React from 'react'
import Root from './containers/root.jsx'
import configureStore from './configure_store.js'
import { useQueries } from 'history'
import createHistory from 'history/lib/createHashHistory'
// import { setupHistory } from './actions/route.js'

const store = configureStore()
const history = useQueries(createHistory)({
    // queryKey: false
})
// setupHistory(history, store)

if (process.env.NODE_ENV !== 'production') {
    window.store = store
    window.rHistory = history
    window.reactRouter = require('react-router')
    window.React = React
}

React.render(
    React.createElement(Root, { store, history }),
    document.getElementById('root')
)
