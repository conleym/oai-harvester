import React from 'react'
import styles from './slide_up.scss'
import FontAwesome from 'react-fontawesome'
import classNames from 'classnames'

export default class SlideUp extends React.Component {
    static displayName = 'SlideUp'

    static propTypes = {
        onSlide: React.PropTypes.func.isRequired,
        slideClasses: React.PropTypes.any,
        up: React.PropTypes.boolean
    }

    constructor(props, context) {
        super(props, context)

        this.setUpClass(this.props.up)
    }

    setUpClass(up) {
        const sliderClasses = classNames(styles.slideUp, {
            [styles.up]: (this.props.up)
        })
        this.sliderClasses = sliderClasses
        console.log(sliderClasses);
        return this.sliderClasses
    }

    onSlide(e) {
        e.preventDefault()
        this.props.up = (this.props.up) ? false : true
        this.setUpClass(this.props.up)
    }

    render() {
        return (
          <div className={this.sliderClasses}>
            <div className={styles.content}>
              <button className="primary" onClick={::this.onSlide}>
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
                  If you are not part of this Early Adopter Program, please contact <a href="mailto:eap@unzin.org">
                    eap@unzin.org</a> and report that you are seeing this
                  tool in error.
                </p>
              </div>
            </div>
          </div>
        )
    }
}
