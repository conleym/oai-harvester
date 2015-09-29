
import React from 'react'

export default class Loader extends React.Component {
    static displayName = 'Loader'

    static defaultProps = {
        message: 'Loading...'
    }

    render() {
        return (
            <div>{this.props.message}</div>
        )
    }
}
