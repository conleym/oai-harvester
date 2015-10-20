import React from 'react'
import Dropzone from '../components/dropzone'
import Home from '../components/home'
import smartLoader from './smart_loader'
import { getBatchId } from '../actions/uploads'

class SmartHome extends React.Component {
    static displayName = 'SmartHome'

    static propTypes = {
        batchId: React.PropTypes.string.isRequired,
        files: React.PropTypes.object.isRequired,
    }

    onSelectFile() {
        this.refs.Dropzone.selectFile()
    }

    render() {
        const { batchId, files } = this.props

        const uploadURL = `/nuxeo/api/v1/upload/${batchId}/0`

        return (
            <Dropzone url={uploadURL} ref="Dropzone">
                <Home
                    files={files}
                    onSelectFile={::this.onSelectFile} />
            </Dropzone>
        )
    }
}

function mapStateToProps(state) {
    return {
        files: state.files,
        batchId: state.batchId
    }
}

export default smartLoader(
    {
        inputFilter(state, props) {
            return { batchId: state.batchId }
        },
        isReady: ({batchId}, state) => (batchId != null),
        loader(dispatch, params, lastParams) {
            if (params.batchId == null) {
                dispatch(getBatchId())
            }
        }
    },
    mapStateToProps,
    { }
)(SmartHome)
