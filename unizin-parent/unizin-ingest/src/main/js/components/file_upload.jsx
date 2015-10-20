import React from 'react'

const { number, string} = React.PropTypes

export default class FileUpload extends React.Component {
    static displayName = 'FileUpload'

    static propTypes = {
        name: string.isRequired,
        size: number.isRequired,
        thumbnail: string,
        // 0 - 100
        progress: number.isRequired,
        error: string,
    }

    render() {
        const { name, size, thumbnail, progress, error } = this.props

        return (
            <div className="dz-preview dz-file-preview">
                <div className="dz-details">
                    {name}
                    <br/>
                    Size: {size} (Units unknown)
                    <img src={thumbnail} />
                </div>
                <div className="dz-progress">
                    {progress}
                </div>
                <div className="dz-error-message">
                    {error}
                </div>
            </div>

        )
    }
}
