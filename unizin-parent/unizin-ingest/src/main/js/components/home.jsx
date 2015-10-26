import React from 'react'
import styles from './home.scss'
import FileUpload from './file_upload.jsx'
import FontAwesome from 'react-fontawesome'
import SlideUp from './slide_up.jsx'

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

        const onSlide = () => {return false}

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
                      <div className={styles.icons}>
                        <FontAwesome name="file-photo-o" aria-hidden />
                        <FontAwesome name="file-movie-o" aria-hidden />
                        <FontAwesome name="file-audio-o" aria-hidden />
                        <FontAwesome name="file-text-o" aria-hidden />
                      </div>

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

                  <SlideUp onSlide={onSlide} up={false} />

                </div>
            </div>
        )
    }
}
