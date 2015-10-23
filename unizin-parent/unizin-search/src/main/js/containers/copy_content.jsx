import React from 'react'
import smartLoader from './smart_loader.jsx'
import { ensureDocument, documentImport } from '../actions/documents.js'
import { isDocumentReady, selectDocumentLoadError } from '../selectors.js'
import CopyContent from '../components/copy_content.jsx'

const { func, func: dispatchFunc, shape, string, object, bool } = React.PropTypes

class SmartCopyContent extends React.Component {
    static displayName = 'SmartCopyContent'

    static propTypes = {
        uid: string.isRequired,
        done: React.PropTypes.func.isRequired,
        history: shape({
            goBack: func.isRequired
        }).isRequired,
        documentImport: dispatchFunc.isRequired,
        document: object.isRequired,
        ready: bool,
        loadError: string,
    }

    constructor(props, context) {
        super(props, context)
        this.onCancel = this.onCancel.bind(this)
        this.onTryAgain = this.onTryAgain.bind(this)
    }

    componentDidMount() {
        this.maybeRedirect()
    }

    componentDidUpdate(prevProps, prevState) {
        this.maybeRedirect()
    }

    maybeRedirect() {
        const { document, ready } = this.props

        if (ready) {
            this.props.done(document)
        }
    }

    onCancel() {
        this.props.history.goBack()
    }

    onTryAgain() {
        this.props.documentImport(this.props.uid)
    }

    render() {
        const { document, loadError } = this.props

        return (
            <CopyContent
                document={document}
                loadError={loadError}
                onCancel={this.onCancel}
                onTryAgain={this.onTryAgain} />
        )
    }
}

function mapStateToProps(state, props) {
    const { uid } = props

    return {
        document: state.documents[uid],
        ready: isDocumentReady(uid)(state),
        loadError: selectDocumentLoadError(uid)(state),
    }
}

export default smartLoader(
    {
        inputFilter(state, {uid}) {
            return { uid }
        },
        isReady: ({uid}, state) => (state.documents[uid] != null),
        loader(dispatch, params, lastParams) {
            if (params.uid != lastParams.uid) {
                dispatch(ensureDocument(params.uid))
                dispatch(documentImport(params.uid))
            }
        }
    },
    mapStateToProps,
    { documentImport }
)(SmartCopyContent)
