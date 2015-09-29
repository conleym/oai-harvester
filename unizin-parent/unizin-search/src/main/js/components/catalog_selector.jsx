import styles from './catalog_selector.scss'
import React from 'react'

export default class CatalogSelector extends React.Component {
    static displayName = 'CatalogSelector'

    onChange(k, event) {
        const enabled = event.target.checked
        const { catalogs } = this.props
        let { selected = [] } = this.props

        // Make sure we only output valid values
        selected = selected.filter(v => catalogs[v] != null)

        if (enabled) {
            selected.push(k)
        } else {
            selected = selected.filter(v => v != k)
        }

        this.props.onChange(selected)
    }
    renderCatalogs() {
        const { catalogs, selected = [] } = this.props

        return Object.keys(catalogs).map((key) => {
            const { label } = catalogs[key]
            const enabled = selected.indexOf(key) >= 0

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
            <aside className={styles.filters}>
                Show results from these catalogs
                <ul>
                    {this.renderCatalogs()}
                </ul>
            </aside>
        )
    }
}
