import React from 'react'
import styles from './logo.scss'

const SVG = require('babel!svg-react!../../resources/skin/resources/logo.svg?name=Icon')

export default class Logo extends React.Component {

    constructor(props, context) {
        super(props, context)
    }

    render() {
        return (
            <SVG className={this.props.className} />
        )
    }
}
