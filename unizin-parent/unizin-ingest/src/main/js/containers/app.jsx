import React from 'react'
import { connect } from 'react-redux'

const { any, node, func } = React.PropTypes

class App extends React.Component {
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

function mapStateToProps(state) {
    return {}
}

export default connect(
  mapStateToProps,
  { }
)(App)
