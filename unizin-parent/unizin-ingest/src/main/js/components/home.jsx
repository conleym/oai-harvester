import React from 'react'
import FileUpload from './file_upload.jsx'

const { func } = React.PropTypes

export default class Home extends React.Component {
    static displayName = 'Home'

    static propTypes = {
        onSelectFile: func.isRequired,
        files: React.PropTypes.object.isRequired,
    }

    renderFiles() {
        const { files } = this.props

        return Object.keys(files).map((key) => {
            const data = files[key]

            return (
                <FileUpload
                    key={key}
                    name={data.name}
                    size={data.size}
                    thumbnail={data.thumbnail}
                    progress={data.progress}
                    error={data.error} />
            )
        })

    }

    render() {

        return (
            <div>
                <button onClick={this.props.onSelectFile}>
                    Upload
                </button>

                {this.renderFiles()}
            </div>
        )
    }
}
