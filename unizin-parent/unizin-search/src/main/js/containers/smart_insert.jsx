import React from 'react'
import smartLoader from './smart_loader.jsx'
import { ensureDocument, documentImport } from '../actions/documents.js'
import { routeReturnUrl} from '../actions/route.js'
import { isDocumentReady, selectDocumentLoadError } from '../selectors.js'
import Insert from '../components/insert.jsx'

const { func, func: dispatchFunc, shape, string, object, bool } = React.PropTypes

class SmartInsert extends React.Component {
    static displayName = 'SmartInsert'

    static propTypes = {
        uid: string.isRequired,
        history: shape({
            goBack: func.isRequired
        }).isRequired,
        documentImport: dispatchFunc.isRequired,
        routeReturnUrl: dispatchFunc.isRequired,
        params: shape({
            uid: string.isRequired
        }).isRequired,
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
            window.location = this.props.routeReturnUrl(document).url
        }
    }

    foo = () => {
        this.something
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
            <Insert
                document={document}
                loadError={loadError}
                onCancel={this.onCancel}
                onTryAgain={this.onTryAgain} />
        )
    }
}

function mapStateToProps(state, props) {
    const { uid } = props.params

    return {
        uid,
        document: state.documents[uid],
        ready: isDocumentReady(uid)(state),
        loadError: selectDocumentLoadError(uid)(state),
    }
}

export default smartLoader(
    {
        inputFilter(state, props) {
            return { uid: props.params.uid }
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
    { documentImport, routeReturnUrl }
)(SmartInsert)
