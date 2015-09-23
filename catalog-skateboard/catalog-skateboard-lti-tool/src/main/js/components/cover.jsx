import styles from './cover.scss'
import React from 'react'
import classNames from 'classnames'

const classes = classNames(styles.cover, 'cover')

const noCover = (
    <div className={classes}>
        No
        Cover
        Available
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
