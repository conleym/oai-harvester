import React from 'react'
import { connect } from 'react-redux'
import { fetchCatalogs } from '../actions/search.js'


class App extends React.Component {
    static displayName = 'App'

    componentWillMount() {
        this.props.fetchCatalogs()
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
  { fetchCatalogs }
)(App)
