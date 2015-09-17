import React from 'react'
import Cover from '../components/cover.jsx'
import { connect } from 'react-redux'
import { ensureDocument } from '../actions/documents.js'
import { routeReturnUrl } from '../actions/route.js'

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
            <div>
                <button onClick={this.props.history.goBack}>
                    &lt; Back to results
                </button>

                <a href={returnUrl}>
                    + Insert
                </a>
                <hr/>

                <Cover document={document} />

                <h1>{document.title}</h1>

                Metadata:
                <pre>
                    {JSON.stringify(document, null, 2)}
                </pre>
            </div>
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
