import React from 'react'
import Cover from '../components/cover.jsx'
import { connect } from 'react-redux'
import { ensureDocument } from '../actions/documents.js'
import { routeReturnUrl } from '../actions/route.js'
import styles from './result.scss'

class Result extends React.Component {
    static displayName = 'Result'

    componentDidMount() {
        this.props.ensureDocument(this.props.params.uid)
    }

    render() {
        const { document } = this.props
        if (!document) {
            return null
        }

        const returnUrl = routeReturnUrl(document).url

        return (
            <main className={styles.result} role="main">
              <navigation>
                <button onClick={this.props.history.goBack} aria-role="button">
                    &lt; Back to results
                </button>
              </navigation>

              <div className={styles.header}>
                <ul className={styles.controls}>
                  <li>
                    <a href={returnUrl} className={styles.btn}>
                      o Preview
                    </a>
                  </li>
                  <li>
                    <a href={returnUrl} className={styles.btn}>
                      + Insert
                    </a>
                  </li>
                </ul>

                <Cover document={document} className={styles.cover} />

                <h1>{document.title}</h1>
              </div>

              <section>
                Metadata:
                <pre>
                    {JSON.stringify(document, null, 2)}
                </pre>
              </section>
            </main>
        )
    }
}

function mapStateToProps(state, props) {
    const { uid } = props.params

    return {
        document: state.documents[uid]
    }
}

export default connect(
  mapStateToProps,
  { ensureDocument }
)(Result)
