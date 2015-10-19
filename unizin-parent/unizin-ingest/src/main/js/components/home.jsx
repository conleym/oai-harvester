import React from 'react'
import FileUpload from './file_upload.jsx'

const { func } = React.PropTypes

export default class Home extends React.Component {
    static displayName = 'Home'

    static propTypes = {
        onSelectFile: func.isRequired,
        Files: func.isRequired,
    }

    render() {
        const { Files } = this.props

        return (
            <div>
                <button onClick={this.props.onSelectFile}>
                    Upload
                </button>

                <Files Template={FileUpload} />
            </div>
        )
    }
}
