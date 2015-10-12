import React from 'react'
import { changeCatalog, searchFor } from '../actions/search.js'
import { routeSearchFor } from '../actions/route.js'
import { selectResults } from '../selectors.js'
import Search from '../components/search.jsx'
import { selectCatalogs } from '../selectors.js'
import { fetchCatalogs } from '../actions/search.js'
import smartLoader from './smart_loader.jsx'

// The props are passed in from the router and connect. This doesn't seem very
// useful for this file
/* eslint-disable react/prop-types */

class SmartSearch extends React.Component {
    static displayName = 'SmartSearch'

    constructor(props, context) {
        super(props, context)
        this.selectPage = this.selectPage.bind(this)
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
        allCatalogs: selectCatalogs(state),
        criteria: state.criteria,
        searchResults: selectResults(search, catalogs, page)(state)
    }
}

export default smartLoader(
    {
        inputFilter: (state, props) => {
            const params = {
                catalogCount: Object.keys(selectCatalogs(state)).length
            }
            const { search, catalogs = [], page = 1 } = props.location.query

            if (search != null) {
                params.search = search
                // Filter needs to
                params.catalogs = catalogs
                params.page = page
            }

            return params
        },
        isReady: (({catalogCount}, state) => catalogCount > 0),
        loader: (dispatch, params) => {
            if (params.catalogCount == 0) {
                dispatch( fetchCatalogs() )
            }

            if (params.search) {
                const { search, catalogs, page } = params
                dispatch(
                    searchFor(search, catalogs, page)
                )

            }
        }
    },
    mapStateToProps,
    { fetchCatalogs, changeCatalog, searchFor, routeSearchFor }
)(SmartSearch)
