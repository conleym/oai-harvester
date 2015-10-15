import React from 'react'
import FontAwesome from 'react-fontawesome'
import styles from './insert.scss'

const { shape, string, func } = React.PropTypes

export default class Insert extends React.Component {
    static displayName = 'Insert'

    static propTypes = {
        document: shape({
            title: string,
            loadError: string,
        }),
        loadError: string,
        onCancel: func.isRequired,
        onTryAgain: func.isRequired,
    }

    renderError(loadError) {

        if (loadError) {
            return (
                <h2 className={styles.loadError} role='alert'>{loadError}</h2>
            )
        }

        return null
    }

    renderControls(loadError) {
        const { onCancel, onTryAgain } = this.props
        if (loadError) {
            return (
                <ul>
                    <li><button onClick={onCancel}>Cancel</button></li>
                    <li><button onClick={onTryAgain} className='primary'>Try again</button></li>
                </ul>
            )
        } else {
            return <button onClick={onCancel}>Cancel</button>
        }
    }

    render() {
        const { document, loadError } = this.props
        return (
            <div className={styles.insert}>
                <FontAwesome name='refresh' spin={loadError == null} aria-hidden='true' className={styles.fa} />
                <h1 aria-live='polite'>Preparing '{document.title}'</h1>

                {this.renderError(loadError)}

                <div className={styles.controls}>
                  {this.renderControls(loadError)}
                </div>
            </div>
        )
    }
}
