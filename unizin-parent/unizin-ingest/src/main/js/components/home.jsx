import React from 'react'
import styles from './home.scss'
import FontAwesome from 'react-fontawesome'
import SlideUp from './slide_up.jsx'

const { func } = React.PropTypes

export default class Home extends React.Component {
    static displayName = 'Home'

    static propTypes = {
        onSelectFile: func.isRequired,
        files: React.PropTypes.object.isRequired,
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
                  </div>

                  <SlideUp buttonText="What is the Early Adopter Program?">
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
                      If you are not part of this Early Adopter Program, please contact <a href="mailto:eap@unzin.org">
                        eap@unzin.org</a> and report that you are seeing this
                      tool in error.
                    </p>
                  </SlideUp>

                </div>
            </div>
        )
    }
}
