import styles from './footer.scss'
import Logo from './logo.jsx'
import React from 'react'
import classNames from 'classnames'


export default class Footer extends React.Component {

    render() {
        const footerClasses = classNames(styles.footer, {
            [this.props.className]: (this.props.className != null)
        })
        return (
          <div className={footerClasses}>
            powered by <Logo className={styles.logo} />
          </div>
        )
    }
}
