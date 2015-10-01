import React from 'react'
import styles from './search.scss'
import { connect } from 'react-redux'
import { changeCatalog, searchFor } from '../actions/search.js'
import { routeSearchFor } from '../actions/route.js'
import SearchResults from '../components/search_results.jsx'
import Footer from '../components/footer.jsx'
import CatalogSelector from '../components/catalog_selector.jsx'
import difference from 'lodash.difference'
import classNames from 'classnames'

function loadData(props) {
    const { criteria } = props
    const { search, catalogs = [], page = 1 } = props.location.query

    if (criteria == null
        || search !== criteria.text
        || difference(criteria.catalogs, catalogs).length > 0
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
        const searchBtnClasses = classNames("btn", "primary", styles.btn)
        let { page } = this.props.location.query
        if (page) { page = parseInt(page, 10) }

        const mainClasses = (criteria.text == null) ? classNames(styles.container,styles.empty) : styles.container
        const brandURL = require('file!../../resources/skin/resources/brand.svg')

        return (
            <main className={mainClasses} role="main">
              <div className={styles.header}>
                { criteria.text == null ? (
                  <div className={styles.brand}>
                    <img src={brandURL} />
                    <h1>Content Discovery Tool</h1>
                  </div>
                ) : null}
                <form className={styles.search} onSubmit={this.onSearch.bind(this)} role="search"
                      aria-label="Search for catalog items">

                    <input type="text" id="searchInput" ref="searchInput" placeholder="Enter search criteria" />
                    <label htmlFor="searchInput" className="aural">Enter search criteria</label>
                    <input type="submit" value="Search" className={searchBtnClasses} />
                </form>
              </div>
              <div className={styles['results-container']}>
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
              <Footer />
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
