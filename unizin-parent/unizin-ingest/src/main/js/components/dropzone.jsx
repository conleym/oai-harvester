import React from 'react'
import DropzoneJS from 'dropzone'
import { connect } from 'react-redux'
import { EventEmitter } from 'events'
import styles from './dropzone.scss'
import { uploadFile, uploadProgress, uploadThumbnail, uploadError } from '../actions/uploads'

DropzoneJS.autoDiscover = false
/* eslint-disable react/no-multi-comp */

const { node, func } = React.PropTypes

class Dropzone extends React.Component {
    static displayName = 'Dropzone'

    static propTypes = {
        children: node,
        url: React.PropTypes.oneOfType([
            React.PropTypes.string,
            React.PropTypes.func
        ]).isRequired
    }

    constructor(props, context) {
        super(props, context)
        // Every time the user selects a file we need to clear the input.
        // The best way to do that is to give it a key that changes on each
        // file selection.
        this.state = { inputKey: 0 }
    }

    componentWillMount() {
        const fileKeys = new Map()
        let nextKey = 0

        this.dz = new DropzoneJS(document.body, {
            addedfile: (file) => {
                const key = `file${nextKey++}`
                fileKeys.set(file, key)
                this.props.uploadFile(key, file)
            },
            thumbnail: (file, dataUrl) => {
                const key = fileKeys.get(file)
                this.props.uploadThumbnail(key, dataUrl)
            },
            uploadprogress: (file, progress, bytesSent) => {
                const key = fileKeys.get(file)
                this.props.uploadProgress(key, progress, bytesSent)
            },
            error: (file, message) => {
                const key = fileKeys.get(file)
                this.props.uploadError(key, message)
            },
            url: ::this.getUrl,
            uploadMultiple: false,
            parallelUploads: 1,
            clickable: false
        })
    }

    getUrl() {
        const { url } = this.props
        if (typeof url === 'function') {
            return url(...arguments)
        }
        return url
    }

    /*
    This is a public function to be called by whatever renders Dropzone.
    It's not ideal, but I just don't see a better way to do this.

    Usage:
    render (<Dropzone ref="Dropzone">)
    to activate: this.refs.Dropzone.selectFile
    */
    selectFile() {
        const input = React.findDOMNode(this.refs.hiddenInput)
        input.click()
    }

    onChangeInput(e) {
        const input = React.findDOMNode(this.refs.hiddenInput)
        const files = input.files

        for (let i = 0; i < files.length; i++) {
            this.dz.addFile( files[i] )
        }

        this.dz.emit('addedFiles', files)

        this.setState({
            inputKey: this.state.inputKey + 1
        })
    }

    render() {
        return (
            <span>
                <input
                    key={this.state.inputKey}
                    onChange={::this.onChangeInput}
                    ref="hiddenInput"
                    style={{
                        visibility: 'hidden',
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        height: 0,
                        width: 0,
                    }}
                    type="file" />
                <div className={styles.hover} >
                    Drop to contribute
                    <br/>
                    You will be presented with a form
                </div>

                {this.props.children}
            </span>
        )
    }
}


const connectedDZ = connect(function() { return {} }, {
    uploadFile, uploadProgress, uploadError, uploadThumbnail
})(Dropzone)

connectedDZ.prototype.selectFile = function() {
    this.refs.wrappedInstance.selectFile()
}

export default connectedDZ