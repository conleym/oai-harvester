import React from 'react'

export default class App extends React.Component {
    static displayName = 'App'

    render() {
        if (process.env.NODE_ENV !== 'production') {
            window.appHistory = this.props.history
        }

        return this.props.children
    }
}
