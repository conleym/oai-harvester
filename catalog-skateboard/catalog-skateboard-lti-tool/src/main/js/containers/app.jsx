import React from 'react'
import { connect } from 'react-redux'
import { searchFor } from '../actions/search.js'
import { routeSearchFor } from '../actions/route.js'
import SearchResults from '../components/search_results.jsx'


function loadData(props) {
    const { criteria } = props
    const { search } = props.location.query

    if (criteria == null || search !== criteria.text) {
        props.searchFor(search)
    }
}

class App extends React.Component {
    static displayName = 'App'

    componentWillMount() {
        loadData(this.props)
    }

    componentWillReceiveProps(nextProps) {
        loadData(nextProps)
    }

    onSearch(e) {
        e.preventDefault()
        const { value } = React.findDOMNode(this.refs.searchInput)
        this.props.routeSearchFor(value)
    }

    render() {
        const { criteria, searchResults } = this.props

        if (process.env.NODE_ENV !== 'production') {
            window.appHistory = this.props.history
        }

        return (
            <div>
                <form onSubmit={this.onSearch.bind(this)}>
                    <input ref="searchInput" />
                    <input type="submit" value="Search" />
                </form>

                <SearchResults criteria={criteria} results={searchResults} />
            </div>
        )
    }
}

function mapStateToProps(state) {
    return {
        criteria: state.criteria,
        searchResults: state.searchResults,
    }
}

export default connect(
  mapStateToProps,
  { searchFor, routeSearchFor }
)(App)
