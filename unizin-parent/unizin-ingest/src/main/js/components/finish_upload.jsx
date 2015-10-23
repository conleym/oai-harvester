import React from 'react'

export default class FinishUpload extends React.Component {
    static displayName = 'FinishUpload'

    static propTypes = {
        file: React.PropTypes.object.isRequired,
        done: React.PropTypes.func.isRequired,
    }

    componentDidMount() {
        console.log('progress', this.props.file.progress)
        if (this.props.file.progress == 100) {
            this.props.done()
        }
    }

    componentDidUpdate(prevProps, prevState) {
        console.log('progress', this.props.file.progress)
        if (this.props.file.progress == 100) {
            this.props.done()
        }
    }

    render() {
        const { file } = this.props
        const progress = file.progress.toFixed(2)

        return (
            <div>
                Upload {progress}% complete

                <p>
                    Your file is still uploading. This may take a few minutes
                    depending on the speed of your connection.
                </p>
            </div>
        )
    }
}
