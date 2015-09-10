import React from 'react'
import { encodeURL } from '../actions.js'

function resultUrl(url, title) {
    const query = encodeURL`?return_type=url&url=${url}&title=${title}&target=_blank`
    return window.lti_data.ext_content_return_url + query
}

export default class SearchResults extends React.Component {
    static displayName = 'SearchResults'

    renderResult(result, index) {
        const { path, title, properties } = result

        const target = location.protocol + '//' + location.hostname + path

        const url = resultUrl(target, title)

        let thumb
        if (properties && properties['thumb:thumbnail']) {
            thumb = (
                <img src={properties['thumb:thumbnail'].data} />
            )
        }

        return (
            <li key={result.uid}>
                <h2>
                    <a href={url}>{title}</a>
                </h2>
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
