import React from 'react'

const { any, node } = React.PropTypes

export default class App extends React.Component {
    static displayName = 'App'

    static propTypes = {
        history: any,
        children: node.isRequired,
    }

    render() {
        if (process.env.NODE_ENV !== 'production') {
            window.appHistory = this.props.history
        }

        return this.props.children
    }
}
