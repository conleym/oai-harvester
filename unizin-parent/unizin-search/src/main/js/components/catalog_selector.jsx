import styles from './catalog_selector.scss'
import React from 'react'

export default class CatalogSelector extends React.Component {
    static displayName = 'CatalogSelector'

    onChange(key, e) {
        this.props.onChange(key, e.target.checked)
    }
    renderCatalogs() {
        const { catalogs } = this.props

        return Object.keys(catalogs).map((key) => {
            const { label, enabled } = catalogs[key]

            return (
                <li key={key}>
                    <input
                        id={key}
                        onChange={this.onChange.bind(this, key)}
                        type="checkbox"
                        checked={enabled} />
                    <label htmlFor={key}>{label}</label>
                </li>
            )
        })
    }

    render() {

        return (
            <aside className={styles.filters} aria-label="Filter search results">
                <h1>Filter search results</h1>
                <form role="form">
                  <fieldset>
                    <legend>
                      <span className="aural">Show results from these Catalogs</span>
                      <span aria-hidden="true">Content Catalogs</span>
                    </legend>

                    <ul>
                        {this.renderCatalogs()}
                    </ul>
                  </fieldset>
                </form>
            </aside>
        )
    }
}
