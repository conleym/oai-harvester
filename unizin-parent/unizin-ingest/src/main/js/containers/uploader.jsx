import React from 'react'
import setupDropzone from '../components/dropzone'
import Home from '../components/home'
import smartLoader from './smart_loader'
import { getBatchId } from '../actions/uploads'

class SmartHome extends React.Component {
    static displayName = 'SmartHome'

    static propTypes = {
        batchId: React.PropTypes.string.isRequired
    }

    componentWillMount() {
        const { Dropzone, Files, clickFileInput } = setupDropzone()
        this.Dropzone = Dropzone
        this.Files = Files
        this.clickFileInput = clickFileInput
    }

    render() {
        const { batchId } = this.props

        const uploadURL = `/nuxeo/api/v1/upload/${batchId}/0`

        return (
            <this.Dropzone url={uploadURL}>
                {this.props.children}
            </this.Dropzone>
        )
    }
}

function mapStateToProps(state) {
    return {
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
