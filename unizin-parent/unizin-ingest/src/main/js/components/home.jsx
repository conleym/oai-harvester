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
        const brandURL = require('file!../../resources/skin/resources/brand.svg')
        const logoURL = require('file!../../resources/skin/resources/logo.svg')

        return (
            <div className={styles.wrapper} role="main">
                <div className={styles.main}>
                  <div className={styles.brand}>
                    <h1>
                      <img src={brandURL} alt="Unizin Content Contribution Tool Logo" title="Logo" />
                      Content contribution tool
                    </h1>
                    <div className={styles.powered}>
                      powered by <img src={logoURL} alt="Unizin Logo" title="Logo" />
                    </div>
                  </div>

                  <div className={styles.upload}>
                      <h2>
                        <FontAwesome name="cloud-upload" aria-hidden /> Drag & Drop
                      </h2>

                      <p>
                        Drag and drop a file from your local computer anywhere on this page to upload or
                        <button onClick={this.props.onSelectFile} className="simple"> select a file</button> from your
                        file system.
                      </p>

                      {this.renderFiles()}
                  </div>

                  <button className="primary">
                    <FontAwesome name="angle-up" aria-hidden /> What is the Early Adopter Program?
                  </button>
                  <div className={styles.description}>
                    <p>
                      Unizin's Content Contribution tool is used to add any kind of instructional material or content to
                      a shared repository.
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

                </div>
            </div>
        )
    }
}
