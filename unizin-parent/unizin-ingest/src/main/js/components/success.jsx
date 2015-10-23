import React from 'react'

export default class Success extends React.Component {
    static displayName = 'Success'

    static propTypes = {
        document: React.PropTypes.object.isRequired,
    }

    render() {
        const { document } = this.props
        return (
            <div>
                File Successfully Uploaded

                <pre>
                    {JSON.stringify(document, null, 2)}
                </pre>
            </div>
        )
    }
}
