import styles from './footer.scss'
import Logo from './logo.jsx'
import React, { PropTypes } from 'react'


export default class Footer extends React.Component {

    render() {
        const footerClasses = (this.props.className != null) ? this.props.className : styles.footer
        return (
          <div className={footerClasses}>
            powered by <Logo className={styles.logo} />
          </div>
        )
    }
}
