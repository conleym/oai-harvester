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
                files.forEach((value, key) => ret.push(transformer(value, key)))
                return ret
            }
        }
    }())

    fileStore.on(() => {
        fileStore.forEach(console.log.bind(console, 'file'))
    })
    window.fileStore = fileStore

    class Dropzone extends React.Component {
        static displayName = 'Dropzone'

        static propTypes = {
            children: node
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
                url: '/foo',
                parallelUploads: 1,
                clickable: false
            })


            // All of this was copied directly from dropzonejs.com
            /* eslint-disable no-var, semi */
            const minSteps = 6,
                maxSteps = 60,
                timeBetweenSteps = 100,
                bytesPerStep = 100000;

            dz.uploadFiles = function(files) {
                var self = this;

                for (var i = 0; i < files.length; i++) {

                    var file = files[i];
                    const totalSteps = Math.round(Math.min(maxSteps, Math.max(minSteps, file.size / bytesPerStep)));

                    for (var step = 0; step < totalSteps; step++) {
                        var duration = timeBetweenSteps * (step + 1);
                        setTimeout(function(file, totalSteps, step) {
                            return function() {
                                file.upload = {
                                    progress: 100 * (step + 1) / totalSteps,
                                    total: file.size,
                                    bytesSent: (step + 1) * file.size / totalSteps
                                };

                                self.emit('uploadprogress', file, file.upload.progress, file.upload.bytesSent);
                                if (file.upload.progress == 100) {
                                    file.status = Dropzone.SUCCESS;
                                    self.emit("success", file, 'success', null);
                                    self.emit("complete", file);
                                    self.processQueue();
                                }
                            };
                        }(file, totalSteps, step), duration);
                    }
                }
            }
            /* eslint-enable no-var, semi */

            global.dz = dz
        }

        componentDidMount() {
            hiddenInput = this.refs.hiddenInput
        }

        componentDidUpdate(prevProps, prevState) {
            hiddenInput = this.refs.hiddenInput
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
                    {fileStore.map((data, file) => (
                        <Template
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
