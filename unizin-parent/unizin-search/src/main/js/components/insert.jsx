import React from 'react'
import FontAwesome from 'react-fontawesome'
import styles from './insert.scss'

const { shape, string } = React.PropTypes

export default class Insert extends React.Component {
    static displayName = 'Insert'

    static propTypes = {
        document: shape({
            title: string,
            loadError: string,
        })
    }

    renderError(document) {

        if (document.loadError) {
            return (
                <h2 className={styles.error} role='alert'>{document.loadError}</h2>
            )
        }

        return null
    }

    render() {
        const { document } = this.props
        return (
            <div className={styles.insert}>
                <FontAwesome name='refresh' spin aria-hidden='true' className={styles.fa} />
                <h1 aria-live='polite'>Preparing '{document.title}'</h1>

                {this.renderError(document)}
            </div>
        )
    }
}
