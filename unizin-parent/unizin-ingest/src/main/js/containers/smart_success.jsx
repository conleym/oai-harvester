import React from 'react'
import smartLoader from './smart_loader'
import { loadDocument } from '../actions/documents'
import Success from '../components/success'

class SmartSuccess extends React.Component {
    static displayName = 'SmartSuccess'

    static propTypes = {
        document: React.PropTypes.object.isRequired,
    }

    render() {
        return (
            <Success document={this.props.document} />
        )
    }
}

function mapStateToProps(state, props) {
    const { uid } = props.params
    return {
        document: state.documents[uid]
    }
}

export default smartLoader(
    {
        inputFilter(state, props) {
            const { uid } = props.params
            return { uid }
        },
        isReady: ({uid}, state) => (state.documents[uid] != null),
        loader(dispatch, params, lastParams) {
            if (params.uid != lastParams.uid) {
                dispatch(loadDocument(params.uid))
            }
        }
    },
    mapStateToProps,
    {}
)(SmartSuccess)
