import React from 'react'
import { connect } from 'react-redux'
import { searchFor } from '../actions.js'
import SearchResults from '../components/search_results.jsx'

class App extends React.Component {
    static displayName = 'App'

    onSearch(e) {
        e.preventDefault()
        const { value } = React.findDOMNode(this.refs.searchInput)
        this.props.searchFor(value)
    }

    render() {
        const { criteria, searchResults } = this.props

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
  { searchFor }
)(App)
