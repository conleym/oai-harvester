import React from 'react'
import { connect } from 'react-redux'
import { waitForDocument } from '../actions/documents.js'
import Insert from '../components/insert.jsx'

class SmartInsert extends React.Component {
    static displayName = 'SmartInsert'

    componentWillMount() {
        this.props.waitForDocument(this.props.params.uid)
    }

    render() {
        const { document } = this.props
        if (document && document.title) {
            return (
                <Insert document={document} />
            )
        }

        return null
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
  { waitForDocument }
)(SmartInsert)
