import styles from './footer.scss'
import Logo from './logo.jsx'
import React, { PropTypes } from 'react'


export default class Footer extends React.Component {

    constructor(props, context) {
        super(props, context)
    }

    render() {
        return (
          <footer>
            powered by <Logo className="logo" />
          </footer>
        )
    }
}
