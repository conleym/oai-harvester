import React from 'react'
import { connect } from 'react-redux'
import { changeCatalog, searchFor } from '../actions/search.js'
import { routeSearchFor } from '../actions/route.js'
import difference from 'lodash.difference'
import { selectResults } from '../selectors.js'
import Search from '../components/search.jsx'

function loadData(props) {
    const { criteria } = props
    const { search, catalogs = [] } = props.location.query
    let { page = 1 } = props.location.query
    page = parseInt(page, 10)


    if (criteria == null
        || criteria.page != page
        || search !== criteria.text
        || difference(criteria.catalogs, catalogs).length > 0
        || difference(catalogs, criteria.catalogs).length > 0) {
        props.searchFor(search, catalogs, page)
    }
}

// The props are passed in from the router and connect. This doesn't seem very
// useful for this file
/* eslint-disable react/prop-types */

class SmartSearch extends React.Component {
    static displayName = 'SmartSearch'

    constructor(props, context) {
        super(props, context)
        this.selectPage = this.selectPage.bind(this)
    }

    componentWillMount() {
        loadData(this.props)
    }

    componentWillReceiveProps(nextProps) {
        loadData(nextProps)
    }

    selectPage(page) {
        const { location, criteria } = this.props
        const { text: value } = criteria
        const { catalogs: selectedCatalogs = [] } = location.query

        this.props.routeSearchFor(value, selectedCatalogs, page)
    }

    onSearch(value) {
        const { allCatalogs, criteria, location } = this.props
        let { catalogs: selectedCatalogs = [] } = location.query

        // If it's the first search the location won't have any of the catalogs
        // and we need to default to all
        if (criteria.text == null) {
            selectedCatalogs = Object.keys(allCatalogs)
        }

        this.props.routeSearchFor(value, selectedCatalogs)
    }

    changeCatalog(catalogIds) {
        const { text: value } = this.props.criteria

        this.props.routeSearchFor(value, catalogIds)
    }

    render() {
        const { allCatalogs, criteria, searchResults, location } = this.props
        const { catalogs: selectedCatalogs = [] } = location.query
        let { page = 1 } = this.props.location.query
        if (page) { page = parseInt(page, 10) }

        return (
            <Search
                allCatalogs={allCatalogs}
                selectedCatalogs={selectedCatalogs}
                criteria={criteria}
                page={page}
                searchResults={searchResults}
                onSearch={this.onSearch.bind(this)}
                selectPage={this.selectPage}
                changeCatalog={this.changeCatalog.bind(this)} />
        )
    }
}

function mapStateToProps(state, props) {
    const { search, catalogs = [], page = 1 } = props.location.query

    return {
        allCatalogs: state.catalogs,
        criteria: state.criteria,
        searchResults: selectResults(search, catalogs, page)(state)
    }
}

export default connect(
  mapStateToProps,
  { changeCatalog, searchFor, routeSearchFor }
)(SmartSearch)
