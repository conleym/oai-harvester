import React from 'react'
import styles from './home.scss'
import FileUpload from './file_upload.jsx'
import FontAwesome from 'react-fontawesome'

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
            <div className={styles.wrapper} role="main">
                <div className={styles.main}>
                  <h1>Content contribution tool</h1>

                  <p>
                    Unizin's Content Contribution tool is used to add any kind of instructional material or content to
                    a shared repository at [].
                  </p>

                  <p>
                    To begin the contribution process, you can drag & drop a file into this window or use the
                    "Upload a File" button to the right.
                  </p>

                  <p>
                    Note: this tool is currently deployed as part of an Early Adopter Program at your institution.
                    If you are not part of this Early Adopter Program, please contact eap@unzin.org and report that
                    you are seeing this tool in error.
                  </p>

                </div>
                <aside>
                  <button onClick={this.props.onSelectFile}>
                      <FontAwesome name='arrow-circle-up' aria-hidden='true' /> Upload a file
                  </button>

                  <p>Drag and drop a file from your local computer anywhere on this page to upload.</p>

                  <p>
                    Alternatively you can click the Choose a file” button above to select a file from your computer.
                  </p>

                  {this.renderFiles()}
                </aside>
            </div>
        )
    }
}
