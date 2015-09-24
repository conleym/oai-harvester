import styles from './cover.scss'
import React from 'react'
import classNames from 'classnames'

export default class Cover extends React.Component {
    static displayName = 'Cover'

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
