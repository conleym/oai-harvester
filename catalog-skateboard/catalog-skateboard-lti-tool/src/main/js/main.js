import React from 'react'
import Root from './containers/app.jsx'
import configureStore from './configure_store.js'

const store = configureStore()

if (process.env.NODE_ENV !== 'production') {
    window.React = React
    window.store = store
}

React.render(
    React.createElement(Root, { store }),
    document.getElementById('root')
)
