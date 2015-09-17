import React from 'react'
import { Link } from 'react-router'
import { routeResult, routeReturnUrl } from '../actions/route.js'
import Cover from './cover.jsx'

export default class SearchResults extends React.Component {
    static displayName = 'SearchResults'

    renderResult(result, index) {
        const { title } = result
        const returnUrl = routeReturnUrl(result).url
        const resultRoute = routeResult(result).route


        return (
            <li key={result.uid}>
                <Cover document={result} />
                <h2>
                    <Link to={resultRoute}>
                        {title}
                    </Link>
                </h2>

                <a href={returnUrl}>
                    + Insert
                </a>
            </li>
        )
    }

    render() {
        const { criteria, results } = this.props

        if (!criteria.text) {
            return null
        }

        return (
            <div>
                {results.totalSize} Results

                <ul>
                    {results.entries.map(this.renderResult.bind(this))}
                </ul>
            </div>
        )
    }
}
