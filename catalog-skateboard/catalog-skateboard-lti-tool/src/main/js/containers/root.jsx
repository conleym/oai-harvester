import React from 'react'
import { Provider } from 'react-redux'
import App from './app.jsx'

export default class Root extends React.Component {
    static displayName = 'Root'

    // Because of context differences we need to pass a function to `<Provider>`
    // in React 0.13, but will be able to pass the content starting in 0.14
    providerContent() {
        return (
            <App />
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
                    {this.providerContent}
                </Provider>
                {this.renderDevtools()}
            </div>
        )
    }
}
