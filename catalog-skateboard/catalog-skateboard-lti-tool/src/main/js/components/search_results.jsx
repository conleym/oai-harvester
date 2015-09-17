import React from 'react'
import { Link } from 'react-router'
import { routeResult, routeReturnUrl } from '../actions/route.js'

const noCover = (
    <div style={{
        border: '1px black solid',
        width: '100px',
    }}>
        No
        <br/>
        Cover
        <br/>
        Available
        <br/>
    </div>
)

export default class SearchResults extends React.Component {
    static displayName = 'SearchResults'

    renderResult(result, index) {
        const { title, properties } = result
        const returnUrl = routeReturnUrl(result).url
        const resultRoute = routeResult(result).route

        let thumb = noCover
        if (properties && properties['thumb:thumbnail']) {
            thumb = (
                <img src={properties['thumb:thumbnail'].data} />
            )
        }

        return (
            <li key={result.uid}>
                {thumb}
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
