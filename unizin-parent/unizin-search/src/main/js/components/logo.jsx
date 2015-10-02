import React from 'react'

export default class Logo extends React.Component {

    static propTypes = {
        className: React.PropTypes.string
    }

    render() {
        const logoURL = require('file!../../resources/skin/resources/logo.svg')
        return (
            <img className={this.props.className} src={logoURL} alt="Unizin Logo" title="Logo" />
        )
    }
}
