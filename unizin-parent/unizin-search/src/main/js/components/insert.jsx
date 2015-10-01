import React from 'react'

export default class Insert extends React.Component {
    static displayName = 'Insert'

    renderError(document) {

        if (document.loadError) {
            return (
                <div>
                    {document.loadError}
                </div>
            )
        }

        return null
    }

    render() {
        const { document } = this.props
        return (
            <div>
                Preparing '{document.title}'

                {this.renderError(document)}
            </div>
        )
    }
}
