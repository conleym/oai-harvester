import React from 'react'
import { Route } from 'react-router'
import { ReduxRouter } from 'redux-router'
import { Provider } from 'react-redux'
import App from './app.jsx'
import SmartHome from './smart_home.jsx'

const { any, shape, func } = React.PropTypes

export const routes = (
    <Route component={App}>
        <Route path="/" component={SmartHome} />
    </Route>
)

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
            <ReduxRouter>{routes}</ReduxRouter>
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
