import React from 'react'
import { Link } from 'react-router'
import FileUpload from './file_upload.jsx'

const { object, func } = React.PropTypes

export default class Home extends React.Component {
    static displayName = 'Home'

    static propTypes = {
        query: object.isRequired,
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
                Hello World
                <pre>{JSON.stringify(this.props.query, null, 2)}</pre>

                <Link to="/">Foo</Link>
                <br/>
                <Link to="/">Bar</Link>

                <Files Template={FileUpload} />
            </div>
        )
    }
}
