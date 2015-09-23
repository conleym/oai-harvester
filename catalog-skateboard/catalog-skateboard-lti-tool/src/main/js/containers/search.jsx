import React from 'react'
import styles from './search.scss'
import { connect } from 'react-redux'
import { changeCatalog, searchFor } from '../actions/search.js'
import { routeSearchFor } from '../actions/route.js'
import SearchResults from '../components/search_results.jsx'
import CatalogSelector from '../components/catalog_selector.jsx'


function loadData(props) {
    const { criteria } = props
    const { search } = props.location.query

    if (criteria == null || search !== criteria.text) {
        props.searchFor(search)
    }
}

class Search extends React.Component {
    static displayName = 'Search'

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
        const { catalogs, criteria, searchResults } = this.props

        if (process.env.NODE_ENV !== 'production') {
            window.appHistory = this.props.history
        }

        return (
            <div className="search-container">
                <form className="search-form" onSubmit={this.onSearch.bind(this)}>

                    { criteria.text == null ? (
                        <label htmlFor="searchInput">
                            Enter Search Term
                        </label>
                    ) : null}


                    <input id="searchInput" ref="searchInput" />
                    <input type="submit" value="Search" />
                </form>
                <div className="results-container">
                  { criteria.text != null ? (
                      <CatalogSelector
                          onChange={this.props.changeCatalog}
                          catalogs={catalogs} />
                  ) : null}
                  { criteria.text != null ? (
                      <SearchResults criteria={criteria} results={searchResults} />
                  ) : null}
                </div>
            </div>
        )
    }
}

function mapStateToProps(state) {
    return {
        catalogs: state.catalogs,
        criteria: state.criteria,
        searchResults: state.searchResults,
    }
}

export default connect(
  mapStateToProps,
  { changeCatalog, searchFor, routeSearchFor }
)(Search)
