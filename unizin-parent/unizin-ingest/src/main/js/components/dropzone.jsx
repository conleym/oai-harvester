import React from 'react'
import DropzoneJS from 'dropzone'
import { EventEmitter } from 'events'
import styles from './dropzone.scss'

DropzoneJS.autoDiscover = false
/* eslint-disable react/no-multi-comp */

const { node, func } = React.PropTypes

export default function generateDropzone()  {
    let dz
    let hiddenInput
    const fileStore = (function() {
        const files = new Map()
        const ee = new EventEmitter()


        return {
            get: (::files.get),
            forEach: (::files.forEach),
            on(callback) {
                ee.addListener('CHANGE', callback)
            },
            off(callback) {
                ee.removeEventListener('CHANGE', callback)
            },
            set(file, data) {
                files.set(file, data)
                ee.emit('CHANGE')
            },
            map(transformer) {
                const ret = []
                let index = 0
                files.forEach((value, key) => ret.push(transformer(value, key, index++)))
                return ret
            }
        }
    }())

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
            if (dz != null) {
                throw new Error('dz already defined')
            }
            dz = new DropzoneJS(document.body, {
                addedfile(file) {
                    fileStore.set(file, {
                        thumbnail: null,
                        progress: 0,
                    })
                },
                thumbnail: function(file, dataUrl) {
                    const meta = fileStore.get(file)
                    fileStore.set(file, {
                        ...meta,
                        thumbnail: dataUrl
                    })
                    // Display the image in your file.previewElement
                },
                uploadprogress: function(file, progress, bytesSent) {
                    const meta = fileStore.get(file)
                    fileStore.set(file, {
                        ...meta,
                        progress,
                        bytesSent,
                    })
                },
                error(file, message) {
                    const meta = fileStore.get(file)
                    fileStore.set(file, {
                        ...meta,
                        error: message
                    })
                },
                url: ::this.getUrl,
                uploadMultiple: false,
                parallelUploads: 1,
                clickable: false
            })

            global.dz = dz
        }

        componentDidMount() {
            hiddenInput = this.refs.hiddenInput
        }

        componentDidUpdate(prevProps, prevState) {
            hiddenInput = this.refs.hiddenInput
        }

        getUrl() {
            const { url } = this.props
            if (typeof url === 'function') {
                return url(...arguments)
            }
            return url
        }


        onChangeInput(e) {
            const input = React.findDOMNode(this.refs.hiddenInput)
            const files = input.files

            for (let i = 0; i < files.length; i++) {
                dz.addFile( files[i] )
            }

            dz.emit('addedFiles', files)

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

    class Files extends React.Component {
        static displayName = 'Files'

        static propTypes = {
            Template: func.isRequired
        }

        constructor(props, context) {
            super(props, context)

            this.refresh = ::this.forceUpdate
        }

        componentDidMount() {
            fileStore.on(this.refresh)
        }

        componentWillUnmount() {
            fileStore.off(this.refresh)
        }

        render() {
            const { Template } = this.props

            return (
                <div>
                    {fileStore.map((data, file, index) => (
                        <Template
                            key={index}
                            filename={file.name}
                            progress={data.progress}
                            bytesSent={data.bytesSent}
                            size={file.size}
                            thumbnail={data.thumbnail}
                            error={data.error}
                            />
                    ))}
                </div>
            )
        }
    }

    return {
        Dropzone,
        Files,
        clickFileInput() {
            React.findDOMNode(hiddenInput).click()
        }
    }
}
