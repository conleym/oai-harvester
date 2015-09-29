import React from 'react'
import Cover from '../components/cover.jsx'
import { connect } from 'react-redux'
import { ensureDocument } from '../actions/documents.js'
import { routeInsert, routePreviewUrl } from '../actions/route.js'
import styles from './result.scss'
import { Link } from 'react-router'

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

        const previewUrl = routePreviewUrl(document).url

        return (
            <main className={styles.result} role="main">
              <navigation>
                <button onClick={this.props.history.goBack} aria-role="button">
                    &lt; Back to results
                </button>

                <ul className={styles.controls}>
                  <li>
                    <a href={previewUrl} target="_blank" className={styles.btn}>
                      o Preview
                    </a>
                  </li>
                  <li>
                    <Link to={routeInsert(document).route} className={styles.btn}>
                      + Insert
                    </Link>
                  </li>
                </ul>
              </navigation>

              <div className={styles.wrapper}>
                <div className={styles.content}>

                  <Cover document={document} className={styles.cover} />

                  <h1>{document.title}</h1>

                  {JSON.stringify(document, null, 2)}
                </div>

                <aside>
                  <h1>Additional information</h1>
                  <ul>
                    <li>Some info</li>
                    <li>More info</li>
                  </ul>
                </aside>
              </div>
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
