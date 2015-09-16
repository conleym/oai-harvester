import './setup_fetch.js'
import React from 'react'
import Root from './containers/root.jsx'
import configureStore from './configure_store.js'

const store = configureStore()

React.render(
    React.createElement(Root, { store }),
    document.getElementById('root')
)
