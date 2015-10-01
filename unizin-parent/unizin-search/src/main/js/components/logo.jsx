import React from 'react'

export default class Logo extends React.Component {

    render() {
        const logoURL = require('file!../../resources/skin/resources/logo.svg')
        return (
            <img src={logoURL} />
        )
    }
}
