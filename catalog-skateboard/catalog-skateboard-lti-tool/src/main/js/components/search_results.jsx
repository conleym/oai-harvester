import React from 'react'

export default class SearchResults extends React.Component {
    static displayName = 'SearchResults'

    renderResult(result, index) {
        return (
            <li key={result.uid}>
                {result.title}
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
