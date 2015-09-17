import React from 'react'
import { Router, Route } from 'react-router'
import { Provider } from 'react-redux'
import App from './app.jsx'
import Search from './search.jsx'
import Result from './result.jsx'

export default class Root extends React.Component {
    static displayName = 'Root'

    // Because of context differences we need to pass a function to `<Provider>`
    // in React 0.13, but will be able to pass the content starting in 0.14
    providerContent() {
        return (
            <Router history={this.props.history}>
                <Route component={App}>
                    <Route path="/result/:uid" component={Result} />
                    <Route path="/search" component={Search} />
                    <Route path="/" component={Search} />
                </Route>
            </Router>
        )
    }

    renderDevtools() {
        if (process.env.NODE_ENV !== 'production') {
            const { DevTools, DebugPanel, LogMonitor } = require('redux-devtools/lib/react')

            return (
                <DebugPanel top right bottom>
                    <DevTools store={this.props.store} monitor={LogMonitor}  visibleOnLoad={false} />
                </DebugPanel>
            )
        }
    }

    render() {
        return (
            <div>
                <Provider store={this.props.store}>
                    {this.providerContent.bind(this)}
                </Provider>
                {this.renderDevtools()}
            </div>
        )
    }
}
