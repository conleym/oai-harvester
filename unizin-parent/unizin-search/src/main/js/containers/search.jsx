import React from 'react'
import styles from './search.scss'
import { connect } from 'react-redux'
import { changeCatalog, searchFor } from '../actions/search.js'
import { routeSearchFor } from '../actions/route.js'
import SearchResults from '../components/search_results.jsx'
import CatalogSelector from '../components/catalog_selector.jsx'
import difference from 'lodash.difference'

function loadData(props) {
    const { criteria } = props
    const { search, catalogs = [], page = 1 } = props.location.query

    if (criteria == null
        || search !== criteria.text
        || difference(catalogs, criteria.catalogs).length > 0) {
        props.searchFor(search, catalogs, page)
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
        const { catalogs, criteria, location } = this.props
        let { catalogs: selectedCatalogs = [] } = location.query

        // If it's the first search the location won't have any of the catalogs
        // and we need to default to all
        if (criteria.text == null) {
            selectedCatalogs = Object.keys(catalogs)
        }

        const { value } = React.findDOMNode(this.refs.searchInput)

        this.props.routeSearchFor(value, selectedCatalogs)
    }

    changeCatalog(catalogIds) {
        const { text: value } = this.props.criteria

        this.props.routeSearchFor(value, catalogIds)
    }

    render() {
        const { catalogs, criteria, searchResults, location } = this.props
        const { catalogs: selectedCatalogs } = location.query
        let { page } = this.props.location.query
        if (page) { page = parseInt(page, 10) }

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
                          onChange={this.changeCatalog.bind(this)}
                          selected={selectedCatalogs}
                          catalogs={catalogs} />
                  ) : null}
                  { criteria.text != null ? (
                      <SearchResults
                          searchFor={this.props.searchFor}
                          page={page}
                          criteria={criteria}
                          results={searchResults} />
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
