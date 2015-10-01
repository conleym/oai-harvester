import styles from './cover.scss'
import React from 'react'
import classNames from 'classnames'

const { string, shape, object } = React.PropTypes

export default class Cover extends React.Component {
    static displayName = 'Cover'

    static propTypes = {
        className: string,
        document: shape({
            properties: object
        })
    }

    render() {
        const { properties } = this.props.document
        const classes = classNames(this.props.className, styles.cover)

        if (properties && properties['thumb:thumbnail']) {
            return (
                <img
                    className={classes}
                    src={properties['thumb:thumbnail'].data} />
            )
        }

        return (
            <div className={classes}>
                No
                Cover
                Available
            </div>
        )
    }
}
