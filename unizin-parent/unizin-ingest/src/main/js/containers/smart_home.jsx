import React from 'react'
import Dropzone from '../components/dropzone'
import Home from '../components/home'
import ContributionForm from '../components/contribution_form'
import FinishUpload from '../components/finish_upload'
import smartLoader from './smart_loader'
import { getBatchId, submit, generateDocument } from '../actions/uploads'
import { reset } from '../actions/route'

class SmartHome extends React.Component {
    static displayName = 'SmartHome'

    static propTypes = {
        step: React.PropTypes.string.isRequired,
        batchId: React.PropTypes.string.isRequired,
        files: React.PropTypes.object.isRequired,
        reset: React.PropTypes.func.isRequired,
        submit: React.PropTypes.func.isRequired,
        generateDocument: React.PropTypes.func.isRequired,
    }

    onSelectFile() {
        this.refs.Dropzone.selectFile()
    }

    render() {
        const { step, batchId, files } = this.props

        const uploadURL = `/nuxeo/api/v1/upload/${batchId}/0`

        let content

        if (step === "upload") {
            content = (
                <Home
                    files={files}
                    onSelectFile={::this.onSelectFile} />
            )
        } else if (step === "form") {
            content = (
                <ContributionForm
                    files={files}
                    onSubmit={this.props.submit}
                    onCancel={this.props.reset}
                    />
            )
        } else if (step === 'finish_upload') {
            const { files } = this.props
            const key = Object.keys(files).pop()
            content = (
                <FinishUpload
                    done={this.props.generateDocument}
                    file={files[key]} />
            )
        }

        return (
            <Dropzone url={uploadURL} ref="Dropzone">
                {content}
            </Dropzone>
        )
    }
}

function mapStateToProps(state) {
    return {
        step: state.step,
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
    { reset, submit, generateDocument }
)(SmartHome)
