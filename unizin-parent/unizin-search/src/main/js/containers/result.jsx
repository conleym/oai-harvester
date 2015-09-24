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
            <main className="result-details">
              <navigation>
                <button onClick={this.props.history.goBack}>
                    &lt; Back to results
                </button>
              </navigation>

              <article>
                <header>
                  <ul className='controls'>
                    <li>
                      <a href={returnUrl} className="btn">
                        o Preview
                      </a>
                    </li>
                    <li>
                      <a href={returnUrl} className="btn">
                        + Insert
                      </a>
                    </li>
                  </ul>

                  <Cover document={document} />

                  <h1>{document.title}</h1>
                </header>

                <section>
                  Metadata:
                  <pre>
                      {JSON.stringify(document, null, 2)}
                  </pre>
                </section>
              </article>
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
