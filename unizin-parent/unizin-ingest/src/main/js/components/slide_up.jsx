import React from 'react'
import styles from './slide_up.scss'
import FontAwesome from 'react-fontawesome'
import classNames from 'classnames'

export default class SlideUp extends React.Component {
    static displayName = 'SlideUp'

    static propTypes = {
        buttonText: React.PropTypes.string.isRequired,
        children: React.PropTypes.any
    }

    constructor(props, context) {
        super(props, context)

        this.state = {
            up: false
        }
    }

    onSlide(e) {
        e.preventDefault()
        this.setState({
            up: !this.state.up
        })
    }

    render() {
        const sliderClasses = classNames(styles.slideUp, {
            [styles.up]: (this.state.up)
        })

        return (
          <div className={sliderClasses}>
            <div className={styles.content}>
              <button className="primary" onClick={::this.onSlide}>
                <FontAwesome name="angle-up" className={styles.fa} aria-hidden /> {this.props.buttonText}
              </button>
              {this.props.children}
            </div>
          </div>
        )
    }
}
