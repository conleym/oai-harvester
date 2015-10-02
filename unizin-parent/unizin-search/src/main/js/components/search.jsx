import React from 'react'
import styles from './search.scss'
import SearchResults from '../components/search_results.jsx'
import Footer from '../components/footer.jsx'
import CatalogSelector from '../components/catalog_selector.jsx'
import classNames from 'classnames'

const { any, number, func, string, arrayOf, shape } = React.PropTypes

export default class Search extends React.Component {
    static displayName = 'Search'

    static propTypes = {
        onSearch: func.isRequired,
        criteria: shape({
            text: string
        }).isRequired,
        allCatalogs: any.isRequired,
        selectedCatalogs: arrayOf(string).isRequired,
        page: number.isRequired,
        searchResults: any,
        changeCatalog: func.isRequired,
        selectPage: func.isRequired,
    }

    render() {

        const {
            criteria, allCatalogs, selectedCatalogs, page, searchResults,
            changeCatalog, selectPage
        } = this.props


        // const { catalogs, criteria, searchResults, location } = this.props
        // const { catalogs: selectedCatalogs } = location.query
        const searchBtnClasses = classNames("btn", "primary", styles.btn)

        const onSearch = (event) => {
            event.preventDefault()
            const { value } = React.findDOMNode(this.refs.searchInput)
            this.props.onSearch(value)
        }

        const mainClasses = classNames(styles.container, {
            [styles.empty]: (criteria.text == null)
        })
        const footerClasses = classNames(null, {
            [styles.footer]: (criteria.text == null)
        })
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
                <form className={styles.search} onSubmit={onSearch} role="search"
                      aria-label="Search for catalog items">

                    <input
                        defaultValue={criteria.text}
                        type="text"
                        id="searchInput"
                        ref="searchInput"
                        placeholder="Enter search criteria" />
                    <label htmlFor="searchInput" className="aural">Enter search criteria</label>
                    <input type="submit" value="Search" className={searchBtnClasses} />
                </form>
              </div>
              <div className={styles['results-container']}>
                { criteria.text != null ? (
                    <CatalogSelector
                        onChange={changeCatalog}
                        selected={selectedCatalogs}
                        catalogs={allCatalogs} />
                ) : null}
                { criteria.text != null ? (
                    <SearchResults
                        selectPage={selectPage}
                        page={page}
                        criteria={criteria}
                        results={searchResults} />
                ) : null}
              </div>
              <Footer className={footerClasses} />
            </main>
        )
    }
}
