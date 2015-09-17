import React from 'react'

const noCover = (
    <div style={{
        border: '1px black solid',
        width: '100px',
    }}>
        No
        <br/>
        Cover
        <br/>
        Available
        <br/>
    </div>
)


export default class Cover extends React.Component {
    static displayName = 'Cover'

    render() {
        const { properties } = this.props.document

        if (properties && properties['thumb:thumbnail']) {
            return (
                <img src={properties['thumb:thumbnail'].data} />
            )
        }

        return noCover
    }
}
