import React from 'react'
import Root from './containers/app.jsx'
import configureStore from './configure_store.js'

const SNAPSHOT_KEY = '_snapshot_'
let snapshot

if (process.env.NODE_ENV !== 'production') {
    try {
        if (localStorage.getItem(SNAPSHOT_KEY)) {
            console.warn('Restoring store from snapshot') // eslint-disable-line no-console
            snapshot = JSON.parse(localStorage.getItem(SNAPSHOT_KEY))
        }
    } catch (e) {
        console.log("Couldn't restore snapshot") // eslint-disable-line no-console
        console.error(e.message) // eslint-disable-line no-console
    }
}

const store = configureStore(snapshot)

if (process.env.NODE_ENV !== 'production') {
    window.React = React
    window.store = store

    window.snapshot = {
        save() {
            const snapshot = JSON.stringify(store.getState())
            localStorage.setItem(SNAPSHOT_KEY, snapshot)
            location.reload()
        },
        clear() {
            localStorage.removeItem(SNAPSHOT_KEY)
            location.reload()
        }
    }
}

React.render(
    React.createElement(Root, { store }),
    document.getElementById('root')
)
