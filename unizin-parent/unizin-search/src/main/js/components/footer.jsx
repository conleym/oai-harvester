import styles from './footer.scss'
import Logo from './logo.jsx'
import React, { PropTypes } from 'react'


export default class Footer extends React.Component {

    render() {
        return (
          <div className={styles.footer}>
            powered by <Logo className={styles.logo} />
          </div>
        )
    }
}
