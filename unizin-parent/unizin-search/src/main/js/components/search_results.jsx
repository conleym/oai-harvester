import styles from './search_result.scss'
import React from 'react'
import { Link } from 'react-router'
import { routeInsert, routeResult, routePreviewUrl } from '../actions/route.js'
import Cover from './cover.jsx'
import Pager from './pager.jsx'
import Loading from './loading.jsx'
import classNames from 'classnames'
import FontAwesome from 'react-fontawesome'
import { checkValue } from '../actions/utils.js'

const { shape, string, func, number, array } = React.PropTypes

function normalizeAuthor(name) {
    const [ last, first ] = name.split(',')

    // If there wasn't a comma, return the original name
    if (first == null) { return name }

    return `${first} ${last}`
}

export const joinAuthors = (authors) => authors.map(normalizeAuthor).join(', ')

export default class SearchResults extends React.Component {
    static displayName = 'SearchResults'

    static propTypes = {
        criteria: shape({
            text: string
        }).isRequired,
        results: shape({
            entities: array,
        }),
        page: number.isRequired,
        selectPage: func.isRequired,
    }

    constructor(props, context) {
        super(props, context)

        this.renderResult = this.renderResult.bind(this)
    }

    renderResult(result, index) {
        const { title } = result
        const resultRoute = routeResult(result).route

        // insert button
        const insertLabel = "Insert " + title + " into your page"
        const insertBtnClasses = classNames("btn", styles.btn, "primary")

        // preview button
        const previewUrl = routePreviewUrl(result).url
        const previewBtnClasses = classNames("btn", styles.btn, styles.preview)
        const previewLabel = "Preview " + title

        const creators = checkValue(joinAuthors(result.properties['hrv:creator']))

        return (
            <li key={result.uid} className={styles.result}>

                <Cover className={styles.cover} document={result} />

                <Link to={resultRoute}>
                    {title}
                </Link>

                <ul className={styles.metadata}>
                    <li>Author: {creators}</li>
                </ul>

                <ul className={styles.controls}>
                  <li>
                    <Link to={routeInsert(result).route} className={insertBtnClasses}  aria-label={insertLabel} role="button">
                      <FontAwesome name='plus' aria-hidden='true' /> Insert
                    </Link>
                  </li>
                  <li>
                    <a href={previewUrl} target="_blank" className={previewBtnClasses} role="button" aria-label={previewLabel}>
                      <FontAwesome name='eye' aria-hidden='true' /> Preview
                    </a>
                  </li>
                </ul>
            </li>
        )
    }

    render() {
        const { criteria, results, selectPage } = this.props

        if (results.totalSize == null) {
            return <Loading message={`Loading results for '${criteria.text}'`} className={styles.loading} />
        }

        const { totalSize = 0, pageSize = 20 } = results

        const wrapperClasses = classNames(styles.wrapper, {
            [styles.singlePage]: (totalSize <= pageSize)
        })

        return (
          <div className={wrapperClasses}>
            <div className={styles.header}>
              <h1>{totalSize} results for {criteria.text}</h1>

              <Pager
                  current={this.props.page}
                  max={Math.ceil(totalSize / pageSize)}
                  onChange={selectPage}
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
