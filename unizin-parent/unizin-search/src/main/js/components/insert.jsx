import React from 'react'

const { shape, string } = React.PropTypes

export default class Insert extends React.Component {
    static displayName = 'Insert'

    static propTypes = {
        document: shape({
            title: string,
            loadError: string,
        })
    }

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
