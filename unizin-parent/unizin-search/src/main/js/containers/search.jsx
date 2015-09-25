import React from 'react'
import styles from './search.scss'
import { connect } from 'react-redux'
import { changeCatalog, searchFor } from '../actions/search.js'
import { routeSearchFor } from '../actions/route.js'
import SearchResults from '../components/search_results.jsx'
import CatalogSelector from '../components/catalog_selector.jsx'


function loadData(props) {
    const { criteria } = props
    const { search, page } = props.location.query

    if (criteria == null || search !== criteria.text) {
        props.searchFor(search, page)
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
        let { page } = this.props.location.query
        if (page) { page = parseInt(page, 10) }

        return (
            <main className={styles['search-container']} role="main">
              <div className="header">
                <form className={styles['search-form']} onSubmit={this.onSearch.bind(this)} role="search"
                      aria-label="Search for catalog items">

                    <input id="searchInput" ref="searchInput" placeholder="Enter search criteria" />
                    <label htmlFor="searchInput" className="aural">Enter search criteria</label>
                    <input type="submit" value="Search" />
                </form>
              </div>
              <div className={styles['results-container']}>
                { criteria.text != null ? (
                    <CatalogSelector
                        onChange={this.props.changeCatalog}
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
            </main>
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
