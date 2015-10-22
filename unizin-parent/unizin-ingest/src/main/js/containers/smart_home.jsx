import React from 'react'
import Dropzone from '../components/dropzone'
import Home from '../components/home'
import ContributionForm from '../components/contribution_form'
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

        let content

        if (Object.keys(files).length === 0) {
            content = (
                <Home
                    files={files}
                    onSelectFile={::this.onSelectFile} />
            )
        } else {
            const onSubmit = (data) => {
                console.log('TODO: do something with', data) // eslint-disable-line no-console
            }
            const onCancel = () => {
                console.log('TODO: Figure out what to do here') // eslint-disable-line no-console
            }

            content = (
                <ContributionForm
                    files={files}
                    onSubmit={onSubmit}
                    onCancel={onCancel}
                    />
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
