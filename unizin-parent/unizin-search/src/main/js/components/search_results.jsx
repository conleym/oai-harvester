import styles from './search_result.scss'
import React from 'react'
import { Link } from 'react-router'
import { routeInsert, routeResult, routePreviewUrl } from '../actions/route.js'
import Cover from './cover.jsx'
import Pager from './pager.jsx'
import Loading from './loading.jsx'
import classNames from 'classnames'

const { shape, string, func, number, array } = React.PropTypes

export default class SearchResults extends React.Component {
    static displayName = 'SearchResults'

    static propTypes = {
        criteria: shape({
            text: string
        }).isRequired,
        searchFor: func,
        results: shape({
            entities: array,
        }),
        page: number.isRequired,
    }

    constructor(props, context) {
        super(props, context)

        this.onPage = this.onPage.bind(this)
        this.renderResult = this.renderResult.bind(this)
    }

    onPage(page) {
        const { criteria, searchFor } = this.props
        searchFor(criteria.text, page)
    }

    renderResult(result, index) {
        const { title } = result
        const resultRoute = routeResult(result).route

        // insert button
        const insertLabel = "Insert " + title + " into your page"
        const insertBtnClasses = classNames("btn", styles.btn)

        // preview button
        const previewUrl = routePreviewUrl(result).url
        const previewBtnClasses = classNames("btn", styles.btn, styles.preview)
        const previewLabel = "Preview " + title

        return (
            <li key={result.uid} className={styles.result}>

                <Cover className={styles.cover} document={result} />

                <Link to={resultRoute}>
                    {title}
                </Link>

                <ul className={styles.metadata}>
                  <li>Type: {result.type}</li>
                  <li>Entity: {result['entity-type']}</li>
                </ul>

                <ul className={styles.controls}>
                  <li>
                    <Link to={routeInsert(result).route} className={insertBtnClasses}  aria-label={insertLabel} role="button">
                      + Insert
                    </Link>
                  </li>
                  <li>
                    <a href={previewUrl} target="_blank" className={previewBtnClasses} role="button" aria-label={previewLabel}>
                      o Preview
                    </a>
                  </li>
                </ul>
            </li>
        )
    }

    render() {
        const { criteria, results } = this.props

        if (results.totalSize == null) {
            return <Loading message={`Loading Results for '${criteria.text}'`} className={styles.loading} />
        }

        const { totalSize = 0, pageSize = 20 } = results

        const wrapperClasses = classNames(styles.wrapper, {
            [styles.singlePage]: (totalSize <= pageSize)
        })

        return (
          <div className={wrapperClasses}>
            <div className={styles.header}>
              <h1>{totalSize} Results for {criteria.text}</h1>

              <Pager
                  current={this.props.page}
                  max={Math.ceil(totalSize / pageSize)}
                  onChange={this.onPage}
                  ariaLabel="Results pagination top" />
            </div>

            <div className={styles.results}>
              <ul>
                  {results.entries.map(this.renderResult)}
              </ul>
            </div>
          </div>
        )
    }
}
