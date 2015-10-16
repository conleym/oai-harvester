import React from 'react'
import FontAwesome from 'react-fontawesome'
import styles from './insert.scss'
import classNames from 'classnames'

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
                    <li><button onClick={onCancel}>Cancel insert</button></li>
                    <li><button onClick={onTryAgain} className='primary'>Try again</button></li>
                </ul>
            )
        } else {
            return <button onClick={onCancel}>Cancel insert</button>
        }
    }

    render() {
        const { document, loadError } = this.props

        return (
            <div className={styles.insert} role="main">
                <div className={styles.messaging}>
                    <h1>
                      <FontAwesome name='refresh' spin={loadError == null} aria-hidden='true' className={styles.fa} />
                      Copying Content
                    </h1>

                    <p>
                      Unizin is currently locating <strong>{document.title}</strong> from its originating source. Once we've
                      located the content, we'll create a copy that is always available for you and your students.
                    </p>

                    <p>
                      This process can take several minutes because of one or more of the following reasons:
                    </p>

                    <ol>
                      <li>We are looking at multiple locations for the source file.</li>
                      <li>We are negotiating access to the file.</li>
                      <li>The file is large.</li>
                    </ol>

                    <div className={styles.controls}>
                      {this.renderControls(loadError)}
                    </div>

                </div>
                {this.renderError(loadError)}
            </div>
        )
    }
}
