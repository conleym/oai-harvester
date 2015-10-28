import React from 'react'
import styles from '../../css/finish_upload.scss'

export default class FinishUpload extends React.Component {
    static displayName = 'FinishUpload'

    static propTypes = {
        file: React.PropTypes.object.isRequired,
        done: React.PropTypes.func.isRequired,
    }

    componentDidMount() {
        if (this.props.file.progress == 100) {
            this.props.done()
        }
    }

    componentDidUpdate(prevProps, prevState) {
        if (this.props.file.progress == 100) {
            this.props.done()
        }
    }

    render() {
        const { file } = this.props
        const progress = file.progress.toFixed(2)

        return (
            <div className={styles.upload}>
                <h1>
                    <span className={styles.percent}>{progress}%</span> complete
                </h1>
                <p>
                    Your file is still uploading. This may take a few minutes
                    depending on the speed of your connection.
                </p>
            </div>
        )
    }
}
