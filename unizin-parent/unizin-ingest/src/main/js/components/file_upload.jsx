import React from 'react'
import fileSize from 'filesize'
import styles from '../../css/file_upload.scss'

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
        const filesize = fileSize(size)
        const progressWidth = `${progress}%`
        const progressLabel = (progress === 100) ? "Complete" : "Uploading"

        return (
            <div className={styles.preview}>
                <h2 className="aural">File details</h2>
                <div className={styles.details}>
                    <div className={styles.thumbnail} aria-hidden="true">
                      <img src={thumbnail} alt={thumbnail} title="File thumbnail" />
                    </div>
                    <div>
                      {name}<span className={styles.size}>({filesize})</span>
                    </div>
                </div>
                <div className={styles.progressWrapper}>
                    <div className={styles.progressLabel}>
                      {progress} {progressLabel}
                    </div>
                    <div className={styles.barWrapper}>
                      <div className={styles.bar} style={{width: progressWidth}}></div>
                    </div>
                </div>
                <div className={styles.error}>
                    {error}
                </div>
            </div>

        )
    }
}
