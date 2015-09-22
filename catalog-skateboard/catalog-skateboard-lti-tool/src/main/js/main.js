import '../resources/skin/resources/css/main.css'
import './setup_fetch.js'
import React from 'react'
import Root from './containers/root.jsx'
import configureStore from './configure_store.js'
import { useQueries } from 'history'
import createHistory from 'history/lib/createHashHistory'
import { setupHistory } from './actions/route.js'

const store = configureStore()
const history = useQueries(createHistory)({
    // queryKey: false
})
setupHistory(history, store)

if (process.env.NODE_ENV !== 'production') {
    window.store = store
    window.rHistory = history
    window.reactRouter = require('react-router')
}

React.render(
    React.createElement(Root, { store, history }),
    document.getElementById('root')
)
