import styles from './footer.scss'
import Logo from './logo.jsx'
import React, { PropTypes } from 'react'


export default class Footer extends React.Component {

    constructor(props, context) {
        super(props, context)
    }

    render() {
        return (
          <div className={styles.footer}>
            powered by <Logo className={styles.logo} />
        </div>
        )
    }
}
