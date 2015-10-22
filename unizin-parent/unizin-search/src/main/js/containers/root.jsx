import React from 'react'
import { Router, Route } from 'react-router'
import { Provider } from 'react-redux'
import App from './app.jsx'
import Search from './smart_search.jsx'
import Result from './result.jsx'
import Insert from './insert.jsx'
import Preview from './preview.jsx'

const { any, shape, func } = React.PropTypes

export default class Root extends React.Component {
    static displayName = 'Root'

    static propTypes = {
        history: any,
        store: shape({
            getState: func.isRequired,
            dispatch: func.isRequired,
        }).isRequired,
    }

    // Because of context differences we need to pass a function to `<Provider>`
    // in React 0.13, but will be able to pass the content starting in 0.14
    providerContent() {
        return (
            <Router history={this.props.history}>
                <Route component={App}>
                    <Route path="/result/:uid" component={Result} />
                    <Route path="/search" component={Search} />
                    <route path="/insert/:uid" component={Insert} />
                    <route path="/preview/:uid" component={Preview} />
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
