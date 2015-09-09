import React from 'react'

export default class SearchResults extends React.Component {
    static displayName = 'SearchResults'

    renderResult(result, index) {
        const { properties } = result

        let thumb
        if (properties && properties['thumb:thumbnail']) {
            thumb = (
                <img src={properties['thumb:thumbnail'].data} />
            )
        }

        return (
            <li key={result.uid}>
                <h2>{result.title}</h2>
                {thumb}
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
                <h1>Results for {criteria.text}</h1>

                totalSize: {results.totalSize}

                <ul>
                    {results.entries.map(this.renderResult.bind(this))}
                </ul>
            </div>
        )
    }
}
