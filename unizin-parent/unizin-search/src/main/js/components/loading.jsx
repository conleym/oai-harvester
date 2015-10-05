import React from 'react'
import styles from './loading.scss'
import classNames from 'classnames'

export default class Loader extends React.Component {
    static displayName = 'Loader'

    static propTypes = {
        message: React.PropTypes.string
    }

    static defaultProps = {
        message: 'Loading...'
    }

    render() {
        const logoURL = require('file!../../resources/skin/resources/brand.svg')

        const loadingClasses = classNames(styles.loading, this.props.className)

        return (
            <div className={loadingClasses}>
              <img src={logoURL} className={styles.logo} alt="Unizin Catalog Search Tool Logo" title="Logo" />
              <h1>{this.props.message}</h1>
            </div>
        )
    }
}