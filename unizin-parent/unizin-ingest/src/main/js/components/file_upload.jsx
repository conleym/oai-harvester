import React from 'react'
import fileSize from 'filesize'
import FontAwesome from 'react-fontawesome'
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

    progressStatus(progress, progressPercent) {
        if (progress === 100) {
            return (
                <div>
                    <FontAwesome name="check-circle" aria-hidden /> Complete
                </div>
            )
        } else {
            return (
                <div>
                    <FontAwesome name="times-circle" aria-hidden role="button" /> Uploading ({progressPercent} complete)
                </div>
              )
        }
    }

    render() {
        const { name, size, thumbnail, progress, error } = this.props
        const filesize = fileSize(size, {base: 0})
        const progressPercent = `${progress}%`

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
                        {this.progressStatus(progress, progressPercent)}
                    </div>
                    <div className={styles.barWrapper}>
                      <div className={styles.bar} style={{width: progressPercent}}></div>
                    </div>
                </div>
                <div className={styles.error}>
                    {error}
                </div>
            </div>

        )
    }
}
